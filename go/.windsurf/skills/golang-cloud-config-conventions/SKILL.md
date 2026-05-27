---
name: golang-cloud-config-conventions
description: Go cloud resource configuration conventions — object storage (S3, GCS, Azure Blob), client initialisation, context and timeout patterns, retry configuration, workload identity authentication, bucket/object naming, presigned URL hygiene, and cross-cloud abstraction. Use whenever Go code interacts with cloud object storage or when cloud client configuration is being written or reviewed. Activate on mentions of S3, GCS, Google Cloud Storage, Azure Blob, object storage, bucket, presigned URL, aws-sdk-go, cloud.google.com/go/storage, MinIO, or "how do I upload/download files in Go". These are engineering conventions built on AWS SDK v2, GCS Go client, and cloud provider well-architected guidance. No single official standard governs multi-cloud Go client configuration.
---

# Go Cloud Resource Configuration Conventions

Conventions for configuring and using cloud object storage in Go. **These are conventions, not a standard.**

This skill covers S3-compatible (AWS S3, MinIO) and GCS object storage patterns. Azure Blob follows the same patterns with `azblob`; the principles transfer.

## How to use this skill

1. Walk the rule categories when writing or reviewing cloud-storage client code.
2. **MUST** violations are blockers. **SHOULD** violations need a documented reason.
3. For BFSI services, data residency (data stored only in India for payment-system data) is a regulatory requirement — see `bfsi-india-core` rule M1.

## Sources

- **AWS SDK for Go v2** — `github.com/aws/aws-sdk-go-v2`.
- **GCS Go client** — `cloud.google.com/go/storage`.
- **AWS Well-Architected Framework** — security, reliability, cost pillars.
- **GCP Cloud Storage best practices** — retry strategy, authentication.
- **OWASP** — secure file upload / presigned URL guidance.

Full notes: `references/sources-and-rationale.md`.

## Rule categories

| # | Category | Reference file |
|---|---|---|
| A | Client initialisation & authentication | `references/A-client-init.md` |
| B | Context, timeouts & retries | `references/B-context-timeouts.md` |
| C | Object operations | `references/C-object-operations.md` |
| D | Presigned URLs | `references/D-presigned-urls.md` |
| E | Naming & organisation | `references/E-naming.md` |
| F | BFSI & compliance rules | `references/F-bfsi-compliance.md` |

---

## Rule index

### A. Client initialisation & authentication

- **A1 [MUST]** Authenticate using workload identity — IAM roles (AWS), Workload Identity (GCP), Managed Identity (Azure) — not static access keys. Static keys in code or environment variables are prohibited in production.
- **A2 [MUST]** Create the cloud client once at startup and share it. Cloud SDK clients are designed to be shared and reused; creating one per request is expensive and exhausts connection limits.
- **A3 [MUST]** Client configuration (bucket names, region, endpoint, storage class) comes from environment config, not hardcoded in source.
- **A4 [SHOULD]** Use the SDK's built-in retry configuration rather than wrapping calls in a manual retry loop. The SDK knows which errors are retryable; a naive retry loop may retry non-idempotent operations.
- **A5 [SHOULD]** For local development and tests, use a local-compatible endpoint (MinIO for S3, the GCS emulator) so no real cloud credentials are required. Configure via environment variable.

### B. Context, timeouts & retries

- **B1 [MUST]** Every cloud storage call takes a `context.Context` with an explicit deadline or timeout. No unbounded cloud calls — a hung upload or download must not block a goroutine indefinitely.
- **B2 [MUST]** Use `context.WithTimeout` for individual operations; derive it from the request or job context so cancellation propagates.
- **B3 [SHOULD]** Set separate, appropriate timeouts for upload vs download vs metadata operations — uploads of large files need longer timeouts than a `HeadObject` call.
- **B4 [SHOULD]** Configure exponential backoff with jitter on the SDK client's retry policy. Do not retry forever — cap total retry duration.

### C. Object operations

