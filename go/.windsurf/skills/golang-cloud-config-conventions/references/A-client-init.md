# A. Client Initialisation & Authentication

Expands rules A1–A5.

## A1 — Workload identity, not static keys [MUST]

**AWS — IAM role via instance metadata / IRSA (EKS):**
```go
import (
    "github.com/aws/aws-sdk-go-v2/config"
    "github.com/aws/aws-sdk-go-v2/service/s3"
)

// Uses the default credential chain: IRSA → instance metadata → env vars
// In EKS with IRSA, the pod's service account is mapped to an IAM role — no key needed
cfg, err := config.LoadDefaultConfig(ctx, config.WithRegion("ap-south-1"))
if err != nil {
    return fmt.Errorf("load AWS config: %w", err)
}
client := s3.NewFromConfig(cfg)
```

**GCP — Application Default Credentials (Workload Identity in GKE):**
```go
import "cloud.google.com/go/storage"

// Uses ADC chain: GKE Workload Identity → metadata server → GOOGLE_APPLICATION_CREDENTIALS
client, err := storage.NewClient(ctx)
if err != nil {
    return fmt.Errorf("storage client: %w", err)
}
defer client.Close()
```

**Prohibited in production:**
```go
// WRONG — static key in environment variable
cfg, _ := config.LoadDefaultConfig(ctx,
    config.WithCredentialsProvider(
        credentials.NewStaticCredentialsProvider(
            os.Getenv("AWS_ACCESS_KEY_ID"),     // static key
            os.Getenv("AWS_SECRET_ACCESS_KEY"),
            "",
        ),
    ),
)
```

Static keys:
- Never rotate automatically.
- Are at risk if the environment variable is logged or inspected.
- Require manual rotation on personnel departure.

## A2 — One client, created at startup [MUST]

```go
type StorageService struct {
    s3     *s3.Client       // created once
    bucket string
}

func NewStorageService(ctx context.Context, bucket string) (*StorageService, error) {
    cfg, err := config.LoadDefaultConfig(ctx, config.WithRegion("ap-south-1"))
    if err != nil {
        return nil, err
    }
    return &StorageService{
        s3:     s3.NewFromConfig(cfg),
        bucket: bucket,
    }, nil
}
```

The AWS S3 client and GCS client are safe for concurrent use. Create them once and inject wherever needed. Creating a new client per operation:
- Makes a new HTTP connection pool per operation.
- Does not benefit from connection reuse.
- May trigger credential-refresh on every call.

## A5 — Local development endpoint [SHOULD]

```go
// AWS-compatible: point at MinIO for local dev
s3Client := s3.NewFromConfig(cfg, func(o *s3.Options) {
    if endpoint := os.Getenv("S3_ENDPOINT"); endpoint != "" {
        o.BaseEndpoint = aws.String(endpoint)
        o.UsePathStyle = true // MinIO requires path-style
    }
})

// GCS: set STORAGE_EMULATOR_HOST=localhost:9023 before creating the client
// The GCS client picks it up automatically
```

This pattern lets developers run `docker compose up minio` and point the app at it without touching production credentials.

## Common findings

1. Static AWS access key stored in an environment variable in the deployment manifest.
2. `s3.NewFromConfig(cfg)` called inside a request handler — new client per request.
3. Hardcoded bucket name and region in source — breaks environment promotion.
4. No local endpoint override — tests require real cloud credentials.
