import { createServer } from 'http'
import { readFileSync } from 'fs'
import { resolve } from 'path'
import { createYoga } from 'graphql-yoga'
import resolvers from './resolvers'
import { warmupCache } from './cache'
import axios from 'axios'

const PORT = Number(process.env.PORT || process.env.GRAPHQL_PORT || 4000)
const API_BASE = process.env.API_BASE_URL || process.env.BACKEND_API_URL || 'http://localhost:8080/api'

// Load GraphQL schema from file
const typeDefs = readFileSync(
  resolve(__dirname, 'schema.graphql'),
  'utf-8'
)

// Create GraphQL Yoga server
const yoga = createYoga({
  schema: {
    typeDefs,
    resolvers
  },
  // CORS configuration for frontend apps
  cors: {
    origin: [
      'http://localhost:3001',
      'http://localhost:3000',
      'http://localhost:8081'
    ],
    credentials: true
  },
  // Enable GraphiQL for development
  graphiql: process.env.NODE_ENV !== 'production',
  // Context: pass request headers for auth
  context: ({ request }) => ({
    headers: Object.fromEntries(request.headers.entries())
  })
})

// Create HTTP server with /health endpoint support
const server = createServer((req, res) => {
  if (req.url === '/health') {
    res.writeHead(200, { 'Content-Type': 'application/json' })
    res.end(JSON.stringify({ status: 'ok', service: 'graphql-gateway' }))
    return
  }
  yoga(req, res)
})

server.listen(PORT, () => {
  console.log(`GraphQL Gateway running at http://localhost:${PORT}/graphql`)
  console.log(`GraphiQL playground: http://localhost:${PORT}/graphql`)

  // 缓存预热：服务启动后异步预热高频查询
  warmupCache({
    categories: async () => {
      const res = await axios.get(`${API_BASE}/products/categories`)
      return res.data?.data ?? res.data
    },
    hotProducts: async () => {
      const res = await axios.get(`${API_BASE}/products/hot`)
      return res.data?.data ?? res.data
    }
  }).catch(err => console.warn('[CacheWarmup] Warmup partially failed:', err.message))
})

export default server
