# syntax=docker/dockerfile:1.7

FROM node:20-alpine AS build
WORKDIR /workspace

COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm \
    npm ci --no-audit --no-fund

COPY . .
ARG MEDICINE_API_BASE_URL=http://localhost:8082/api
ENV VUE_APP_API_BASE_URL=${MEDICINE_API_BASE_URL}
RUN npm run build

FROM busybox:1.36.1-musl AS runtime
COPY --from=build /workspace/dist/ /www/

EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1/index.html || exit 1
CMD ["httpd", "-f", "-p", "80", "-h", "/www"]
