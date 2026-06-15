/**
 * Mock API Server for Tailor IS Backend
 * 模拟后端认证接口，用于前端开发调试
 */
import express from 'express'
import cors from 'cors'

const app = express()
const PORT = 8080

app.use(cors())
app.use(express.json())

// Mock user database
const MOCK_USERS = {
  admin: {
    id: 1,
    username: 'admin',
    phone: '13800138000',
    email: 'admin@tailoris.com',
    realName: '系统管理员',
    password: 'admin123',
    roles: ['SUPER_ADMIN'],
    permissions: ['*:*:*']
  }
}

// Token store
const TOKENS = {}

function generateToken(userId) {
  return 'mock_token_' + userId + '_' + Date.now()
}

// Login endpoint
app.post('/api/auth/login', (req, res) => {
  const { username, password } = req.body
  const user = MOCK_USERS[username]

  if (!user || user.password !== password) {
    return res.status(401).json({
      code: 401,
      msg: '用户名或密码错误'
    })
  }

  const token = generateToken(user.id)
  TOKENS[token] = { userId: user.id, expires: Date.now() + 1800000 }

  res.json({
    code: 200,
    msg: '操作成功',
    data: {
      token,
      userInfo: {
        id: user.id,
        username: user.username,
        phone: user.phone,
        email: user.email,
        realName: user.realName,
        roles: user.roles,
        permissions: user.permissions
      }
    }
  })
})

// Send SMS code (mock)
app.post('/api/auth/sms-code', (req, res) => {
  const { phone } = req.body
  console.log(`[Mock] 短信验证码已发送到: ${phone}`)
  res.json({ code: 200, msg: '验证码已发送' })
})

// Send email code (mock)
app.post('/api/auth/email-code', (req, res) => {
  const { email } = req.body
  console.log(`[Mock] 邮箱验证码已发送到: ${email}`)
  res.json({ code: 200, msg: '验证码已发送' })
})

// Login by code (mock)
app.post('/api/auth/login/code', (req, res) => {
  const { target, code, type } = req.body
  console.log(`[Mock] 验证码登录: type=${type}, target=${target}, code=${code}`)
  res.json({
    code: 200,
    msg: '登录成功',
    data: {
      token: generateToken('mock'),
      userInfo: {
        id: 1,
        username: 'admin',
        phone: '13800138000',
        realName: '系统管理员',
        roles: ['SUPER_ADMIN'],
        permissions: ['*:*:*']
      }
    }
  })
})

// Reset password code
app.post('/api/auth/reset/code', (req, res) => {
  res.json({ code: 200, msg: '验证码已发送' })
})

// Reset password
app.post('/api/auth/reset-password', (req, res) => {
  res.json({ code: 200, msg: '密码重置成功' })
})

// User info
app.get('/api/user/info', (req, res) => {
  const authHeader = req.headers.authorization
  if (!authHeader) {
    return res.status(401).json({ code: 401, msg: '未登录' })
  }
  const token = authHeader.replace('Bearer ', '')
  if (!TOKENS[token]) {
    return res.status(401).json({ code: 401, msg: 'Token无效' })
  }
  const user = MOCK_USERS.admin
  res.json({
    code: 200,
    data: {
      id: user.id,
      username: user.username,
      phone: user.phone,
      email: user.email,
      realName: user.realName,
      roles: user.roles,
      permissions: user.permissions
    }
  })
})

// Logout
app.post('/api/auth/logout', (req, res) => {
  const authHeader = req.headers.authorization
  if (authHeader) {
    const token = authHeader.replace('Bearer ', '')
    delete TOKENS[token]
  }
  res.json({ code: 200, msg: '登出成功' })
})

// Register endpoints (mock)
app.post('/api/auth/register/phone', (req, res) => {
  res.json({ code: 200, msg: '注册成功' })
})

app.post('/api/auth/register/email', (req, res) => {
  res.json({ code: 200, msg: '注册成功' })
})

// Dashboard stats (mock)
app.get('/api/dashboard/stats', (req, res) => {
  res.json({
    code: 200,
    data: {
      totalUsers: 1234,
      totalOrders: 5678,
      totalRevenue: 123456.78,
      activeMerchants: 89
    }
  })
})

// User list (mock)
app.get('/api/admin/users', (req, res) => {
  res.json({
    code: 200,
    data: {
      total: 1,
      list: [{
        id: 1,
        username: 'admin',
        phone: '13800138000',
        email: 'admin@tailoris.com',
        status: 1,
        createTime: '2026-01-01 00:00:00'
      }]
    }
  })
})

app.listen(PORT, () => {
  console.log(`Mock API Server running on http://localhost:${PORT}`)
  console.log(`Default account: admin / admin123`)
})
