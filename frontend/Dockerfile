# =====================================================================
# 前端镜像：Vue3 构建 + Nginx 托管
# Nginx 配置通过 docker-compose 卷挂载 deploy/nginx/nginx.conf
# =====================================================================
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json ./
RUN npm install --registry=https://registry.npmmirror.com
COPY . .
RUN npm run build

FROM nginx:1.25-alpine
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
