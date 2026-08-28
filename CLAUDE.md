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
- backend REST → http://localhost:8081  (Swagger UI: `/api-docs.html`, spec: `/api-docs`)
- backend Socket.IO server → port 8082
- MySQL → host port 3307 (container 3306), db `socialapp`, root pw from `.env`

The compose file: `db` has a TCP healthcheck and `backend` waits on
`condition: service_healthy`; `social/db.sql` is the one-time MySQL init (fresh volume
only); data persists in the `mysql-data` volume and uploaded images in the `uploads`
volume (`/app/uploads` in the backend container). Overridable env vars live in `.env`
(`*_PORT`, `MYSQL_*`, `JWT_SECRET`, `SENDGRID_*`, and `PUBLIC_API_URL`/`PUBLIC_SOCKET_URL`
which are **baked into the frontend build** as `VITE_API_URL`/`VITE_SOCKET_URL`).
DB export/import for infra migration: `scripts/db-export.sh` / `scripts/db-import.sh`.
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

JUnit 5 unit-test suite (68 tests) under `social/src/test/java` — **no DB / Docker / Spring
context**, runs on plain `./mvnw test` (deps already in `spring-boot-starter-test` +
`spring-security-test`). Service tests use `@ExtendWith(MockitoExtension.class)` +
`@MockitoSettings(strictness = LENIENT)`, mock every constructor dep, and instantiate the
impl directly in `@BeforeEach`:

| Test | Covers |
|------|--------|
| `security/RateLimitFilterTest` | `RateLimitFilter` window counter (per-IP, per-bucket, XFF, GET/non-auth bypass, disabled flag) via `MockHttpServletRequest`/`MockFilterChain` |
| `service/impl/FileUploadsServiceImplTest` | upload validation + downscale/recompress + small-image passthrough + non-image reject, `MockEnvironment` + `@TempDir` |
| `service/impl/NormReactionTest` | `PostServiceImpl.normReaction` (static pkg-private) |
| `service/impl/FollowServiceImplTest` | block/unblock guards (self, already-blocked no-op, not-blocked reject), `friendStatus` block direction (`blocked_out`/`blocked_in`/`self`/`none`), `isBlockedEither`, `sendRequest` guards (blocked, deactivated target, self) — Mockito, no context |
| `service/impl/PostServiceImplTest` | `createPost` visibility (`friends`/default `public`) + draft rules (title-only ok, empty rejected, published missing body); `publishPost` guards (non-author, already published, missing content, happy path); `repost` guards (friends-only, own post, draft, idempotent, saves + notifies) — `ArgumentCaptor<Posts>` |
| `service/impl/ReportServiceImplTest` | `ReportServiceImpl.create` auto-hide threshold: invalid type, below threshold no-hide, at threshold sets `status=hidden` + saves, already-hidden untouched, duplicate open report no-op, threshold `0` disables — `ReflectionTestUtils` for `@Value autoHideThreshold` |
| `service/impl/NotificationServiceImplTest` | `listMinePaged` page/size clamping (negative page→0, size<1→20, size>50→50) via `ArgumentCaptor<Pageable>` |
| `utils/EmailTemplateTest` | `EmailTemplate.otp` HTML + HTML-escaping |
| `utils/HashtagUtilsTest` | `HashtagUtils.extract` (lowercase/dedup, Unicode + `_`, skip all-digit, 20-tag cap) + `normalize` (strip `#`, reject spaces/empty/all-digit/null) |
| `utils/JwtUtilsTest` | refresh-token round-trip; `validateRefreshToken` rejects an access token; `validateAccessToken` rejects a refresh token; blacklisted refresh token rejected — `ReflectionTestUtils` for `@Value` secret/expiry |
| `service/impl/AuthServiceImplTest` | `login` withholds tokens + saves a code + sets `twofaRequired` when 2FA on (issues tokens when off); `verifyTwoFactor` guards (wrong code, expired, 2FA disabled) + happy path (case-insensitive code, clears code, issues access+refresh) — 8 mocked ctor deps |

