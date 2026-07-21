FROM node:22-alpine AS build
RUN corepack enable
WORKDIR /workspace
COPY package.json pnpm-workspace.yaml tsconfig.base.json ./
COPY packages ./packages
COPY apps/mall-web ./apps/mall-web
RUN pnpm install --frozen-lockfile=false
RUN pnpm --filter mall-web build

FROM nginx:1.27-alpine
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/apps/mall-web/dist /usr/share/nginx/html
EXPOSE 80
