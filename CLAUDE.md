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

`HIP_Forum/` and `forum_fe/` differ only slightly (`nginx.conf`, `package.json`,
`Comment.vue`, `FollowVue.vue`, plus `forum_fe` has `EditComments.vue`). The root
`docker-compose.yml` builds **`HIP_Forum/`**; `social/compose.yml` builds `forum_fe/`.
When editing frontend code, confirm which copy the compose file you care about uses.

## Running

**Full stack (from repo root):**
```
docker compose up --build
```
- frontend → http://localhost:80
- backend REST → http://localhost:8081  (Swagger UI: `/api-docs.html`, spec: `/api-docs`)
- backend Socket.IO server → port 8082
- MySQL → host port 3307 (container 3306), db `socialapp`, root password in the compose file

`social/db.sql` is mounted as the MySQL init script; data persists in the `mysql-data` volume.

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
- Route prefixes: `/auth`, `/user`, `/post`, `/comment`, `/follow`, `/chat`, `/admin`.
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
- `hibernate.ddl-auto=update` — schema is auto-migrated from entities on startup.
- Hibernate dialect is set inconsistently (`MySQL5Dialect` and `MySQL8Dialect` both
  appear in `application.properties`); leave as-is unless fixing that specifically.

## Frontend architecture notes

- Entry `src/main.js` → `App.vue` → `src/router/index.js`. Routes (all under
  `beforeEnter` guard except auth pages): `/`, `/login`, `/signup`, `/forgot-password`,
  `/post/:id`, `/follow`, `/users` (user search), `/user/:id` (profile),
  `/profile/edit`, `/chat`. Guard gates on `Token` + `UserId` in `localStorage`.
- API layer: `src/storages/api.js` creates axios instances (`api`, `apiForm`, `authApi`,
  `authApiFormData`); `authApi*` inject `Authorization: Bearer <Token from localStorage>`.
  The response interceptor rejects with `error.response.data` (the `ResponseData` body),
  and on HTTP 401 clears `localStorage` + redirects to `/login`.
  **`BASE_URL` is hardcoded** in `api.js` (currently `http://localhost:8081/`); image
  URLs and the Socket.IO URL (`http://localhost:8082`, in `pages/chat/Chat.vue`) are
  hardcoded too — not read from `.env`. Per-feature calls live in `src/apis/*.js`.
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
- `social/compose.yml`, `social/docker-compose.yml`, and root `docker-compose.yml` are
  three overlapping definitions with different passwords/ports. The root one is the
  intended entrypoint.
