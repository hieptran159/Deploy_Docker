# CLAUDE.md

Guidance for working in this repository.

## What this repo is

A **deployment/orchestration repo** that bundles a full forum ("social") application so it
can be run with Docker Compose. It vendors point-in-time snapshots of two upstream
projects (each has its own GitHub repo per `README.md`):

| Path        | Role     | Stack                                               |
|-------------|----------|-----------------------------------------------------|
| `forum_fe/` | Frontend | Vue 3 + Vite 5, Pinia, vue-router, Tailwind, DevExtreme |
| `social/`   | Backend  | Spring Boot 3.1.7, Java 17, JPA/Hibernate, MySQL 8, netty-socketio, SendGrid |

`socialdata.sql` (root), `social/db.sql`, `social/db1.sql` are database dumps/seeds.

`forum_fe/` is the actively developed frontend and the one the root `docker-compose.yml`
builds. (A stale earlier copy, `HIP_Forum/`, was removed in Aug 2026 — ignore any
lingering references to it.)

## Running

**Full stack (from repo root)** — see `DEPLOY.md` for the full guide:
```
cp .env.example .env      # optional; all vars have defaults
docker compose up -d --build
```
- frontend → http://localhost  (`forum_fe/` built to nginx; `FRONTEND_PORT`)
- backend REST → http://localhost:8081  (Swagger UI: `/api-docs.html`, spec: `/api-docs`;
  health: `/actuator/health` — only `health` is exposed, `permitAll` in `CustomFilterSecurity`)
- backend Socket.IO server → port 8082
- MySQL → host port 3307 (container 3306), db `socialapp`, root pw from `.env`

The compose file: `db` has a TCP healthcheck and `backend` waits on
`condition: service_healthy`; `backend` itself has a `curl`-based healthcheck against
`/actuator/health` (curl is `apt-get`-installed in the runtime stage of `social/Dockerfile`)
— informational only, nothing gates on it. On a fresh (empty) `mysql-data` volume the backend's
**Flyway** run at startup builds the whole schema from
`social/src/main/resources/db/migration/V1__baseline.sql` (the old `social/db.sql`
init-mount was removed). Data persists in the `mysql-data` volume and uploaded images in
the `uploads` volume (`/app/uploads` in the backend container). Overridable env vars live in `.env`
(`*_PORT`, `MYSQL_*`, `JWT_SECRET`, `SENDGRID_*`, and `PUBLIC_API_URL`/`PUBLIC_SOCKET_URL`
which are **baked into the frontend build** as `VITE_API_URL`/`VITE_SOCKET_URL`).
DB export/import for infra migration: `scripts/db-export.sh` / `scripts/db-import.sh`.
`scripts/backup.sh` (+ `scripts/backup.logrotate`) is the cron-driven periodic backup:
gzipped `mysqldump` + `tar` of the `uploads` volume into `backup/` (gitignored), prunes to
`BACKUP_KEEP_DAYS` (14); self-contained (reads `MYSQL_*` inside the `db` container, no
`.env` sourcing); optional offsite via `BACKUP_RSYNC_DEST` (rsync/SSH),
`BACKUP_RCLONE_DEST` (`rclone sync` to e.g. Cloudflare R2 / Backblaze B2 free tier), or
`BACKUP_GIT_DIR` (mirror into a private repo clone, force-pushed as one parentless commit
so history never grows). Not wired to cron in the repo — install per `DEPLOY.md`.
`scripts/backfill-hashtags.sh` is a one-off to index `#tags` in pre-existing posts (see
the Hashtags note under backend architecture).

**Backend alone:**
```
cd social
./mvnw spring-boot:run        # needs a MySQL on localhost:3306 / db socialapp
./mvnw install -DskipTests    # build jar (target/*.jar); this is what the Dockerfile runs
```
`application.properties` reads env vars (`MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_USER`,
`MYSQL_PASSWORD`, `MYSQL_DATABASE`, `SERVER_PORT`, `JWT_SECRET`, `SENDGRID_*`,
`SOCKET_SERVER_PORT/HOST`) with localhost defaults; compose overrides them.

**Frontend alone:**
```
cd forum_fe
npm install
npm run dev       # Vite dev server
npm run build     # → dist/, served by nginx in the Docker image
```

## Tests

JUnit 5 unit-test suite (72 tests) under `social/src/test/java` — **no DB / Docker / Spring
context**, runs on plain `./mvnw test` (deps already in `spring-boot-starter-test` +
`spring-security-test`). Service tests use `@ExtendWith(MockitoExtension.class)` +
`@MockitoSettings(strictness = LENIENT)`, mock every constructor dep, and instantiate the
impl directly in `@BeforeEach`:

| Test | Covers |
|------|--------|
| `security/RateLimitFilterTest` | `RateLimitFilter` window counter (per-IP, per-bucket, GET/non-auth bypass, disabled flag); XFF **ignored** by default (spoof-proof) and honored only when `trustForwarded` — via `MockHttpServletRequest`/`MockFilterChain` |
| `service/impl/FileUploadsServiceImplTest` | upload validation + downscale/recompress + small-image passthrough + non-image reject, `MockEnvironment` + `@TempDir` |
| `service/impl/NormReactionTest` | `PostServiceImpl.normReaction` (static pkg-private) |
| `service/impl/FollowServiceImplTest` | block/unblock guards (self, already-blocked no-op, not-blocked reject), `friendStatus` block direction (`blocked_out`/`blocked_in`/`self`/`none`), `isBlockedEither`, `sendRequest` guards (blocked, deactivated target, self) — Mockito, no context |
| `service/impl/PostServiceImplTest` | `createPost` visibility (`friends`/`private`/default `public`) + draft rules (title-only ok, empty rejected, published missing body); `publishPost` guards (non-author, already published, missing content, happy path); `repost` guards (friends-only, own post, draft, idempotent, saves + notifies) — `ArgumentCaptor<Posts>` |
| `service/impl/ReportServiceImplTest` | `ReportServiceImpl.create` auto-hide threshold: invalid type, below threshold no-hide, at threshold sets `status=hidden` + saves, already-hidden untouched, duplicate open report no-op, threshold `0` disables — `ReflectionTestUtils` for `@Value autoHideThreshold` |
| `service/impl/NotificationServiceImplTest` | `listMinePaged` page/size clamping (negative page→0, size<1→20, size>50→50) via `ArgumentCaptor<Pageable>` |
| `utils/EmailTemplateTest` | `EmailTemplate.otp` HTML + HTML-escaping |
| `utils/HashtagUtilsTest` | `HashtagUtils.extract` (lowercase/dedup, Unicode + `_`, skip all-digit, 20-tag cap) + `normalize` (strip `#`, reject spaces/empty/all-digit/null) |
| `utils/JwtUtilsTest` | refresh-token round-trip; `validateRefreshToken` rejects an access token; `validateAccessToken` rejects a refresh token; blacklisted refresh token rejected — `ReflectionTestUtils` for `@Value` secret/expiry |
| `service/impl/AuthServiceImplTest` | `login` withholds tokens + saves a code + sets `twofaRequired` when 2FA on (issues tokens when off); `verifyTwoFactor` guards (wrong code, expired, 2FA disabled) + happy path (case-insensitive code, clears code, issues access+refresh) — 8 mocked ctor deps |

