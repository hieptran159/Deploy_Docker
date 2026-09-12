FROM node:22-slim AS build-stage
WORKDIR /app

# URL công khai (trình duyệt gọi tới) — có thể override qua build.args trong compose
ARG VITE_API_URL=http://localhost:8081
ARG VITE_SOCKET_URL=http://localhost:8082

COPY package*.json ./
RUN npm ci
COPY . .

# .env.production được nạp ở chế độ build production và đè lên .env
RUN printf "VITE_API_URL=%s\nVITE_SOCKET_URL=%s\n" "$VITE_API_URL" "$VITE_SOCKET_URL" > .env.production
RUN npm run build

FROM nginx:stable-alpine AS production-stage
COPY --from=build-stage /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
# Bắt lỗi cú pháp nginx.conf ngay lúc build (job `images` của CI), thay vì để container
# crash-loop sau khi deploy.
RUN nginx -t
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
