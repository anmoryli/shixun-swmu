# syntax=docker/dockerfile:1.7

# ===== Stage 1: build Vue 2 SPA =====
FROM node:20-alpine AS build
WORKDIR /workspace

COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm \
    npm ci --no-audit --no-fund

COPY . .
# Public base URL injected at build time; compose passes it as build arg.
# When MEDICINE_API_BASE_URL=/api the frontend uses same-origin /api,
# which the nginx layer below reverse-proxies to medicine-backend:8082.
ARG MEDICINE_API_BASE_URL=/api
ENV VUE_APP_API_BASE_URL=${MEDICINE_API_BASE_URL}
RUN npm run build

# ===== Stage 2: serve via nginx (handles /api reverse proxy, /actuator, SPA fallback) =====
# Switched from busybox httpd to nginx:alpine because busybox httpd lacks
# proxy_pass and try_files, which broke API calls (HTTP 404 on /api/*) and
# Vue history-mode route refresh (404 on nested paths like /user/profile/1).
FROM nginx:1.27-alpine AS runtime

# Replace default site with project config (reverse proxy + SPA fallback).
# The file lives under medical-managerment-system/nginx/default.conf so
# compose's build context (./medical-managerment-system) can resolve it.
COPY nginx/default.conf /etc/nginx/conf.d/default.conf

# Static assets built in stage 1.
COPY --from=build /workspace/dist/ /usr/share/nginx/html/

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1/actuator/health || exit 1

# nginx:alpine already starts nginx in the foreground via its own entrypoint.