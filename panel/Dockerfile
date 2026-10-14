FROM node:26.11.1-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginx:1.31.6-alpine

RUN apk upgrade --no-cache

COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

RUN chown -R 1000:1000 /usr/share/nginx/html /etc/nginx/conf.d \
      /var/cache/nginx /var/run /run /tmp

USER 1000

EXPOSE 8080

# Docker only understands 0 (healthy) and 1 (unhealthy); wget also exits
# with 4 or 8, hence the shell around it.
HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 \
  CMD ["sh", "-c", "wget -qO /dev/null http://127.0.0.1:8080/ || exit 1"]