`SocialApplicationTests` (`@SpringBootTest` context-load) spins up a throwaway MySQL 8 via
**Testcontainers** (`@Container @ServiceConnection MySQLContainer`) — needs a Docker daemon
(CI `ubuntu-latest` has one; a dev box needs Docker running). It supplies a test
`jwt.secretkey` + `socket-server.port=0` via `@SpringBootTest(properties=…)`, and also
exercises the full Flyway V1→Vn chain on an empty DB + Hibernate `validate`. `./mvnw test`
runs the whole suite; the release build still uses `./mvnw install -DskipTests`. No
frontend tests.

## Backend architecture notes

- Package root: `com.didan.social`. Layers: `controller/` → `service/` (+ `service/impl/`)
  → `repository/` (Spring Data JPA) → `entity/` (composite keys in `entity/keys/`).
  DTOs in `dto/`, request bodies in `payload/request/`, responses wrapped in
  `payload/ResponseData`.
- Route prefixes: `/auth`, `/user`, `/post`, `/comment`, `/friend`, `/chat`, `/admin`,
  `/notification`.
- Friends: `Followers` (table `followers`) now carries a `status` column —
  `"pending"` (request sent, `users1`→`users2`) or `"accepted"` (friends); legacy NULL =
  accepted. `FollowService` / `FollowController` were reworked into a friend-request flow
  under `/friend` (`request`/`accept`/`decline`/`cancel`/`{id}` delete = unfriend,
  `list/{id}`, `requests/incoming|outgoing`, `status/{id}`). `ChatService.addMember` only
  admits users who are already friends of the caller.
- Blocking: `entity/Blocks` (table `blocks`, composite key `blocker_id`+`blocked_id`,
  `ddl-auto`). `/friend/block/{id}` (POST) also deletes any `followers` rows both ways;
  `/friend/block/{id}` (DELETE) unblock; `/friend/blocked` lists ids I blocked.
  `friendStatus` returns `blocked_out` / `blocked_in` before the friend states.
  `FollowService.isBlockedEither` guards `sendRequest` and `ChatServiceImpl.openDirectConversation`.
  Feed + search filter out authors in `blockRepository.blockedIdsOf(me) ∪ blockerIdsOf(me)`
  (`PostRepository.findFeedExcludingAuthors` / `countFeedExcludingAuthors` /
  `searchByKeywordExcludingAuthors`, used only when the exclude set is non-empty — `NOT IN`
  with an empty collection is avoided). Guests (no `me`) get no filtering.
- Admin audit log: `entity/AdminLog` (table `admin_logs`, `ddl-auto`). `service/AdminLogService.record(action,targetType,targetId,detail)` is a standalone bean, fire-and-forget (swallows its own errors), called from `AdminServiceImpl` (`GRANT_ADMIN`/`BAN_USER`/`UNBAN_USER`) and `ReportServiceImpl` (`HANDLE_REPORT`/`REMOVE_TARGET`/`RESTORE_TARGET`) after the action succeeds. `GET /admin/logs?page=&size=` (`AdminService.getLogs`, admin-gated, newest first). FE: "Nhật ký quản trị" card in `AdminPage.vue` with "Xem thêm".
- Auto-hide: `Posts.status = "hidden"` is set by `ReportServiceImpl.create` when a POST
  reaches `app.moderation.post-autohide-threshold` (`MOD_POST_AUTOHIDE`, default 3) OPEN
  reports — the `PUBLISHED` predicate then drops it from feed/search. `getPostById` shows
  a `hidden` post only to its author or an admin. `ReportDTO.targetStatus` carries the
  post status into the admin queue; `POST /report/admin/{id}/restore-target`
  (`restoreReportedTarget`) sets it back to `published` and resolves that post's OPEN
  reports (shared `resolveOpenFor` helper with `removeReportedTarget`).
- Deactivate: `Users.deactivated` (`null`/`0` = active, `1` = self-deactivated). `POST
  /user/deactivate?password=` (`UserServiceImpl.deactivateMyAccount`) sets the flag +
  blacklists the current token. **`AuthServiceImpl.login` auto-clears it** — logging back in
  reactivates. While deactivated: `blockRelatedIds` (feed/search exclude set) folds in
  `userRepository.findDeactivatedIds()`, `getPostById` / `getUserById` return null to
  non-owners, `getAllUser` / `searchUser` skip them, and `sendRequest` /
  `openDirectConversation` reject them. FE: EditProfile "Vô hiệu hoá tạm thời" in the
  danger zone (redirects to `/login`).
