---
name: Image Upload Feature Review (2026-04-13)
description: Production readiness review of Phase 3 image attachment — upload, attach/detach, static serving
type: project
---

Key findings from the April 13 2026 review of the image upload / Phase 3 board feature.

**Why:** Recorded so future reviews can track which issues are open and avoid re-reporting them.

**How to apply:** When reviewing future changes touching PostImage, ImageUploadService, LocalFileStorage, PostService.attachImages/detachImages, or WebMvcConfig, consult these first.

## Critical issues

1. **Image ownership not enforced — IDOR on attach** — `PostService.attachImages` calls `findAllByIdInAndPostIsNull(imageIds)`. Any authenticated user can supply imageIds they did not upload, because there is no `uploaderId` column on `post_images` and no check that the image belongs to the requesting user. User A can silently steal/attach images uploaded by User B.

2. **Path traversal in LocalFileStorage.store** — `file.getOriginalFilename()` is used only to extract the extension via `StringUtils.getFilenameExtension`. The extension itself is then appended to a UUID filename, so the constructed path is safe. However, `dir` is built from `userId` + date — both trusted — so no traversal is possible in the current implementation. This is NOT an actual traversal risk. (Downgraded to note.)

3. **Content-type spoofing: extension+MIME checked but magic bytes are not** — An attacker can rename a PHP/HTML/SVG file to `evil.jpg`, set Content-Type: image/jpeg, and the service will accept it. The stored file will be served by Spring's static resource handler. If this is ever deployed behind a CDN or Nginx that re-detects MIME type by content (rare), this is exploitable. Minimum fix: read first 4 bytes of the file's InputStream and validate the magic number.

4. **Detach on update leaks orphan images and files** — `PostService.update` calls `detachImages(postId)`, which sets `post_id = NULL` on all current images. It does not delete the `PostImage` rows or the physical files. These orphan records/files accumulate on disk forever. No cleanup job exists.

5. **Silent image count mismatch on attach — no error thrown** — `attachImages` calls `findAllByIdInAndPostIsNull(imageIds)` which silently drops IDs that are either non-existent or already attached. If a client sends 5 IDs and 2 are already attached to another post, only 3 are attached and no error is returned. The post is created successfully but the client doesn't know images were dropped.

## Warning-level issues

6. **`ext` can be null from getOriginalFilename** — If `getOriginalFilename()` returns null (e.g., programmatic client omits filename header), `StringUtils.getFilenameExtension(null)` returns null. The null-check in `validate` catches this correctly. However, in `LocalFileStorage.store`, the same `getFilenameExtension` call happens again without the null guard — `UUID.randomUUID() + "." + null` produces a file named `uuid.null`. The validation in `ImageUploadService.validate` runs before `store`, so a null extension would already throw. But because `store` is called on the `FileStorage` interface, a future caller bypassing `ImageUploadService` would produce `.null` files.

7. **No `@Valid` on imageIds in request bodies** — `@Size(max=10)` on `List<Long> imageIds` in `CreatePostRequest` / `UpdatePostRequest` validates list length, but there is no check that individual `Long` values are positive (non-zero, non-negative). A client can send `imageIds: [-1, 0]`; the DB query will simply find nothing and silently attach no images.

8. **`application.yaml` has a hardcoded JWT secret in the committed file** — `jwt_secret_must_be_changed_in_production_in_case_of_security_breaches` is committed. This was noted in the 2026-04-03 review and remains unfixed.

9. **`WebMvcConfig.addResourceHandlers` uses string concatenation for the resource location** — `"file:" + uploadRoot + "/"`. If `uploadRoot` contains a trailing slash already (e.g., `./uploads/`) this produces `file:./uploads//`, which works on Linux but is fragile. Use `Path.of(uploadRoot).toUri().toString()` instead.

10. **`LocalFileStorage.delete` throws RuntimeException on IOException** — If a file is already gone (concurrent delete, manual cleanup), `Files.deleteIfExists` will not throw. But other IOExceptions (permissions) will bubble as an unlogged `RuntimeException`, masking the root cause. The global handler will catch it as a 500, but the IOException is swallowed without a log entry.

11. **No disk space guard** — There is no check of available disk space before writing. Under load, concurrent uploads can fill the volume, causing an IOException that surfaces as a generic 500 to the client.

12. **No `@Transactional` rollback on file write failure** — `ImageUploadService.upload` is `@Transactional`. Each iteration calls `fileStorage.store(...)` then `postImageRepository.save(...)`. If `store` succeeds for files 1-3 but fails on file 4, the transaction rolls back the DB rows for files 1-3, but the physical files on disk for files 1-3 are NOT deleted (no compensation). Orphan files are left on disk.

## Improvement-level issues

13. **`Post.@Builder` access is public** — `Post` declares `@Builder` without `(access = AccessLevel.PRIVATE)`. This violates CLAUDE.md which requires private builder + static factory `of()`. The `of()` method exists but `Post.builder()` is also callable from outside, breaking the convention.

14. **`PostImage.url` stores the full public URL path** — The column stores `/uploads/posts/1/2026/04/uuid.jpg`. If the app moves to S3 or changes its URL prefix, all existing rows are stale. Better to store the relative storage path and compute the URL at read time via `FileStorage.toUrl(path)`.

15. **Test coverage gaps** — `PostServiceTest` does not test `attachImages` or `detachImages` behavior (images with valid IDs, images already attached, empty list). `ImageUploadServiceTest` does not test the partial-failure scenario (store succeeds for file 1, fails for file 2). `LocalFileStorageTest` does not test `store` with a null original filename.

16. **`ImageController` uses `@ResponseStatus(HttpStatus.CREATED)` but `ApiResponse.ok()` implies 200** — The HTTP status is 201 (correct), but the `ApiResponse.ok()` factory method is semantically "200 OK". This is a naming inconsistency, not a bug.
