<template>
  <footer
    class="app-footer"
    role="contentinfo"
    aria-label="页脚"
  >
    <div class="footer-container">
      <div
        class="footer-content"
        role="navigation"
        aria-label="页脚导航"
      >
        <div class="footer-column">
          <h4>品牌介绍</h4>
          <ul>
            <li>
              <router-link to="/about">
                关于裁智云
              </router-link>
            </li>
            <li>
              <router-link to="/brand-story">
                品牌故事
              </router-link>
            </li>
            <li>
              <router-link to="/join-us">
                加入我们
              </router-link>
            </li>
            <li>
              <router-link to="/news">
                新闻动态
              </router-link>
            </li>
          </ul>
        </div>
        <div class="footer-column">
          <h4>帮助中心</h4>
          <ul>
            <li>
              <router-link to="/shopping-guide">
                购物指南
              </router-link>
            </li>
            <li>
              <router-link to="/payment">
                支付方式
              </router-link>
            </li>
            <li>
              <router-link to="/delivery">
                配送说明
              </router-link>
            </li>
            <li>
              <router-link to="/after-sale">
                售后服务
              </router-link>
            </li>
          </ul>
        </div>
        <div class="footer-column">
          <h4>商家服务</h4>
          <ul>
            <li>
              <router-link to="/merchant-apply">
                商家入驻
              </router-link>
            </li>
            <li>
              <router-link to="/merchant-rules">
                商家规则
              </router-link>
            </li>
            <li>
              <router-link to="/merchant-backend">
                商家后台
              </router-link>
            </li>
            <li>
              <router-link to="/merchant-training">
                商家培训
              </router-link>
            </li>
          </ul>
        </div>
        <div class="footer-column">
          <h4>联系我们</h4>
          <ul>
            <li>
              <router-link to="/contact">
                客服热线: 400-888-8888
              </router-link>
            </li>
            <li>
              <router-link to="/contact">
                工作时间: 9:00-18:00
              </router-link>
            </li>
            <li>
              <router-link to="/contact">
                邮箱: support@tailoris.com
              </router-link>
            </li>
            <li class="social-icons">
              <el-icon :size="24">
                <ChatDotRound />
              </el-icon>
              <el-icon :size="24">
                <Share />
              </el-icon>
              <el-icon :size="24">
                <Message />
              </el-icon>
            </li>
          </ul>
        </div>
      </div>
      <div class="footer-bottom">
        <p>© 2024 Tailor IS 裁智云</p>
        <p>ICP备案号: 京ICP备12345678号</p>
      </div>
    </div>
  </footer>
</template>

<script setup lang="ts">
import { ChatDotRound, Share, Message } from '@element-plus/icons-vue';
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
.footer-content {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 40px;
  margin-bottom: 30px;
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
}
.footer-column ul li a:hover {
  color: #fff;
}
.social-icons {
  display: flex;
  gap: 12px;
}
.footer-bottom {
  border-top: 1px solid #333;
  padding-top: 20px;
  text-align: center;
}
.footer-bottom p {
  margin: 4px 0;
  font-size: 12px;
  color: #888;
}
</style>
