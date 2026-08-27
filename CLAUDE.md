# CLAUDE.md

Guidance for working in this repository.

## What this repo is

A **deployment/orchestration repo** that bundles a full forum ("social") application so it
can be run with Docker Compose. It vendors point-in-time snapshots of three upstream
projects (each has its own GitHub repo per `README.md`):

| Path         | Role     | Stack                                               |
|--------------|----------|-----------------------------------------------------|
| `HIP_Forum/` | Frontend | Vue 3 + Vite 5, Pinia, vue-router, Tailwind, DevExtreme |
| `forum_fe/`  | Frontend | Near-identical second copy of the frontend (see note below) |
| `social/`    | Backend  | Spring Boot 3.1.7, Java 17, JPA/Hibernate, MySQL 8, netty-socketio, SendGrid |

`socialdata.sql` (root), `social/db.sql`, `social/db1.sql` are database dumps/seeds.

### Two frontend copies

`HIP_Forum/` and `forum_fe/` differ (`nginx.conf`, `package.json`, several components).
**`forum_fe/` is the one that's actively developed and the one the root
`docker-compose.yml` builds** — `HIP_Forum/` is a stale earlier copy; ignore it unless
told otherwise.

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
cd HIP_Forum
npm install
npm run dev       # Vite dev server
npm run build     # → dist/, served by nginx in the Docker image
```

## Tests

There is effectively no test suite — only `social/src/test/.../SocialApplicationTests.java`
(empty context-load test). There are no frontend tests.

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
  `swagger-ui/**`; every other request needs a valid JWT. Session is STATELESS, CSRF off.
- Real-time chat: `socket/SocketModule` registers `netty-socketio` listeners
  (`send_message` / `get_message` events, room per `conversationID`); token passed as a
  `token` URL param on the handshake. Runs on its own port (8082), separate from the
  Spring web port.
- Uploaded files land on disk at `./uploads/images/<type>/` (`app.file.upload-dir`).
  `ResourceWebConfig` serves them at `/images/**` from `file:./uploads/images/` (with a
  `classpath:/static/uploads/images/` fallback for the seeded images). Signup avatar is
  optional — `avtUrl` is set to `""` when omitted.
- `hibernate.ddl-auto=update` — schema is auto-migrated from entities on startup.
- Hibernate dialect is set inconsistently (`MySQL5Dialect` and `MySQL8Dialect` both
  appear in `application.properties`); leave as-is unless fixing that specifically.

## Frontend architecture notes

- Entry `src/main.js` → `App.vue` → `src/router/index.js`. Routes (all under
  `beforeEnter` guard except auth pages): `/`, `/login`, `/signup`, `/forgot-password`,
  `/post/:id`, `/follow` (Friends page: friends / incoming / outgoing tabs),
  `/users` (user search), `/user/:id` (profile),
  `/profile/edit`, `/chat`, `/admin`. Guard gates on `Token` + `UserId` in
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
  SendGrid API key, DB passwords). Do not add more, and be deliberate before rotating or
  echoing them.
- Hostnames/ports are pinned to a specific deployment (`hp11.hipe.id.vn`,
  `hipe.id.vn`, `didan.id.vn`). Changing target environment means editing the hardcoded
  frontend `BASE_URL`/`.env`, `nginx.conf` `server_name`/`proxy_pass`, and the compose
  environment blocks together.
- `social/compose.yml`, `social/docker-compose.yml` are stale earlier drafts. The root
  `docker-compose.yml` is the only supported entrypoint (see `DEPLOY.md`).
