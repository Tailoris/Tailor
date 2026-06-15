<template>
  <footer class="app-footer">
    <div class="footer-container">
      <div class="footer-content">
        <div class="footer-column">
          <h4>品牌介绍</h4>
          <ul>
            <li><a href="#">关于裁智云</a></li>
            <li><a href="#">品牌故事</a></li>
            <li><a href="#">加入我们</a></li>
            <li><a href="#">新闻动态</a></li>
          </ul>
        </div>
        <div class="footer-column">
          <h4>帮助中心</h4>
          <ul>
            <li><a href="#">购物指南</a></li>
            <li><a href="#">支付方式</a></li>
            <li><a href="#">配送说明</a></li>
            <li><a href="#">售后服务</a></li>
          </ul>
        </div>
        <div class="footer-column">
          <h4>商家服务</h4>
          <ul>
            <li><a href="#">商家入驻</a></li>
            <li><a href="#">商家规则</a></li>
            <li><a href="#">商家后台</a></li>
            <li><a href="#">商家培训</a></li>
          </ul>
        </div>
        <div class="footer-column">
          <h4>联系我们</h4>
          <ul>
            <li><a href="#">客服热线: 400-888-8888</a></li>
            <li><a href="#">工作时间: 9:00-18:00</a></li>
            <li><a href="#">邮箱: support@tailoris.com</a></li>
            <li class="social-icons">
              <el-icon :size="24"><ChatDotRound /></el-icon>
              <el-icon :size="24"><Share /></el-icon>
              <el-icon :size="24"><Message /></el-icon>
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
import { ChatDotRound, Share, Message } from '@element-plus/icons-vue'
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
