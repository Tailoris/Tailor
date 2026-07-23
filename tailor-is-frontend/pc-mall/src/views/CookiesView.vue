<template>
  <div class="cookies-view" role="main" aria-label="Cookie政策">
    <div class="view-header">
      <h1 class="page-title">Cookie政策</h1>
      <p class="page-subtitle">了解我们如何使用Cookie</p>
    </div>

    <div class="content-container">
      <div class="content-section">
        <h2>一、什么是Cookie</h2>
        <p>
          Cookie是存储在您设备上的小型文本文件，用于识别您的设备和记录您的偏好设置。
          当您访问我们的网站时，我们会在您的浏览器中设置Cookie。
        </p>
      </div>

      <div class="content-section">
        <h2>二、我们使用的Cookie类型</h2>
        <h3>2.1 必要Cookie</h3>
        <p>
          必要Cookie对于网站的基本功能是必需的，无法被禁用。它们包括：
        </p>
        <ul>
          <li><strong>会话Cookie：</strong>维持您的登录状态。</li>
          <li><strong>认证Cookie：</strong>验证您的身份。</li>
          <li><strong>安全Cookie：</strong>保护您的账户安全。</li>
        </ul>

        <h3>2.2 功能Cookie</h3>
        <p>
          功能Cookie用于记住您的偏好设置，提升用户体验：
        </p>
        <ul>
          <li><strong>语言偏好：</strong>记住您选择的语言。</li>
          <li><strong>主题偏好：</strong>记住您选择的界面主题。</li>
          <li><strong>购物车：</strong>保存您的购物车内容。</li>
        </ul>

        <h3>2.3 分析Cookie</h3>
        <p>
          分析Cookie用于收集网站使用数据，帮助我们改进服务：
        </p>
        <ul>
          <li><strong>访问统计：</strong>记录页面访问量和访问来源。</li>
          <li><strong>行为分析：</strong>分析用户浏览和点击行为。</li>
          <li><strong>性能监控：</strong>监控网站性能和加载速度。</li>
        </ul>

        <h3>2.4 广告Cookie</h3>
        <p>
          广告Cookie用于提供个性化广告：
        </p>
        <ul>
          <li><strong>兴趣定向：</strong>根据您的浏览历史展示相关广告。</li>
          <li><strong>转化追踪：</strong>追踪广告效果和转化。</li>
        </ul>
      </div>

      <div class="content-section">
        <h2>三、如何管理Cookie</h2>
        <h3>3.1 浏览器设置</h3>
        <p>
          您可以通过浏览器设置管理Cookie：
        </p>
        <ul>
          <li><strong>Chrome：</strong>设置 > 隐私和安全 > Cookie和其他网站数据</li>
          <li><strong>Firefox：</strong>设置 > 隐私与安全 > Cookie</li>
          <li><strong>Safari：</strong>设置 > 隐私 > Cookie和网站数据</li>
          <li><strong>Edge：</strong>设置 > 隐私、搜索和服务 > Cookie和站点权限</li>
        </ul>

        <h3>3.2 拒绝Cookie的影响</h3>
        <p>
          拒绝某些Cookie可能会影响您的用户体验：
        </p>
        <ul>
          <li>无法登录账户。</li>
          <li>购物车内容不会保存。</li>
          <li>无法记住偏好设置。</li>
          <li>某些功能可能无法正常工作。</li>
        </ul>
      </div>

      <div class="content-section">
        <h2>四、第三方Cookie</h2>
        <p>
          我们的网站可能包含第三方服务，这些服务也可能设置Cookie：
        </p>
        <ul>
          <li><strong>社交媒体插件：</strong>如分享按钮。</li>
          <li><strong>分析工具：</strong>如Google Analytics。</li>
          <li><strong>支付网关：</strong>如支付宝、微信支付。</li>
        </ul>
        <p>
          我们无法控制第三方Cookie的使用，建议查看他们的隐私政策。
        </p>
      </div>

      <div class="content-section">
        <h2>五、Cookie的存储期限</h2>
        <p>不同类型的Cookie存储期限不同：</p>
        <ul>
          <li><strong>会话Cookie：</strong>关闭浏览器后自动删除。</li>
          <li><strong>持久Cookie：</strong>存储期限从几天到几年不等。</li>
        </ul>
      </div>

      <div class="content-section">
        <h2>六、政策更新</h2>
        <p>我们可能定期更新本政策，重大变更将通过通知告知您。</p>
        <p>最后更新：2024年1月15日</p>
      </div>

      <div class="content-section">
        <h2>七、联系我们</h2>
        <p>如果您对本政策有任何疑问，请通过以下方式联系我们：</p>
        <ul>
          <li>邮箱：support[at]tailor-is.com</li>
          <li>客服热线：400-888-8888</li>
          <li>在线客服：工作日 9:00-18:00</li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// Cookie政策页面
</script>

<style scoped>
.cookies-view {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px 20px;
}

.view-header {
  text-align: center;
  margin-bottom: 32px;
}

.page-title {
  font-size: 32px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 8px 0;
}

.page-subtitle {
  font-size: 16px;
  color: #64748b;
  margin: 0;
}

.content-container {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.content-section {
  margin-bottom: 32px;
}

.content-section:last-child {
  margin-bottom: 0;
}

.content-section h2 {
  font-size: 20px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 16px 0;
  padding-bottom: 8px;
  border-bottom: 2px solid #1d39c4;
}

.content-section h3 {
  font-size: 16px;
  font-weight: 600;
  color: #334155;
  margin: 16px 0 12px 0;
}

.content-section p {
  font-size: 14px;
  color: #475569;
  line-height: 1.8;
  margin: 0 0 12px 0;
}

.content-section ul {
  margin: 0 0 12px 0;
  padding-left: 24px;
}

.content-section li {
  font-size: 14px;
  color: #475569;
  line-height: 1.8;
  margin-bottom: 8px;
}

.content-section li:last-child {
  margin-bottom: 0;
}

@media (max-width: 768px) {
  .cookies-view {
    padding: 16px 12px;
  }

  .page-title {
    font-size: 24px;
  }

  .content-container {
    padding: 16px;
  }
}
</style>