- Repost / share: `entity/Reposts` (table `reposts`, key `user_id`+`post_id`, `ddl-auto`).
  `POST /post/{id}/repost?note=` / `DELETE /post/{id}/repost` / `GET /post/reposts/{userId}`.
  The home feed is a **native UNION grouped by `post_id`** (`PostRepository.FEED_UNION` →
  `feedPage` = `SELECT pid, MAX(t) sort_t, MAX(is_repost) FROM (union) GROUP BY pid`; a
  repost's `created_at` is always > the post's `posted_at`, so `MAX(t)` is the repost time
  when any repost exists — bumping the post up **once**, never showing it twice).
  `feedCount` = `COUNT(DISTINCT pid)`. `getAllPostsByPage` hydrates `Posts` by id and, for
  `is_repost=1` pids, attaches the latest reposter's `repostedBy`/`repostedById`/
  `repostedAt`/`repostNote` (`RepostRepository.findByPostIdsOrderByCreatedAtDesc`, first
  row per pid). FE `Post.vue` shows "Bạn đã chia sẻ" when `repostedById` is me and has an
  inline 🔁 toggle button (optimistic). `:ex` (block-exclude set) must be non-empty for
  the native `NOT IN`, so the service passes `["-"]` when nobody is excluded. `PostDTO`
  gains `repostCount`/`reposted`/`repostedBy`/`repostedById`/`repostedAt`/`repostNote`,
  filled by `applyRepostInfo` (2 batch queries, no N+1) on feed/search/detail/reposts-list.
  Notification type `REPOST` (added to FE `notifTarget.POST_TYPES`). The old
  `findAllPostByCommentAtOrPostAt` / `findFeedExcludingAuthors` / `countFeedExcludingAuthors`
  / `countPublished` JPQL methods are now unused by the feed but kept.
- Friends feed: `GET /post/feed/friends?page=` + `/post/feed/friends/pages`
  (`PostService.getFriendsFeed` / `friendsFeedPageInfo`). Same grouped-UNION as the main
  feed but `PostRepository.FRIEND_FEED_UNION` filters `IN (:ids)` where ids =
  `followService.friendIdsOf(me) ∪ {me}` minus deactivated (always non-empty). Original
  rows require the author be a friend; repost rows require the **reposter** be a friend.
  `PostServiceImpl` now injects `FollowService`; row→DTO building is shared via
  `buildFeedFromRows`. FE `Home.vue` has an "Tất cả" / "Bạn bè" tab (login only).
- `EditProfile.vue` opens with a public-profile **preview card** (cover/avatar/name/public
  fields + link to `/user/{me}`) above a "Cài đặt tài khoản" heading; `UserProfile.vue`
  shows a "Chỉnh sửa hồ sơ" button on your own profile.
- `GET /post/by-user/{userId}?page=&size=` (`PostService.getPostsByUser`) — a user's
  **published** posts, newest first, `{items,total,page,totalPages}`. `UserProfile.vue` now
  renders the "Bài viết" and "Đã chia sẻ" sections with the shared `<Post>` card (was N
  individual `getPostById` calls) + "Xem thêm". The repost glyph is an inline Feather
  "repeat" SVG (not the 🔁 emoji) in `Post.vue` / `PostDetail.vue`.
- `Post.vue` shows a ✏️ button for the author that opens an inline `EditPost` popup
  (`<DxPopup v-if="editing">`) and emits `@refresh`; every parent that lists `<Post>` wires
  `@refresh` to reload. `CreatePost.vue` / `EditPost.vue` show live hashtag chips parsed by
  `helper.js#extractHashtags` (mirrors backend `HashtagUtils`).
- `PostDetail.vue`'s comment `@mention` autocomplete filters the full user list client-side.
  That list comes from `GET /user/getAllUser` (heavy: N+1 over followers/posts/participants
  per user + full DTOs) — fetched **lazily on the first `@` keystroke**, not on mount, and
  memoised for the SPA session via `apis/user.js#getAllUsersCached`. A server-side
  `?q=` mention-search endpoint would be the real fix if the user base grows.
- Reporting: `components/ReportDialog.vue` (singleton in `App.vue`, `provide('openReport')`
  `(targetType, targetId, label)`), a reason radio list (`spam`/`harassment`/`hate`/`nsfw`/
  `misinfo`/`other`) + optional detail → `sendReport(type, id, "<label>: <detail>")`.
  `PostDetail.vue` / `Comment.vue` / `UserProfile.vue` call `openReport` instead of the old
  plain confirm.
