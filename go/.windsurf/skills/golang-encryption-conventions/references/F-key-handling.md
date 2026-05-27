# F. Key Handling in Go

Expands rules F1–F6.

## F1 — No hard-coded key material [MUST]

```go
// ALL WRONG
const encryptionKey = "this-is-a-32-byte-key-dont-use!!"
var aesKey = []byte{0x2b, 0x7e, 0x15, 0x16 /* ... */}
defaultKey := os.Getenv("ENC_KEY", "fallback-static-key") // committed default
```

Hard-coded keys are in the binary, in the container image, and visible in the repository history. Once committed they are effectively public. Keys come from a managed secret store at runtime (see `golang-credential-management` skill).

## F2 — Envelope encryption [MUST for production data]

Direct encryption with a long-lived master key means every ciphertext is vulnerable if the master key leaks, and re-keying all encrypted data requires re-encrypting every row.

Envelope encryption:

```go
// Pseudocode — actual KMS API varies by provider
func encryptField(kmsClient KMSClient, kekID string, plaintext []byte) (*EncryptedField, error) {
    // 1. Ask KMS to generate a fresh data key
    dk, err := kmsClient.GenerateDataKey(ctx, kekID, 32) // {plaintext, ciphertext}
    if err != nil {
        return nil, fmt.Errorf("generate DEK: %w", err)
    }
    defer func() {
        // zeroise the plaintext DEK immediately after use (F3)
        for i := range dk.Plaintext { dk.Plaintext[i] = 0 }
    }()

    // 2. Encrypt the data with the plaintext DEK
    nonce, ct, err := aesGCMEncrypt(dk.Plaintext, plaintext, []byte(kekID))
    if err != nil {
        return nil, err
    }

    // 3. Store the KMS-encrypted DEK alongside the ciphertext
    return &EncryptedField{
        KEKRef:         kekID,
        EncryptedDEK:   dk.Ciphertext, // safe to store; only KMS can decrypt it
        Nonce:          nonce,
        Ciphertext:     ct,
    }, nil
}
```

The master (Key Encryption Key) never leaves the KMS/HSM. Rotating the KEK means re-encrypting only the DEKs, not all the data. A compromised DEK affects only the data encrypted with it.

## F3 — Zeroise after use [SHOULD]

```go
key := fetchDEK() // []byte
defer func() {
    for i := range key { key[i] = 0 }
}()
// use key...
```

Go's garbage collector does not guarantee when memory is freed or zeroed. Overwriting the key bytes reduces the window in which a heap dump or core file would expose them. Limitations:
- Go may copy slice contents on GC compaction (not the current GC, but worth knowing).
- Passing a slice by value copies the header but shares the backing array — zero the backing array.
- The compiler may optimise away a loop that "does nothing useful" — use `subtle.ConstantTimeCopy` or an assembly stub if a hard guarantee is needed.

For most applications, best-effort zeroisation is still worth doing.

## F4 — Avoid spreading keys [SHOULD]

```go
// WRONG — now there are two copies of the key in memory
keyCopy := key // separate backing array if this is a string, same if slice

// BETTER — pass the slice; both refer to the same backing array
func encrypt(key []byte, ...) { ... }
```

Each copy is a separate byte array that may persist independently. Prefer passing `[]byte` through function parameters over copying into struct fields, strings, or new slices.

## F5 — Redacting key-bearing types [MUST]

```go
type SecretKey []byte

func (SecretKey) String() string               { return "[REDACTED]" }
func (SecretKey) LogValue() slog.Value         { return slog.StringValue("[REDACTED]") }
func (SecretKey) MarshalJSON() ([]byte, error) { return []byte(`"[REDACTED]"`), nil }
```

A plain `[]byte` or `string` key will appear in logs or JSON if someone passes it to `slog`, `fmt.Printf`, `json.Marshal`, or an error message. A typed wrapper whose `String()` redacts prevents this. The underlying bytes are accessed via a method that is grepped/reviewed:

```go
func (s SecretKey) Bytes() []byte { return []byte(s) }
```

## F6 — Key versioning for rotation [SHOULD]

Every stored ciphertext carries the key version that produced it:

```go
type EncryptedField struct {
    KeyVersion int    // 1, 2, 3...
    Nonce      []byte
    Ciphertext []byte
}
```

When a new key is introduced, new records use the new key; old records are still decryptable via their version tag. A background job can re-encrypt old records at leisure. Without versioning, rotating a key requires an immediate, synchronous re-encryption of all data.

## Common findings

1. Key derived from a static string with a SHA-256 hash — not envelope encryption.
2. Plaintext DEK persisted alongside the ciphertext without KMS encryption.
3. Key material in a plain `[]byte` with no redacting type — lands in logs.
4. No key version tag on stored ciphertext — rotation requires full table re-encryption.
5. DEK not zeroised after use — plaintext key lives in heap indefinitely.
