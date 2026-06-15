# syntax=docker/dockerfile:1

# ==============================================================================
# Tailor IS GraphQL Gateway - Production Dockerfile
# 基础镜像: node:20-alpine
# 多阶段构建: builder (编译 TS) -> runner (运行编译后的 JS)
# ==============================================================================

# -------- Stage 1: Builder --------
FROM node:20-alpine AS builder

WORKDIR /app

# 安装依赖
COPY package.json package-lock.json* ./
RUN npm ci

# 复制源码并编译 TypeScript
COPY . .
RUN npm run build

# -------- Stage 2: Runner (生产镜像) --------
FROM node:20-alpine AS runner

# 非 root 用户增强安全性
RUN addgroup -S nodejs && adduser -S nodejs -G nodejs

WORKDIR /app

# 环境变量
ENV NODE_ENV=production \
    PORT=4000 \
    TZ=Asia/Shanghai

# 仅复制生产依赖和编译产物
COPY package.json package-lock.json* ./
RUN npm ci --only=production && npm cache clean --force

COPY --from=builder /app/dist ./
COPY --from=builder /app/schema.graphql ./

# 暴露端口
EXPOSE 4000

# 健康检查
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:4000/health || exit 1

# 切换到非 root 用户
USER nodejs

# 启动命令
CMD ["node", "index.js"]
