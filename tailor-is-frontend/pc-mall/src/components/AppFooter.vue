<template>
  <footer
    class="app-footer"
    role="contentinfo"
    aria-label="页脚"
  >
    <div class="footer-container">
      <div class="footer-newsletter">
        <div class="newsletter-content">
          <div class="newsletter-icon">
            <el-icon><Message /></el-icon>
          </div>
          <div class="newsletter-text">
            <h4>{{ $t('footer.newsletterTitle') }}</h4>
            <p>{{ $t('footer.newsletterDesc') }}</p>
          </div>
          <div class="newsletter-form">
            <input
              v-model="email"
              type="email"
              :placeholder="$t('footer.emailPlaceholder')"
              aria-label="输入邮箱订阅"
              @keyup.enter="subscribe"
            >
            <button
              class="subscribe-btn"
              @click="subscribe"
              :disabled="isSubscribing"
            >
              <span v-if="isSubscribing">{{ $t('common.loading') }}</span>
              <span v-else>{{ $t('footer.subscribe') }}</span>
            </button>
          </div>
        </div>
      </div>
      <div
        class="footer-content"
        role="navigation"
        aria-label="页脚导航"
      >
        <div class="footer-column brand-column">
          <div class="brand-info">
            <router-link
              to="/"
              class="footer-logo"
              aria-label="裁智云 Tailor IS 首页"
            >
              <span
                class="logo-icon"
                aria-hidden="true"
              >✂️</span>
              <span class="logo-text">裁智云 Tailor IS</span>
            </router-link>
            <p class="brand-description">
              {{ $t('footer.brandDesc') }}
            </p>
            <div class="social-links">
              <a
                href="#"
                class="social-link"
                aria-label="微信"
                target="_blank"
                rel="noopener noreferrer"
              >
                <el-icon><ChatDotRound /></el-icon>
              </a>
              <a
                href="#"
                class="social-link"
                aria-label="微博"
                target="_blank"
                rel="noopener noreferrer"
              >
                <el-icon><Share /></el-icon>
              </a>
              <a
                href="#"
                class="social-link"
                aria-label="抖音"
                target="_blank"
                rel="noopener noreferrer"
              >
                <el-icon><VideoCamera /></el-icon>
              </a>
              <a
                href="#"
                class="social-link"
                aria-label="Instagram"
                target="_blank"
                rel="noopener noreferrer"
              >
                <el-icon><Camera /></el-icon>
              </a>
            </div>
          </div>
        </div>
        <div class="footer-column">
          <h4>{{ $t('footer.brand') }}</h4>
          <ul>
            <li>
              <router-link to="/about">
                {{ $t('footer.about') }}
              </router-link>
            </li>
            <li>
              <router-link to="/brand-story">
                {{ $t('footer.brandStory') }}
              </router-link>
            </li>
            <li>
              <router-link to="/join-us">
                {{ $t('footer.joinUs') }}
              </router-link>
            </li>
            <li>
              <router-link to="/news">
                {{ $t('footer.news') }}
              </router-link>
            </li>
          </ul>
        </div>
        <div class="footer-column">
          <h4>{{ $t('footer.support') }}</h4>
          <ul>
            <li>
              <router-link to="/shopping-guide">
                {{ $t('footer.shoppingGuide') }}
              </router-link>
            </li>
            <li>
              <router-link to="/payment">
                {{ $t('footer.payment') }}
              </router-link>
            </li>
            <li>
              <router-link to="/delivery">
                {{ $t('footer.delivery') }}
              </router-link>
            </li>
            <li>
              <router-link to="/after-sale">
                {{ $t('footer.afterSale') }}
              </router-link>
            </li>
          </ul>
        </div>
        <div class="footer-column">
          <h4>{{ $t('footer.merchant') }}</h4>
          <ul>
            <li>
              <router-link to="/merchant-apply">
                {{ $t('footer.merchantApply') }}
              </router-link>
            </li>
            <li>
              <router-link to="/merchant-rules">
                {{ $t('footer.merchantRules') }}
              </router-link>
            </li>
            <li>
              <router-link to="/merchant-backend">
                {{ $t('footer.merchantBackend') }}
              </router-link>
            </li>
            <li>
              <router-link to="/merchant-training">
                {{ $t('footer.merchantTraining') }}
              </router-link>
            </li>
          </ul>
        </div>
        <div class="footer-column">
          <h4>{{ $t('footer.contact') }}</h4>
          <ul>
            <li class="contact-item">
              <el-icon><Phone /></el-icon>
              <span>{{ $t('footer.hotline') }}</span>
            </li>
            <li class="contact-item">
              <el-icon><Clock /></el-icon>
              <span>{{ $t('footer.workTime') }}</span>
            </li>
            <li class="contact-item">
              <el-icon><Message /></el-icon>
              <span>{{ $t('footer.email') }}</span>
            </li>
            <li class="contact-item">
              <el-icon><MapLocation /></el-icon>
              <span>{{ $t('footer.address') }}</span>
            </li>
          </ul>
        </div>
      </div>
      <div class="footer-bottom">
        <div class="bottom-left">
          <p>&copy; 2024 Tailor IS {{ $t('footer.allRights') }}</p>
          <div class="legal-links">
            <router-link to="/privacy">
              {{ $t('footer.privacy') }}
            </router-link>
            <span class="link-divider">|</span>
            <router-link to="/terms">
              {{ $t('footer.terms') }}
            </router-link>
            <span class="link-divider">|</span>
            <router-link to="/cookies">
              {{ $t('footer.cookies') }}
            </router-link>
          </div>
        </div>
        <div class="bottom-right">
          <div class="certifications">
            <span class="cert-item">{{ $t('footer.icp') }}</span>
            <span class="cert-item">{{ $t('footer.security') }}</span>
          </div>
        </div>
      </div>
    </div>
  </footer>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { ChatDotRound, Share, Message, VideoCamera, Camera, Phone, Clock, MapLocation } from '@element-plus/icons-vue';