`SocialApplicationTests` (`@SpringBootTest` context-load) is the exception — it needs a
running MySQL on the configured host, so `./mvnw test` fails without one; the normal build
uses `./mvnw install -DskipTests`. No frontend tests.

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
- Post visibility: `Posts.visibility` (`null`/`"public"` = everyone, `"friends"` = author +
  author's friends). Every feed/search/profile query also carries `PostRepository.VISIBLE`
  (`p.visibility IS NULL OR = 'public' OR p.userPost.users.userId IN :vids`) where `:vids` =
  `followService.friendIdsOf(viewer) ∪ {viewer}` (or `["-"]` for a guest — non-empty for
  the native `IN`). `getPostById` gates a `friends` post to author/friend-of-author; a
  `friends` post cannot be reposted. Friends feed needs no `:vids` filter (already
  friends-only). `CreatePostRequest`/`EditPostRequest` carry `visibility`; FE `CreatePost`/
  `EditPost` have a "Ai xem được" select; `Post.vue`/`PostDetail.vue` show a "👥 Bạn bè" badge.
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
- Refresh tokens: `JwtUtils.generateRefreshToken` mints a long-lived JWT (`jwt.refresh-expiration-ms`,
  default 30d) with a `typ=refresh` claim; `generateAccessToken` uses `jwt.access-expiration-ms`
  (default 1d). `validateAccessToken` now rejects a `typ=refresh` token and `validateRefreshToken`
  rejects a plain access token, so the two are not interchangeable. `Users.refresh_token`
  (`ddl-auto`, 512) stores the user's **current** refresh token — `POST /auth/refresh?refreshToken=`
  (`AuthServiceImpl.refreshAccess`, in the `/auth/**` permit list) checks it matches that column,
  blacklists the old refresh token, and returns a **rotated** `{accessToken, refreshToken, isAdmin}`.
  `login` / `verifyEmail` issue a refresh token alongside the access token (and blacklist any
  prior one); `logout` blacklists it and nulls the column. FE: `LOCALKEYS.REFRESH_TOKEN`
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
  Session is STATELESS, CSRF off. The feed `PostDTO` carries `authorName`/`authorAvatar`
  so `Post.vue` cards don't call `/user/{id}` per row.
- `security/RateLimitFilter` (plain servlet filter, order `HIGHEST_PRECEDENCE+5`, runs
  before the JWT filter) throttles POST/PATCH on `/auth/**` per client IP with in-memory
  fixed-window counters (single-instance deploy). Buckets: `signin` 20/5min, `signup`
  6/hr, `otp-send` (`resend-verify`+`token-reset`) 5/15min, `otp-check` (`verify`+`reset`+`2fa/verify`)
  20/10min, `default` 40/5min. Over limit → HTTP **429** + `Retry-After` header, body
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
- `hibernate.ddl-auto=update` — schema is auto-migrated from entities on startup.
- Hibernate dialect is set inconsistently (`MySQL5Dialect` and `MySQL8Dialect` both
  appear in `application.properties`); leave as-is unless fixing that specifically.

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

- Secrets are committed in `docker-compose.yml`, `social/compose.yml`,
  `social/src/main/resources/application.properties`, and `.env` files (JWT signing key,
  DB passwords). Do not add more, and be deliberate before rotating or echoing them.
- The SendGrid API key is NOT committed — set `SENDGRID_API_KEY` in `.env` (docker) or
  `social/.env` (when running `./mvnw spring-boot:run`; `spring-dotenv` loads it). Blank
  key = mail sending is skipped, OTP/verify codes still print to the backend log as
  `[verify] ... code=`. `SENDGRID_FROM_EMAIL` must be on a SendGrid-authenticated
  domain/sender. An OS env var of the same name overrides the `.env` file.
- Hostnames/ports are pinned to a specific deployment (`hp11.hipe.id.vn`,
  `hipe.id.vn`, `didan.id.vn`). Changing target environment means editing the hardcoded
  frontend `BASE_URL`/`.env`, `nginx.conf` `server_name`/`proxy_pass`, and the compose
  environment blocks together.
- `social/compose.yml`, `social/docker-compose.yml` are stale earlier drafts. The root
  `docker-compose.yml` is the only supported entrypoint (see `DEPLOY.md`).