- Post visibility: `Posts.visibility` (`null`/`"public"` = everyone, `"friends"` = author +
  author's friends, `"private"` = author only). `PostServiceImpl.normVisibility` maps the
  request value to one of those three. Every feed/search/profile/by-tag query carries
  `PostRepository.VISIBLE` = `(p.visibility IS NULL OR = 'public' OR p.userPost.users.userId
  = :me OR (p.visibility = 'friends' AND p.userPost.users.userId IN :vids))` where `:vids` =
  `followService.friendIdsOf(viewer) ∪ {viewer}` and `:me` = the viewer id (both `"-"` for a
  guest — non-empty for the native `IN` / `=`). The native `FEED_UNION` carries the same
  clause with `:me`; `FRIEND_FEED_UNION` just adds `visibility <> 'private'` (private posts
  never show in the friends feed). `getPostById` gates a `private` post to the author and a
  `friends` post to author/friend-of-author; neither `friends` nor `private` can be reposted.
  `CreatePostRequest`/`EditPostRequest` carry `visibility`; FE `CreatePost`/`EditPost` have a
  "Ai xem được" select (🌐/👥/🔒); `Post.vue`/`PostDetail.vue` show a "👥 Bạn bè" / "🔒 Chỉ
  mình tôi" badge.
- Draft posts: `Posts.status` (`null`/`"published"` = live, `"draft"` = draft). Every feed
  and search query carries `PostRepository.PUBLISHED` (`p.status IS NULL OR p.status =
  'published'`) so drafts never leak; `countPublished()` backs `feedPageInfo`. `createPost`
  reads `CreatePostRequest.draft` (`"true"`/`"1"`) — a draft only needs title **or** body,
  saved as `"draft"`. `getPostById` returns null for a `draft` unless the caller is its
  author. `GET /post/drafts` (mine), `PATCH /post/publish/{id}` (author-only; requires
  title+body, bumps `postedAt` to now). FE: `CreatePost.vue` "Lưu nháp" button, `/drafts`
  page (edit via `EditPost` popup / publish / delete), header tab id 6.
- Hashtags: `entity/PostHashtags` (table `post_hashtags`, composite key `post_id`+`tag`,
  `ddl-auto`, indexed on `tag` and `post_id`). `utils/HashtagUtils.extract(title, body)`
  parses `#tag` (`[\p{L}\p{N}_]{1,50}`, Unicode + `_`, lowercased, all-digit tags dropped,
  ≤20/post); `normalize` cleans a client-supplied tag. `PostServiceImpl.syncHashtags` runs
  after `createPost` / `updatePost` (delete-all-then-insert) and rows are cleared on delete.
  `applyHashtags(dtos)` fills `PostDTO.hashtags` with **one** batched `IN` query on
  feed/search/detail/by-user/drafts (no N+1). `GET /post/by-tag/{tag}?page=&size=`
  (`getPostsByTag` → `{items,total,page,totalPages,tag}`, carries `PostRepository.VISIBLE`
  via `:vids`) and `GET /post/hashtags/trending?limit=` (`getTrendingHashtags` →
  `[{tag,count}]`, counts **public+published** only) are both in the guest permit list. FE:
  route `/tag/:tag` (no guard) → `pages/tag/TagPage.vue`; tag chips in `Post.vue` /
  `PostDetail.vue` link to it; "Hashtag nổi bật" card on `Home.vue`. Posts that predate
  the feature have no index rows (`#text` sits in the body as plain text) — run
  `scripts/backfill-hashtags.sh` **once** to populate `post_hashtags` for existing posts
  (idempotent `INSERT IGNORE`, regex mirrors `HashtagUtils`; needs a `mysql` client, reads
  `.env` / `MYSQL_*` like the other scripts).
- Notifications: `entity/Notifications` (plain columns, no JPA relations; table
  auto-created by `ddl-auto=update`). `NotificationService.push(...)` is fire-and-forget
  and swallows its own errors so it never breaks the caller. `pushUnique` dedups on
  `(recipient,type,targetId)`, `pushUniquePerActor` on `(recipient,actor,type,targetId)`.
  Types: `FRIEND_REQUEST`, `FRIEND_ACCEPT`, `COMMENT` (targetId=postId), `COMMENT_LIKE`,
  `POST_LIKE`, `MESSAGE` (targetId=conversationId). `/auth/signin` returns `isAdmin`
  ("0"/"1") so the client no longer probes `/admin/blacklist` at login.
- Chat message reactions: `entity/MessageReactions` (table `message_reactions`, key
  `message_id`+`user_id`, `emoji` col, `ddl-auto`). `POST /chat/message/{id}/react?emoji=`
  (upsert, participant-only) / `DELETE /chat/message/{id}/react` — both return the new
  `{emoji: count}` map and broadcast `message_reaction` to the room.
  `getAllMessagesInConversation` batch-loads reactions into `MessageDTO.reactions` +
  `myReaction`. FE `Chat.vue`: 🙂 hover button → 6-emoji picker, chips under the bubble.
- Group chat avatar: `Conversations.avatarUrl` (`ddl-auto`). `PATCH
  /chat/conversation/{id}/avatar` (multipart `avatar`, participant-only, non-DM) →
  `storeFile(.., "conversation", id + "-" + ts)` (**unique filename** so the URL changes and
  clients don't reuse the cached old image), broadcasts `conversation_avatar`.
  `ConversationDTO` carries `avatarUrl`; FE shows it in sidebar rows + chat header (✎ overlay
  to change; uploader also calls `loadConversations()`). DM rows/header show the **other
  user's avatar** (`dmAvatars`, resolved alongside `dmNames`), not the 💬 icon.
- Message content at-rest encryption: `utils/MessageCrypto` (AES-256-GCM, `@Component`).
  Key from `app.message.crypto-key` / `MESSAGE_CRYPTO_KEY` (base64 32 bytes); **blank = no
  encryption** (plaintext, legacy behaviour). `encrypt` prepends `enc1:` + base64(iv‖ct);
  `decrypt` returns any non-`enc1:` value untouched (so old plaintext rows keep working — no
  migration). Wired into `ChatServiceImpl` (`sendMessage` / `editMessage` encrypt;
  `toDTO` / conversation-list `lastMessage` decrypt) and `socket/SocketService` (encrypt on
  save). `MessageNotifier` and the socket `get_message` broadcast use the **plaintext request**
  string, so `@mention` parsing / previews are unaffected. Not E2EE — the server still holds
  plaintext in memory. Losing/rotating the key makes existing ciphertext unreadable.
- Mute conversation: `Participants.muted` (`ddl-auto`, `null`/`0` = notify, `1` = muted, per
  user per conversation). `PATCH /chat/conversation/{id}/mute?muted=` (`ChatServiceImpl.setConversationMuted`,
  participant-only). `MessageNotifier.notifyMessage` skips a muted participant **unless** they
  are `@`-mentioned. `ConversationDTO.muted` (the caller's flag) → FE `Chat.vue` header 🔔/🔕
  toggle + 🔕 in sidebar rows.
- "Đã chỉnh sửa" marker: `Posts.editedAt` / `Comments.editedAt` (`ddl-auto`, nullable). Set to
  now in `PostServiceImpl.updatePost` / `CommentServiceImpl.updateComment` (not on publish).
  `PostDTO.editedAt` / `CommentDTO.editedAt` carry it; FE shows "· đã chỉnh sửa" (post) /
  "· đã sửa" (comment) next to the timestamp.
- Realtime avatar change: `UserServiceImpl.updateUser` (and `updateCover`) name the file
  `"<userId>-<ts>"` so the URL always changes; on avatar change it broadcasts `user_avatar`
  `{userId, avtUrl}` via `RealtimeGateway.toUser` to the user + every `friendIdsOf`. FE:
  `appState.avatarUpdates` (id → new avtUrl) is populated by `TheHeader`'s notif-socket
  listener; `BaseAvatar` and `Post.vue` prefer `avatarUpdates[userId]` over the DTO value;
  `Chat.vue` watches it to patch `dmAvatars`. (`TheHeader` also updates `LINK_AVT` + its own
  avatar when the event is for the current user.)
- Chat socket (`SocketModule`, netty-socketio :8082): events `send_message`→`get_message`,
  `typing`, `seen`, `presence {userId,online}` (broadcast on join/leave; on join the new
  client is also told who is already present). `SocketService.broadcastExcept` relays to
  the room minus the sender.
- **Auth is a custom JWT filter**, not DB-backed `UserDetails`. `JwtAuthenticationFilter`
  validates the bearer token and sets the Spring `Authentication` principal to the
  **userId string**. Retrieve the current user with
  `AuthorizePathService.getUserIdAuthoried()`.
- **Sessions are per-device** (`entity/UserSessions`, table `user_sessions`, Flyway `V4`).
  One row per logged-in device: `refresh_hash` (SHA-256, never the raw token),
  `access_token` (kept only so a ban can revoke it — access JWTs are stateless),
  `remember`, `created_at`, `last_used_at`. `service/SessionService` owns all of it —
  `openSession` (login/2FA/verify), `closeSession` (logout: revokes **only** the calling
  device, identified by the raw token that `JwtAuthenticationFilter` now puts in the
  Authentication *credentials*), `revokeAllSessions` (ban / deactivate / delete account).
  `openSession` also prunes that user's rows older than the refresh TTL.
  **Before this, `users.access_token` + `users.refresh_token` were single slots**, so
  logging in on a second device blacklisted the first device's token and overwrote the
  refresh hash — the second login kicked the first out, and it could not even recover via
  `/auth/refresh`. Logout had the mirror bug (device A's logout blacklisted device B's
  token). Those two columns are now `@Transient` on `Users` (in-memory carriers to the
  controller only); V4 copies live sessions across so **nobody is logged out by the deploy**
  and deliberately does not drop the columns yet.
  `refreshAccess` rotates within one session row and deliberately does **not** blacklist the
  old access token — the client only refreshes after a 401, and blacklisting it would kick
  out other tabs on the same device.
- Refresh tokens: `JwtUtils.generateRefreshToken` mints a long-lived JWT (`jwt.refresh-expiration-ms`,
  default 30d) with a `typ=refresh` claim; `generateAccessToken` uses `jwt.access-expiration-ms`
  (default 1d). `validateAccessToken` now rejects a `typ=refresh` token and `validateRefreshToken`
  rejects a plain access token, so the two are not interchangeable. `Users.refresh_token`
  (`ddl-auto`, 512) stores the **SHA-256 hex** (`JwtUtils.sha256Hex`) of the user's current
  refresh token — never the raw value. `AuthServiceImpl.issueRefreshToken` sets the column to
  the hash and stashes the raw token in the `@Transient Users.plainRefreshToken` (returned to
  the client that one request; `AuthController` reads `getPlainRefreshToken()`).
  `POST /auth/refresh?refreshToken=` (`refreshAccess`, `/auth/**` permit list) hashes the
  incoming token and compares to the column, blacklists the raw token just used, and returns a
  **rotated** `{accessToken, refreshToken, isAdmin}`. `login` / `verifyEmail` re-issue (overwrite
  the hash — that alone revokes the previous one); `logout` just nulls the column. **Deploy
  note**: existing rows hold plaintext → those users' next refresh fails once and they re-login.
  FE: `LOCALKEYS.REFRESH_TOKEN`
  (`"RefreshToken"`); `src/storages/api.js` response interceptor, on a 401 for a request that
  had a token, calls `/auth/refresh` **once** (single-flight `refreshPromise` shared across
  concurrent 401s), updates `Token`/`RefreshToken`/`isAdmin`, and replays the original request;
  only if refresh fails does it clear `localStorage` + redirect to `/login`. `/auth/*` calls
  themselves are exempt from the retry.
- Two-factor (email code): `Users.twofa_enabled` (`ddl-auto`, `null`/`0` off, `1` on) +
  `twofa_code` / `twofa_expires`. `POST /user/2fa/enable?password=` / `/user/2fa/disable?password=`
  (`UserServiceImpl.setTwoFactor`, password-confirmed like deactivate). When on,
  `AuthServiceImpl.login` (after the password check) generates a 6-char code, saves it with a
  10-min expiry, emails it (`MailService` + `[2fa] … code=` log line), sets a `@Transient
  Users.twofaRequired` and returns **without** issuing tokens — `/auth/signin` then responds
  `{twoFactorRequired:"1", email}`. `POST /auth/2fa/verify?email=&code=`
  (`AuthServiceImpl.verifyTwoFactor`, `/auth/**` permit list, `otp-check` rate bucket) checks
  the code (case-insensitive) + expiry, clears it, and returns the full login payload
  (access+refresh, blacklisting any prior tokens; also clears `deactivated` like `login`).
  `UserDTO.twoFactorEnabled` is set **only for the owner** in `getUserById`. FE: `Login.vue`
  has a two-stage flow (`stage` `creds`→`2fa`); `EditProfile.vue` "Xác thực 2 bước" card
  toggles it (reuses the shared `currentPassword` field).
- `CustomFilterSecurity` permits `/auth/**`, `/images/**`, `/api-docs**/**`,
  `swagger-ui/**`, and **GET** `/post/get`, `/post/pages`, `/post/search`, `/post/by-tag/*`,
  `/post/hashtags/trending` (guest can
  browse the home feed without an account — the SPA's `/` route has no guard; every
  other route/endpoint still needs a valid JWT, incl. `/post/{id}` and `/user/**`).
  Session is STATELESS, CSRF off. Its `authenticationEntryPoint` writes a valid
  `{"success":false,"statusCode":401,"description":...}` JSON body (`application/json;charset=UTF-8`).
  `ResourceWebConfig.extendMessageConverters` also pins the Jackson converter to
  `application/json;charset=UTF-8` (Spring 6 drops the charset by default). The feed `PostDTO`
  carries `authorName`/`authorAvatar` so `Post.vue` cards don't call `/user/{id}` per row.
- `security/RateLimitFilter` (plain servlet filter, order `HIGHEST_PRECEDENCE+5`, runs
  before the JWT filter) throttles POST/PATCH on `/auth/**` and **POST `/report`** per
  client IP with in-memory fixed-window counters (single-instance deploy). Buckets:
  `signin` 20/5min, `signup` 6/hr, `otp-send` (`resend-verify`+`token-reset`) 5/15min,
  `otp-check` (`verify`+`reset`+`2fa/verify`) 20/10min, `report` (exact `/report`, not the
  admin routes) 10/hr, `default` 40/5min. Over limit → HTTP **429** + `Retry-After` header, body
  `{success:false,statusCode:429,description:"Bạn thao tác quá nhanh..."}` (surfaces via
  the FE's `e?.description`). Client IP from `X-Forwarded-For`/`X-Real-IP` then
  `getRemoteAddr`. Tunable via `app.ratelimit.*` props / `RATELIMIT_*` env
  (`RATELIMIT_ENABLED=false` disables).
- Real-time chat: `socket/SocketModule` registers `netty-socketio` listeners
  (`send_message` / `get_message` events, room per `conversationID`); token passed as a
  `token` URL param on the handshake. Runs on its own port (8082), separate from the
  Spring web port.
- Uploaded files land on disk at `./uploads/images/<type>/` (`app.file.upload-dir`).
  `ResourceWebConfig` serves them at `/images/**` from `file:./uploads/images/` (with a
  `classpath:/static/uploads/images/` fallback for the seeded images). Signup avatar is
  optional — `avtUrl` is set to `""` when omitted.
- `FileUploadsServiceImpl.storeFile` accepts only png/jpg/jpeg (extension + Tika mime),
  rejects `> app.file.max-size-bytes` (`UPLOAD_MAX_BYTES`, default 10MB) with a Vietnamese
  message, then `compress(...)` downscales to `app.file.max-dimension`px (`UPLOAD_MAX_DIMENSION`,
  1600) on the long edge and re-encodes (JPEG `app.file.jpeg-quality`=0.82; PNG kept as PNG,
  and left untouched if already within bounds). Undecodable/oversized-after-encode → original
  bytes stored (never blocks upload). EXIF orientation is NOT applied. Spring's hard multipart
  cap is `UPLOAD_MULTIPART_MAX` (12MB); `controller/UploadExceptionHandler`
  (`@RestControllerAdvice`) turns `MaxUploadSizeExceededException` / `MultipartException` into
  the standard 503 + `statusCode:500` ResponseData body.
- **DB schema is Flyway-managed** (`flyway-core` + `flyway-mysql`, version via Spring Boot
  3.1.7 BOM). Migrations in `social/src/main/resources/db/migration/` (`V1__baseline.sql` =
  point-in-time dump of the prod schema, 21 tables, no data; `V2`+`V3` collapse the two
  duplicate `messages` image columns down to the one the entity actually uses, `message_img`).
  `spring.flyway.enabled` and
  `spring.jpa.hibernate.ddl-auto` are env-gated: defaults `FLYWAY_ENABLED=true` +
  `DDL_AUTO=validate` (Hibernate only checks entity↔table match, never `ALTER`s). Set
  `DDL_AUTO=update` + `FLYWAY_ENABLED=false` in `.env` to fall back to the old behaviour.
  `baseline-on-migrate=true` + `baseline-version=1` → an existing DB (no
  `flyway_schema_history`) is marked at V1 and V1 is **not** re-run; only V2+ apply. New
  schema change = add `V<n>__*.sql`, never edit an applied one. `validate` is strict about
  a mapped column/table being **missing**, lenient about extra columns, length, index/FK
  names, nullability. (Watch out: Spring Boot's `CamelCaseToUnderscoresNamingStrategy` is
  applied to explicit `@Column(name=…)` values too — so `Messages`' `@Column(name="messageImg")`
  actually maps to the physical column `message_img`, not `messageImg`.)
- Hibernate dialect is **not** configured — Hibernate 6 auto-detects `MySQLDialect` from the
  JDBC connection at startup (per HHH90000025). The old conflicting `database-platform=MySQL5Dialect`
  (ignored) + `properties.hibernate.dialect=MySQL8Dialect` (deprecated) lines were removed.

## Frontend design system ("Bảng hiệu")

`src/main.css` is the whole design layer — tokens + primitives. 12 of the 29 `.vue` files
carry no raw Tailwind colour/radius/shadow at all, so changing the tokens re-skins the app;
the rest are neutralised by a Tailwind-override block at the bottom of `main.css`
(`.bg-gray-*`, `.rounded-*`, `.shadow*`, `.text-gray-*`, `.bg-blue/red/green-*` all map onto
tokens). **Add new colour through a token, not a raw Tailwind class.**

Concept: Vietnamese public notice board / hand-painted signboard. Flat colour blocks, hard
2px borders, and a **solid offset shadow** (`--lift: 5px 5px 0`) — never a blurred grey one
(`--shadow` is now `none`; three popovers that referenced it were moved to `--lift`).

Palette (`--ink` #0E3B3E, `--paper` #FCF8ED, `--turmeric` #F2B01E, `--cinnabar` #D6402F).
Colour encodes, it does not decorate:
- **turmeric** = the site's voice / active state (selected tab, `.seg__btn.is-on`, online,
  `.act-pill.is-live`). **Never text on a light ground** — 1.9:1. Fills only, ink text on top.
- **cinnabar** = *your* action (`.act-pill.is-on` = liked/reposted). `--cinnabar-ink` #B8291A
  is the text-safe variant (5.9:1).
- **ink** = structure. `--stroke` is a separate token from `--ink` because in dark mode
  `--ink` becomes the *background*, so borders need their own colour (#35595C).
- `--wash` (neutral ink tint) is for static surfaces; `--turmeric-wash` only for hover/selected.

Every pair passes WCAG AA in both themes, verified in-browser. Two need per-theme inversion:
`.act-pill.is-on` and `.chip--cinnabar` use white-on-cinnabar-ink in light but
**ink-on-cinnabar in dark** (white-on-cinnabar is only 3.7:1 there).

Radius is a hierarchy, not one value: `0` structural blocks · `--radius-control` 3px form
controls · `999px` identity (avatars, pills).

Type: **Be Vietnam Pro** (Google Fonts, loaded in `index.html`), one family, weights
400/500/600/800. Scale is a 1.25 major third: `--fs-xs`12 `--fs-sm`13 `--fs-ui`14
`--fs-base`16 `--fs-lg`20 `--fs-xl`25 `--fs-2xl`31 `--fs-3xl`39. `--measure` 66ch caps
reading width (`.measure`, `.post-body`); `.post-title--hero` caps at 30ch.

Key primitives: `.board` (+`--framed`/`--turmeric`/`--paper`) the signboard; `.card`
(border, **no** shadow — that's the hierarchy); `.seg` segmented control; `.act-pill`;
`.tag-chip` (+`--sm`); `.chip` (+`--ink`/`--turmeric`/`--cinnabar`/`--quiet`) for
classification; `.post-badge`; `.status-on`; `.sign-btn`; `.rule`.

**`.post-row` left tick colour encodes post kind** — turmeric = normal, cinnabar
(`--repost`) = shared, muted (`--draft`) = draft, hatched (`--hidden`).

Motion: one non-user-triggered moment only — the home banner's post count eases up once
(`Home.vue#countTo`, respects `prefers-reduced-motion`). Everything else answers an action.
Deliberately avoided: ALL-CAPS eyebrow labels (Chat's three were converted to sentence case),
`→` appended to links, mono for numerals (use `.tnum`), soft grey card shadows.

**Icons are `components/AppIcon.vue`** — one component, ~33 Feather-style stroke glyphs
described as plain data (`path`/`circle`/`rect`) and rendered with `v-for`, **never `v-html`**.
Usage: `<AppIcon name="bell" :size="18" />`; it inherits `currentColor`, so it needs no
per-state colour rules. This replaced the DevExtreme icon font (fixed `#333`, invisible on
dark, needed `!important` everywhere; and `message`/`email` shared one envelope glyph) and
emoji-as-icons (different on every OS, no stroke or colour control). **No `DxButton icon=`
remains** — icon-only buttons are `.icon-btn`, icon+label are `.sign-btn`/`.act-pill`.
Add a new glyph to the `ICONS` map, don't reach for a font or an inline `<svg>`.
The one place icons must stay emoji: `<option>` in the "Ai xem được" `<select>`
(CreatePost/EditPost) — native options render text only. Reaction/emoji pickers keep emoji
because there the emoji *is* the content.

The header nav is a **plain `<nav>` (`.hdr-tab`), not `DxTabs`** — DxTabs normalises
`selectedIndex: -1` to the *last* item, so every route outside the five tab routes
(`/post/:id`, `/tag/:tag`, `/saved`, `/drafts`, `/notifications`, `/profile/edit`) lit up
"Tìm người dùng"; `selected-item: null` does not deselect either. Don't reintroduce DxTabs here.

DevExtreme still needs `!important` where its own CSS wins: text-mode button colours for
`type="default"/"success"/"danger"`, which otherwise keep DevExtreme blue/green.

Mobile (≤720px): the header's `div.flex-1` spacer is hidden and replaced with an auto-margin
— flex-grow eats the first line's free space and pushes the avatar group onto a second row,
whereas auto-margin absorbs leftover space only *after* line-breaking. Tabs drop to their own
full-width scrollable row; tab labels hide below 1180px (icons stay).

Local dev against the deployed backend: put `DEV_API_TARGET=https://api.hipe.id.vn` +
`VITE_API_URL=/api-proxy` in `forum_fe/.env.local`. `vite.config.js` then proxies
`/api-proxy` same-origin and **strips `Origin`/`Referer`** (the backend's CORS allow-list
rejects `localhost`, which returns 403 on `/auth/signin`). Dev-server only; builds ignore it.

## Frontend architecture notes

- Entry `src/main.js` → `App.vue` → `src/router/index.js`. Routes (all under
  `beforeEnter` guard **except** `/`, `/post/:id`, and the auth pages — those two are
  public for guests, see the security note): `/`, `/login`, `/signup`, `/forgot-password`,
  `/post/:id`, `/follow` (Friends page: friends / incoming / outgoing / **blocked** tabs),
  `/users` (user search), `/user/:id` (profile), `/profile/edit`, `/chat`, `/admin`,
  `/saved`, `/notifications`, `/drafts`. Guard gates on `Token` + `UserId` in
  `localStorage`. No admin flag is exposed at login, so admin status is probed by
  calling `/admin/blacklist` (a GET only admins can run) after login and on header
  mount; the result is cached in `localStorage.isAdmin` and drives both the "Quản trị"
  header tab visibility and the `/admin` route guard (`AdminPage.vue` still self-gates).
  The router's `scrollBehavior` restores `savedPosition` on back/forward (returns `{top:0}`
  otherwise); since feed lists load async it waits for a `window` `'page:ready'` event
  (1200 ms timeout fallback) before scrolling — `Home.vue` dispatches it after
  `getListPost()` + `nextTick`. Home's feed is `?page=N`-paginated so the same page (hence
  same DOM height) re-renders on back-nav; infinite-scroll list pages (`TagPage` etc.) would
  need `<keep-alive>` to restore position the same way.
- Direct messages: `POST /chat/direct/{userId}` (added to `ChatController`/`ChatServiceImpl`)
  finds-or-creates a conversation named `dm:<sorted userIds>` and adds **both** users as
  participants. `UserProfile.vue`'s "Nhắn tin" calls it and navigates to
  `/chat?c=<id>&name=<other name>`; `Chat.vue` opens the conversation from that query and
  shows `dm:*` (and legacy `dm_*`) as "💬 Tin nhắn riêng".
- API layer: `src/storages/api.js` creates axios instances (`api`, `apiForm`, `authApi`,
  `authApiFormData`); `authApi*` inject `Authorization: Bearer <Token from localStorage>`.
  The response interceptor rejects with `error.response.data` (the `ResponseData` body),
  and on HTTP 401 (for a request that had a token) tries `POST /auth/refresh` once and
  replays the request; only if that fails does it clear `localStorage` + redirect to
  `/login` (see the refresh-token note under backend architecture).
  Endpoints are centralised in `src/config.js` (`API_URL`, `SOCKET_URL`, `IMAGE_BASE`),
  overridable via Vite env vars `VITE_API_URL` / `VITE_SOCKET_URL` in `forum_fe/.env`
  (defaults point at `localhost:8081` / `localhost:8082`). Per-feature calls live in
  `src/apis/*.js`; avatar/image `<img src>` uses `IMAGE_BASE`.
- Real-time chat (`pages/chat/Chat.vue`) uses `socket.io-client` against the backend's
  netty-socketio on `:8082`, passing `conversationID` + `token` as handshake query
  params. One socket is bound to one conversation; switching conversations reconnects.
  The server does **not** echo `get_message` to the sender, so the sender appends its
  own message locally.
- `localStorage` keys (`src/storages/localStorage.js`): `Token`, `RefreshToken`, `UserId`,
  `UserName`, `linkAvt`, `isAdmin`.
- Backend business errors return HTTP **503** with `statusCode: 500` in the body (not
  just true server errors); some "empty" cases (`/chat/conversation/alls` with no
  groups, `searchConversation`/`searchUser` with no hits) also throw → treat as empty.
- The `location /api` proxy in `nginx.conf` points at a docs page and is not the real
  data path; the SPA talks to the backend directly via the absolute `BASE_URL`.
- Path alias `@` → `src/`.

## Caution

- **`JWT_SECRET` has NO committed default anymore** — `application.properties` is
  `${JWT_SECRET}` (fail-fast) and `docker-compose.yml` is `${JWT_SECRET:?...}` (compose
  errors if unset). It must be set in `.env` (`openssl rand -base64 32`, base64-decoded to
  the HMAC key so ≥ 32 bytes). `.env.example` ships it blank. The old committed value
  `wyD7j04/…` was rotated out (Sep 2026) and is dead. Rotating it again invalidates every
  live access + refresh token → all users (and the auto-poster bot, which self-heals via
  `reauth()`) must re-login once.
- DB passwords (`MYSQL_ROOT_PASSWORD=tranhiep12345`) are still committed with working
  defaults in `docker-compose.yml` / `.env.example`. Do not add more, and be deliberate
  before rotating or echoing them.
- The SendGrid API key is NOT committed — set `SENDGRID_API_KEY` in `.env` (docker) or
  `social/.env` (when running `./mvnw spring-boot:run`; `spring-dotenv` loads it). Blank
  key = mail sending is skipped, OTP/verify codes still print to the backend log as
  `[verify] ... code=`. `SENDGRID_FROM_EMAIL` must be on a SendGrid-authenticated
  domain/sender. An OS env var of the same name overrides the `.env` file.
- Hostnames/ports are pinned to a specific deployment (`hp11.hipe.id.vn`,
  `hipe.id.vn`, `didan.id.vn`). Changing target environment means editing the hardcoded
  frontend `BASE_URL`/`.env`, `nginx.conf` `server_name`/`proxy_pass`, and the compose
  environment blocks together.
- The root `docker-compose.yml` is the only supported entrypoint (see `DEPLOY.md`).
  (Stale `social/compose.yml` / `social/docker-compose.yml` drafts were deleted Sep 2026.)

## Security notes / hardening done

- **XSS (fixed)**: `components/Dialog/MDialog.vue` used to render `content` via `v-html`.
  Several callers interpolate user-controlled names (`fullName`, group name) into the
  message → stored XSS that could exfiltrate `localStorage` tokens. Now rendered as text
  (`{{ content }}` + `white-space: pre-line`). **Never reintroduce `v-html` for dialog
  content.** Post/comment bodies are already rendered with `{{ }}` / `whitespace-pre-wrap`.
- Display strings are sanitized on write (strip `<` `>` + control chars, length cap):
  `UserServiceImpl.clean` (profile), `AuthServiceImpl.signup` (fullName),
  `ChatServiceImpl.createConversation` / `renameConversation` (group name).
- `AuthServiceImpl.login` returns the **same** "Email and Password does not match" whether
  the email exists or not, **and** runs a dummy `passwordEncoder.matches(pw, DUMMY_BCRYPT)`
  when the email is missing so response time is constant (no timing oracle). `/auth/2fa/verify`,
  `/auth/token-reset`, `/auth/verify` still reveal existence — lower priority, `otp-*` rate-limited.
- **`deletePost` FK failure (fixed Sep 2026)**: four FKs reference `posts` — `bookmarks`,
  `post_likes`, `user_comment`, `user_posts` — but the `Posts` entity only cascades the last
  three. Deleting a post anyone had **saved** violated the `bookmarks` FK and returned 503,
  so no interacted-with post could be deleted. `deletePost` now clears `post_hashtags`,
  `bookmarks` and `reposts` before `postRepository.delete`. It deliberately does **not**
  delete `reports`: `ReportServiceImpl.removeReportedTarget` calls `deletePost` and *then*
  `resolveOpenFor`, so deleting them would erase the moderation trail. Covered by
  `PostServiceImplTest#deletePostClearsBookmarksHashtagsAndRepostsBeforeDeleting`.
- **`deletePost` IDOR (fixed)**: previously deleted any post by id with no ownership check —
  any logged-in user could wipe anyone's post. Now requires author (`userPostRepository
  .findFirstByPosts_PostIdAndUsers_UserId`) or `isAdmin == 1`. `updatePost` / `publishPost`
  / `deleteComment` already checked authorship.
- **Rate-limit IP spoofing (fixed)**: `RateLimitFilter.clientIp` used to trust
  `X-Forwarded-For` unconditionally → an attacker rotating the header bypassed every
  per-IP limit. Now `app.ratelimit.trust-forwarded` (`RATELIMIT_TRUST_FORWARDED`, default
  **false**) gates it: default uses `request.getRemoteAddr()` (unspoofable); set `true`
  only behind a trusted proxy that sets `CF-Connecting-IP` / `X-Real-IP` / appends to XFF
  (then the **last** XFF entry is used, not the client-controlled first).
- CORS: `config/CorsConfig` now reads `app.cors.allowed-origins` (`APP_CORS_ALLOWED_ORIGINS`,
  comma list, default `*`) via `allowedOriginPatterns`; no `allowCredentials` (header-based
  auth). **Set it to the real frontend origins in production.**
- Auth is a hand-rolled JWT filter; the HS256 signing key is committed (see above). Admin
  endpoints are gated **in the service layer** (`AdminServiceImpl.authAdmin`,
  `ReportServiceImpl.requireAdmin`), not by a Spring `hasRole` — the JWT principal carries
  no authorities. Chat reads/writes check participant membership; post/comment edits check
  authorship. Report auto-hide needs 3 distinct reporter accounts (signup is rate-limited;
  `POST /report` is IP-rate-limited too — `report` bucket, 10/hr).
- `deletePost` requires the author (via `UserPostRepository`) or `isAdmin == 1` — was an
  unauthenticated-delete IDOR before. `RateLimitFilter` keys on `request.getRemoteAddr()` by
  default; set `app.ratelimit.trust-forwarded=true` (`RATELIMIT_TRUST_FORWARDED`) only behind
  a proxy — otherwise `X-Forwarded-For` spoofing bypasses every per-IP limit. `login` runs a
  dummy bcrypt when the email is missing so timing can't enumerate users.
- Min password length is **8** (`AuthServiceImpl.signup` + `updatePassword`; FE hints match).
- Known residual risks (not fixed): committed DB password default,
  `GET /user/{id}` exposes email to any logged-in user (by design — profile shows it).
