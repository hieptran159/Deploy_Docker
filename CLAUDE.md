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

Small JUnit 5 unit-test suite under `social/src/test/java` — **no DB / Docker / Spring
context**, runs on plain `./mvnw test` (deps already in `spring-boot-starter-test` +
`spring-security-test`):

| Test | Covers |
|------|--------|
| `security/RateLimitFilterTest` | `RateLimitFilter` window counter (per-IP, per-bucket, XFF, GET/non-auth bypass, disabled flag) via `MockHttpServletRequest`/`MockFilterChain` |
| `service/impl/FileUploadsServiceImplTest` | upload validation + downscale/recompress + small-image passthrough + non-image reject, `MockEnvironment` + `@TempDir` |
| `service/impl/NormReactionTest` | `PostServiceImpl.normReaction` (static pkg-private) |
| `utils/EmailTemplateTest` | `EmailTemplate.otp` HTML + HTML-escaping |

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
- Draft posts: `Posts.status` (`null`/`"published"` = live, `"draft"` = draft). Every feed
  and search query carries `PostRepository.PUBLISHED` (`p.status IS NULL OR p.status =
  'published'`) so drafts never leak; `countPublished()` backs `feedPageInfo`. `createPost`
  reads `CreatePostRequest.draft` (`"true"`/`"1"`) — a draft only needs title **or** body,
  saved as `"draft"`. `getPostById` returns null for a `draft` unless the caller is its
  author. `GET /post/drafts` (mine), `PATCH /post/publish/{id}` (author-only; requires
  title+body, bumps `postedAt` to now). FE: `CreatePost.vue` "Lưu nháp" button, `/drafts`
  page (edit via `EditPost` popup / publish / delete), header tab id 6.
- Notifications: `entity/Notifications` (plain columns, no JPA relations; table
  auto-created by `ddl-auto=update`). `NotificationService.push(...)` is fire-and-forget
  and swallows its own errors so it never breaks the caller. `pushUnique` dedups on
  `(recipient,type,targetId)`, `pushUniquePerActor` on `(recipient,actor,type,targetId)`.
  Types: `FRIEND_REQUEST`, `FRIEND_ACCEPT`, `COMMENT` (targetId=postId), `COMMENT_LIKE`,
  `POST_LIKE`, `MESSAGE` (targetId=conversationId). `/auth/signin` returns `isAdmin`
  ("0"/"1") so the client no longer probes `/admin/blacklist` at login.
- Chat socket (`SocketModule`, netty-socketio :8082): events `send_message`→`get_message`,
  `typing`, `seen`, `presence {userId,online}` (broadcast on join/leave; on join the new
  client is also told who is already present). `SocketService.broadcastExcept` relays to
  the room minus the sender.
- **Auth is a custom JWT filter**, not DB-backed `UserDetails`. `JwtAuthenticationFilter`
  validates the bearer token and sets the Spring `Authentication` principal to the
  **userId string**. Retrieve the current user with
  `AuthorizePathService.getUserIdAuthoried()`.
- `CustomFilterSecurity` permits `/auth/**`, `/images/**`, `/api-docs**/**`,
  `swagger-ui/**`, and **GET** `/post/get`, `/post/pages`, `/post/search` (guest can
  browse the home feed without an account — the SPA's `/` route has no guard; every
  other route/endpoint still needs a valid JWT, incl. `/post/{id}` and `/user/**`).
  Session is STATELESS, CSRF off. The feed `PostDTO` carries `authorName`/`authorAvatar`
  so `Post.vue` cards don't call `/user/{id}` per row.
- `security/RateLimitFilter` (plain servlet filter, order `HIGHEST_PRECEDENCE+5`, runs
  before the JWT filter) throttles POST/PATCH on `/auth/**` per client IP with in-memory
  fixed-window counters (single-instance deploy). Buckets: `signin` 20/5min, `signup`
  6/hr, `otp-send` (`resend-verify`+`token-reset`) 5/15min, `otp-check` (`verify`+`reset`)
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
  and on HTTP 401 clears `localStorage` + redirects to `/login`.
  Endpoints are centralised in `src/config.js` (`API_URL`, `SOCKET_URL`, `IMAGE_BASE`),
  overridable via Vite env vars `VITE_API_URL` / `VITE_SOCKET_URL` in `forum_fe/.env`
  (defaults point at `localhost:8081` / `localhost:8082`). Per-feature calls live in
  `src/apis/*.js`; avatar/image `<img src>` uses `IMAGE_BASE`.
- Real-time chat (`pages/chat/Chat.vue`) uses `socket.io-client` against the backend's
  netty-socketio on `:8082`, passing `conversationID` + `token` as handshake query
  params. One socket is bound to one conversation; switching conversations reconnects.
  The server does **not** echo `get_message` to the sender, so the sender appends its
  own message locally.
- `localStorage` keys (`src/storages/localStorage.js`): `Token`, `UserId`, `UserName`,
  `linkAvt`.
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
