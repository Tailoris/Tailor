<template>
  <view class="register-page">
    <view class="register-header">
      <text class="logo">🧵</text>
      <text class="title">注册账号</text>
    </view>
    
    <view class="register-form">
      <view class="form-item">
        <view class="input-wrap">
          <text class="label">+86</text>
          <input type="number" maxlength="11" placeholder="请输入手机号" v-model="form.phone" class="input"></input>
        </view>
      </view>
      
      <view class="form-item">
        <view class="input-wrap">
          <input type="number" placeholder="请输入验证码" v-model="form.code" class="input"></input>
          <text class="sms-btn" :class="{ disabled: countdown > 0 }" @click="sendSms">
            {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
          </text>
        </view>
      </view>
      
      <view class="form-item">
        <view class="input-wrap">
          <input type="password" placeholder="请设置密码(6-20位)" v-model="form.password" class="input"></input>
        </view>
      </view>
      
      <view class="form-item">
        <view class="input-wrap">
          <input type="password" placeholder="请确认密码" v-model="form.confirmPassword" class="input"></input>
        </view>
      </view>
      
      <view class="agreement-item">
        <text class="check-icon" :class="{ checked: agreed }" @click="agreed = !agreed">{{ agreed ? '☑' : '☐' }}</text>
        <text class="text">我已阅读并同意</text>
        <text class="link-text">《用户服务协议》</text>
        <text class="text">和</text>
        <text class="link-text">《隐私政策》</text>
      </view>
      
      <button class="register-btn" @click="handleRegister" :loading="loading">立即注册</button>
      
      <view class="login-link">
        <text>已有账号?</text>
        <text class="link" @click="goLogin">立即登录</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { register, sendSmsCode } from '@/api/auth'

const loading = ref(false)
const countdown = ref(0)
const agreed = ref(false)
const form = ref({
  phone: '',
  code: '',
  password: '',
  confirmPassword: ''
})

let timer = null

function sendSms() {
  if (countdown.value > 0) return
  
  if (!form.value.phone || form.value.phone.length !== 11) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
    return
  }
  
  sendSmsCode({ phone: form.value.phone, type: 'register' })
    .then(() => {
      uni.showToast({ title: '验证码已发送', icon: 'success' })
      countdown.value = 60
      timer = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) clearInterval(timer)
      }, 1000)
    })
    .catch(e => {
      uni.showToast({ title: e.message || '发送失败', icon: 'none' })
    })
}

function handleRegister() {
  if (!form.value.phone || form.value.phone.length !== 11) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
    return
  }
  
  if (!form.value.code) {
    uni.showToast({ title: '请输入验证码', icon: 'none' })
    return
  }
  
  if (!form.value.password || form.value.password.length < 6) {
    uni.showToast({ title: '密码长度不能少于6位', icon: 'none' })
    return
  }
  
  if (form.value.password !== form.value.confirmPassword) {
    uni.showToast({ title: '两次密码输入不一致', icon: 'none' })
    return
  }
  
  if (!agreed.value) {
    uni.showToast({ title: '请先阅读并同意用户协议', icon: 'none' })
    return
  }
  
  loading.value = true
  
  register({
    phone: form.value.phone,
    code: form.value.code,
    password: form.value.password
  })
    .then(() => {
      uni.showToast({ title: '注册成功', icon: 'success' })
      setTimeout(() => {
        uni.navigateBack()
      }, 500)
    })
    .catch(e => {
      uni.showToast({ title: e.message || '注册失败', icon: 'none' })
    })
    .finally(() => {
      loading.value = false
    })
}

function goLogin() {
  uni.navigateBack()
}
</script>

<style lang="scss" scoped>
.register-page {
  min-height: 100vh;
  background: #fff;
  padding: 0 40rpx;
}

.register-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 120rpx;
  margin-bottom: 60rpx;
  
  .logo {
    font-size: 100rpx;
    margin-bottom: 20rpx;
  }
  
  .title {
    font-size: 44rpx;
    font-weight: bold;
    color: #333;
  }
}

.register-form {
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
  
  .agreement-item {
    display: flex;
    align-items: flex-start;
    margin: 40rpx 0;
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
  
  .register-btn {
    background: #FF4D4F;
    color: #fff;
    border: none;
    border-radius: 50rpx;
    height: 90rpx;
    line-height: 90rpx;
    font-size: 32rpx;
  }
  
  .login-link {
    text-align: center;
    margin-top: 40rpx;
    
    text:first-child {
      color: #999;
      font-size: 28rpx;
    }
    
    .link {
      color: #FF4D4F;
      font-size: 28rpx;
      margin-left: 10rpx;
    }
  }
}
</style>
