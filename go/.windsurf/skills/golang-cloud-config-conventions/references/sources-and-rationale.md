# Sources & Rationale

This skill is **engineering conventions, not a standard.**

## What this skill draws on

- **AWS SDK for Go v2** documentation and retry/credential best practices.
- **GCS Go client** documentation and retry-strategy guidance.
- **AWS Well-Architected Framework** — security and reliability pillars: least-privilege IAM, encryption at rest, versioning for durability.
- **GCP Architecture Framework** — identity-based authentication (Workload Identity), encryption with CMEK, regional data storage.
- **OWASP Secure File Upload** cheat sheet — content-type verification, quarantine, storage outside web root.
- **BFSI data-residency requirement** (RBI circular DPSS.CO.OD.No.2785/06.08.005/2017-2018) — restated in `bfsi-india-core` rule M1.

## What is NOT a standard

Multi-cloud Go client configuration has no single authoritative standard. AWS SDK v2 and GCS client APIs differ; the skill provides patterns for each and derives principles that apply to both.

## Provider-specific notes

- **S3 vs GCS checksum approach** differs — S3 SDK v2 uses `ChecksumAlgorithm` on `PutObject`; GCS uses `Writer.CRC32C`. Both achieve the same integrity goal.
- **Retry configuration** — AWS SDK v2 has a built-in adaptive retry mode; GCS uses `gax.Backoff`. Both are configurable; the principle (use the SDK's retry, not a manual loop) is the same.
- **Azure Blob** follows the same patterns with `github.com/Azure/azure-sdk-for-go/sdk/storage/azblob`; workload identity uses Managed Identity. The skill does not cover Azure specifics but the principles transfer directly.

## MinIO note

MinIO is S3-compatible and is the recommended local development substitute. The `S3_ENDPOINT` environment variable override pattern (rule A5) is the standard way to point the AWS SDK at MinIO without code changes.