const email = ref('');
const isSubscribing = ref(false);

async function subscribe() {
  if (!email.value.trim()) {
    ElMessage.warning('请输入邮箱地址');
    return;
  }
  
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(email.value)) {
    ElMessage.warning('请输入有效的邮箱地址');
    return;
  }
  
  isSubscribing.value = true;
  try {
    await new Promise((resolve) => setTimeout(resolve, 1500));
    ElMessage.success('订阅成功，感谢您的关注！');
    email.value = '';
  } catch {
    ElMessage.error('订阅失败，请稍后重试');
  } finally {
    isSubscribing.value = false;
  }
}
</script>

<style scoped>
.app-footer {
  background: #1a1a2e;
  color: #ccc;
  padding: 40px 0 20px;
  margin-top: 40px;
}
.footer-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}
.footer-newsletter {
  background: linear-gradient(135deg, #1d39c4 0%, #3b82f6 100%);
  border-radius: 12px;
  padding: 32px;
  margin-bottom: 40px;
}
.newsletter-content {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}
.newsletter-icon {
  width: 56px;
  height: 56px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}
.newsletter-text h4 {
  margin: 0 0 4px 0;
  color: #fff;
  font-size: 18px;
}
.newsletter-text p {
  margin: 0;
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
}
.newsletter-form {
  flex: 1;
  min-width: 300px;
  display: flex;
  gap: 8px;
}
.newsletter-form input {
  flex: 1;
  padding: 12px 16px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
}
.subscribe-btn {
  padding: 12px 24px;
  background: #fff;
  color: #1d39c4;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}
.subscribe-btn:hover:not(:disabled) {
  opacity: 0.9;
}
.subscribe-btn:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}
.footer-content {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 40px;
  margin-bottom: 30px;
}
.brand-column {
  grid-column: span 1;
}
.brand-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.footer-logo {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  font-weight: 700;
  font-size: 18px;
  color: #fff;
}
.logo-icon {
  font-size: 20px;
}
.brand-description {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  color: #94a3b8;
}
.social-links {
  display: flex;
  gap: 12px;
}
.social-link {
  width: 40px;
  height: 40px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  transition: all 0.2s;
}
.social-link:hover {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
}
.footer-column h4 {
  color: #fff;
  font-size: 16px;
  margin-bottom: 16px;
}
.footer-column ul {
  list-style: none;
  padding: 0;
}
.footer-column ul li {
  margin-bottom: 10px;
}
.footer-column ul li a {
  color: #ccc;
  text-decoration: none;
  font-size: 14px;
  transition: color 0.2s;
}
.footer-column ul li a:hover {
  color: #fff;
}
.contact-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
.contact-item .el-icon {
  color: #1d39c4;
  flex-shrink: 0;
}
.footer-bottom {
  border-top: 1px solid #333;
  padding-top: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}
.bottom-left p {
  margin: 4px 0;
  font-size: 12px;
  color: #888;
}
.legal-links {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
}
.legal-links a {
  color: #888;
  text-decoration: none;
}
.legal-links a:hover {
  color: #ccc;
}
.link-divider {
  color: #444;
}
.certifications {
  display: flex;
  gap: 16px;
}
.cert-item {
  font-size: 12px;
  color: #888;
  padding: 4px 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 4px;
}
@media (max-width: 1024px) {
  .footer-content {
    grid-template-columns: repeat(3, 1fr);
  }
  .brand-column {
    grid-column: span 3;
  }
}
@media (max-width: 768px) {
  .newsletter-content {
    flex-direction: column;
    text-align: center;
  }
  .newsletter-form {
    width: 100%;
    min-width: auto;
  }
  .footer-content {
    grid-template-columns: repeat(2, 1fr);
    gap: 24px;
  }
  .brand-column {
    grid-column: span 2;
  }
  .footer-bottom {
    flex-direction: column;
    text-align: center;
  }
}
@media (max-width: 480px) {
  .footer-content {
    grid-template-columns: 1fr;
  }
  .brand-column {
    grid-column: span 1;
  }
}
</style>