- **C1 [MUST]** Stream large files — never read an entire object into memory with `io.ReadAll` when it could be large. Use `io.Copy` to stream to a destination.
- **C2 [MUST]** Always close the response body from `GetObject` or equivalent. An unclosed body leaks the underlying HTTP connection back to the pool.
- **C3 [MUST]** Verify uploaded content integrity. For S3: use `ChecksumAlgorithm` (SHA-256 or CRC32C) on `PutObject`. For GCS: `Writer.SendCRC32C`. This catches silent bit-rot or partial uploads.
- **C4 [MUST]** For BFSI document uploads (KYC, statements, contracts): verify the file content type by inspecting bytes, not the declared MIME type, before storing. Apply the same controls as `golang-data-storage-conventions` rule C4.
- **C5 [SHOULD]** Use multipart upload for files > 5 MB (S3) or resumable uploads (GCS) for files > 5 MB. Direct single-part upload of large files is not resumable and may fail silently on timeout.
- **C6 [SHOULD]** Tag objects with metadata useful for lifecycle, compliance, and audit: `tenantID`, `dataClass`, `uploadedBy`, `purposeCode`.

### D. Presigned URLs

- **D1 [MUST]** Presigned URLs have a short expiry appropriate to their purpose. Upload presigned URLs: 5–15 minutes. Download presigned URLs: as short as the consumer can tolerate, max 24 hours.
- **D2 [MUST]** Presigned URLs are single-use or scoped to a specific object and operation. A URL that allows any key under a prefix is a data-exposure risk.
- **D3 [MUST]** Never log presigned URLs. A presigned URL grants access to the object for its full lifetime — logging it puts object access in log stores potentially accessible to more people than the object itself.
- **D4 [SHOULD]** Validate that the object key in an inbound presigned URL or upload confirmation refers to an object the caller is entitled to access. Do not trust the key from the client without verification.
- **D5 [MUST]** For BFSI: presigned download URLs for sensitive documents (account statements, KYC) must not be cached by CDNs or proxies. Set `Cache-Control: no-store` and use `Content-Disposition: attachment` on the served object.

### E. Naming & organisation

- **E1 [SHOULD]** Object keys are namespaced by tenant and entity type: `<tenantID>/<entityType>/<year>/<month>/<objectID>.<ext>`. Flat namespaces are unmanageable at scale.
- **E2 [MUST]** Never use user-supplied input directly as an object key. Sanitise, validate against an allow-list of characters, and prefix with the server-generated path. Path-traversal in object keys can escape the intended bucket prefix.
- **E3 [SHOULD]** Add a checksum or hash of the content to the object key or metadata for deduplication and integrity tracking.
- **E4 [SHOULD]** Use consistent object-key design for lifecycle policies: `<tenantID>/kyc/...` can have a different lifecycle (7-year retention) than `<tenantID>/temp/...` (7-day retention).

### F. BFSI & compliance rules

- **F1 [MUST]** Payment-system data and KYC documents must be stored in Indian regions. AWS: `ap-south-1` (Mumbai). GCP: `asia-south1`. Verify the region in the client configuration at startup. *(Binds `bfsi-india-core` rule M1.)*
- **F2 [MUST]** Bucket access is private by default. Block all public access. There are no publicly readable buckets for regulated data.
- **F3 [MUST]** Enable server-side encryption with a customer-managed key (CMK) on all buckets holding regulated data. KMS key ARN / Cloud KMS key is part of the bucket configuration, not an afterthought.
- **F4 [MUST]** Enable bucket versioning and object deletion protection for buckets holding documents subject to retention requirements (KYC: 5 years post-relationship end; transaction records: ~10 years — verify per applicable direction).
- **F5 [SHOULD]** Enable access logging on all production buckets and ship logs to the SIEM.

## Out of scope

- Bucket creation and IAM policy management — infrastructure-as-code concern (Terraform, Pulumi).
- CDN/CloudFront configuration.
- Data transfer and egress cost optimisation.
