FROM node:18-alpine
WORKDIR /app
RUN apk add --no-cache curl
COPY package.json server.js ./
EXPOSE 9095
USER node
CMD ["node", "server.js"]
