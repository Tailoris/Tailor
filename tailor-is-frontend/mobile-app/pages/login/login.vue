<template>
  <view class="login-page">
    <view class="login-header">
      <text class="logo">🧵</text>
      <text class="title">Tailor IS</text>
      <text class="subtitle">智能定制商城</text>
    </view>
    
    <view class="login-tabs">
      <text class="tab" :class="{ active: activeTab === 'password' }" @click="activeTab = 'password'">密码登录</text>
      <text class="tab" :class="{ active: activeTab === 'sms' }" @click="activeTab = 'sms'">验证码登录</text>
    </view>
    
    <view class="login-form">
      <view class="form-item" v-if="activeTab === 'sms'">
        <view class="input-wrap">
          <text class="label">+86</text>
          <input type="number" maxlength="11" placeholder="请输入手机号" v-model="form.phone" class="input"></input>
        </view>
      </view>
      
      <view class="form-item" v-if="activeTab === 'password'">
        <view class="input-wrap">
          <text class="label">+86</text>
          <input type="number" maxlength="11" placeholder="请输入手机号" v-model="form.phone" class="input"></input>
        </view>
      </view>
      
      <view class="form-item" v-if="activeTab === 'password'">
        <view class="input-wrap">
          <input type="password" placeholder="请输入密码" v-model="form.password" class="input"></input>
        </view>
      </view>
      
      <view class="form-item" v-if="activeTab === 'sms'">
        <view class="input-wrap">
          <input type="number" placeholder="请输入验证码" v-model="form.code" class="input"></input>
          <text class="sms-btn" :class="{ disabled: countdown > 0 }" @click="sendSms">
            {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
          </text>
        </view>
      </view>
      
      <button class="login-btn" @click="handleLogin" :loading="loading">
        {{ activeTab === 'password' ? '登录' : '登录' }}
      </button>
      
      <view class="login-footer">
        <text class="link" @click="goRegister">立即注册</text>
        <text class="divider">|</text>
        <text class="link">忘记密码?</text>
      </view>
      
      <view class="agreement">
        <text class="check-icon" :class="{ checked: agreed }" @click="agreed = !agreed">{{ agreed ? '☑' : '☐' }}</text>
        <text class="text">我已阅读并同意</text>
        <text class="link-text">《用户服务协议》</text>
        <text class="text">和</text>
        <text class="link-text">《隐私政策》</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { login, sendSmsCode } from '@/api/auth'
import { encryptAsync } from '@/utils/crypto'

const activeTab = ref('password')
const loading = ref(false)
const countdown = ref(0)
const agreed = ref(false)
const form = ref({
  phone: '',
  password: '',
  code: ''
})

let timer = null

function sendSms() {
  if (countdown.value > 0) return
  
  if (!form.value.phone || form.value.phone.length !== 11) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
    return
  }
  
  sendSmsCode({ phone: form.value.phone, type: 'login' })
    .then(() => {
      uni.showToast({ title: '验证码已发送', icon: 'success' })
      countdown.value = 60
      timer = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) {
          clearInterval(timer)
        }
      }, 1000)
    })
    .catch(e => {
      uni.showToast({ title: e.message || '发送失败', icon: 'none' })
    })
}

function handleLogin() {
  if (!form.value.phone || form.value.phone.length !== 11) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
    return
  }
  
  if (activeTab.value === 'password' && !form.value.password) {
    uni.showToast({ title: '请输入密码', icon: 'none' })
    return
  }
  
  if (activeTab.value === 'sms' && !form.value.code) {
    uni.showToast({ title: '请输入验证码', icon: 'none' })
    return
  }
  
  if (!agreed.value) {
    uni.showToast({ title: '请先阅读并同意用户协议', icon: 'none' })
    return
  }
  
  loading.value = true
  
  const loginData = activeTab.value === 'password'
    ? { phone: form.value.phone, password: form.value.password }
    : { phone: form.value.phone, code: form.value.code }
  
  login(loginData)
    .then(async (res) => {
      // FE-H-2: 使用 AES-GCM 加密存储 Token
      const encryptedToken = await encryptAsync(res.data.token)
      uni.setStorageSync('token', encryptedToken)
      uni.setStorageSync('userInfo', res.data.user)
      uni.showToast({ title: '登录成功', icon: 'success' })
      setTimeout(() => {
        uni.switchTab({ url: '/pages/index/index' })
      }, 500)
    })
    .catch(e => {
      uni.showToast({ title: e.message || '登录失败', icon: 'none' })
    })
    .finally(() => {
      loading.value = false
    })
}

function goRegister() {
  uni.navigateTo({ url: '/pages/register/register' })
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  background: #fff;
  padding: 0 40rpx;
}

.login-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 150rpx;
  margin-bottom: 80rpx;
  
  .logo {
    font-size: 120rpx;
    margin-bottom: 20rpx;
  }
  
  .title {
    font-size: 48rpx;
    font-weight: bold;
    color: #333;
    margin-bottom: 12rpx;
  }
  
  .subtitle {
    font-size: 28rpx;
    color: #999;
  }
}

.login-tabs {
  display: flex;
  justify-content: center;
  margin-bottom: 60rpx;
  
  .tab {
    font-size: 32rpx;
    color: #999;
    margin: 0 40rpx;
    padding-bottom: 20rpx;
    position: relative;
    
    &.active {
      color: #333;
      font-weight: bold;
      
      &::after {
        content: '';
        position: absolute;
        bottom: 0;
        left: 50%;
        transform: translateX(-50%);
        width: 60rpx;
        height: 4rpx;
        background: #FF4D4F;
        border-radius: 2rpx;
      }
    }
  }
}

.login-form {
  .form-item {
    margin-bottom: 30rpx;
    
    .input-wrap {
      display: flex;
      align-items: center;
      height: 100rpx;
      border-bottom: 2rpx solid #eee;
      
      .label {
        font-size: 32rpx;
        color: #333;
        margin-right: 20rpx;
      }
      
      .input {
        flex: 1;
        height: 100%;
        font-size: 30rpx;
      }
      
      .sms-btn {
        color: #FF4D4F;
        font-size: 28rpx;
        padding: 12rpx 24rpx;
        border: 2rpx solid #FF4D4F;
        border-radius: 30rpx;
        
        &.disabled {
          color: #999;
          border-color: #ddd;
        }
      }
    }
  }
  
  .login-btn {
    margin-top: 60rpx;
    background: #FF4D4F;
    color: #fff;
    border: none;
    border-radius: 50rpx;
    height: 90rpx;
    line-height: 90rpx;
    font-size: 32rpx;
  }
  
  .login-footer {
    display: flex;
    justify-content: center;
    margin-top: 40rpx;
    
    .link {
      color: #FF4D4F;
      font-size: 28rpx;
    }
    
    .divider {
      color: #ddd;
      margin: 0 20rpx;
    }
  }
  
  .agreement {
    display: flex;
    align-items: flex-start;
    margin-top: 60rpx;
    flex-wrap: wrap;
    justify-content: center;
    
    .check-icon {
      font-size: 36rpx;
      margin-right: 10rpx;
      color: #ddd;
      
      &.checked {
        color: #FF4D4F;
      }
    }
    
    .text {
      font-size: 24rpx;
      color: #999;
    }
    
    .link-text {
      font-size: 24rpx;
      color: #FF4D4F;
    }
  }
}
</style>
