<template>
  <div class="join-us-page">
    <div class="page-header">
      <h1>加入我们</h1>
      <p>与优秀的人一起做有挑战的事</p>
    </div>
    <div class="container">
      <section class="culture-section">
        <h2>企业文化</h2>
        <div class="culture-cards">
          <div class="culture-card">
            <h3>开放包容</h3>
            <p>鼓励创新思维，尊重多元文化，营造开放的工作氛围</p>
          </div>
          <div class="culture-card">
            <h3>快速成长</h3>
            <p>完善的培训体系，清晰的晋升通道，助你职业发展加速</p>
          </div>
          <div class="culture-card">
            <h3>技术驱动</h3>
            <p>拥抱前沿技术，追求工程卓越，用技术解决产业痛点</p>
          </div>
        </div>
      </section>

      <section class="benefits-section">
        <h2>员工福利</h2>
        <div class="benefits-grid">
          <div class="benefit-item">
            <span class="benefit-icon">💰</span>
            <span>有竞争力的薪酬</span>
          </div>
          <div class="benefit-item">
            <span class="benefit-icon">🏥</span>
            <span>五险一金 + 商业保险</span>
          </div>
          <div class="benefit-item">
            <span class="benefit-icon">🏖️</span>
            <span>弹性工作制 + 带薪年假</span>
          </div>
          <div class="benefit-item">
            <span class="benefit-icon">📚</span>
            <span>技术培训 + 学习基金</span>
          </div>
          <div class="benefit-item">
            <span class="benefit-icon">🍽️</span>
            <span>免费午餐 + 下午茶</span>
          </div>
          <div class="benefit-item">
            <span class="benefit-icon">🎮</span>
            <span>团建活动 + 年度旅游</span>
          </div>
        </div>
      </section>

      <section class="positions-section">
        <h2>热招职位</h2>
        <div class="position-list">
          <div class="position-card">
            <div class="position-info">
              <h3>高级 Java 工程师</h3>
              <p>负责微服务架构设计与核心业务开发</p>
            </div>
            <div class="position-tags">
              <span class="tag">全职</span>
              <span class="tag">3-5年</span>
              <span class="tag">北京</span>
            </div>
          </div>
          <div class="position-card">
            <div class="position-info">
              <h3>前端开发工程师</h3>
              <p>负责 PC 端与移动端前端开发</p>
            </div>
            <div class="position-tags">
              <span class="tag">全职</span>
              <span class="tag">2-3年</span>
              <span class="tag">北京</span>
            </div>
          </div>
          <div class="position-card">
            <div class="position-info">
              <h3>AI 算法工程师</h3>
              <p>负责纸样生成、侵权检测等 AI 模型研发</p>
            </div>
            <div class="position-tags">
              <span class="tag">全职</span>
              <span class="tag">3-5年</span>
              <span class="tag">北京</span>
            </div>
          </div>
          <div class="position-card">
            <div class="position-info">
              <h3>产品经理</h3>
              <p>负责服装产业 SaaS 产品规划与设计</p>
            </div>
            <div class="position-tags">
              <span class="tag">全职</span>
              <span class="tag">3-5年</span>
              <span class="tag">北京</span>
            </div>
          </div>
        </div>
      </section>

      <section class="apply-section">
        <h2>投递简历</h2>
        <p>请将简历发送至：</p>
        <a
          href="mailto:hr@tailoris.com"
          class="email-link"
        >hr@tailoris.com</a>
        <p class="apply-note">
          邮件标题格式：应聘职位 - 姓名 - 工作年限
        </p>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
// 加入我们页面
</script>

<style scoped>
.join-us-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.page-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  text-align: center;
  padding: 80px 20px;
}

.page-header h1 {
  font-size: 48px;
  margin-bottom: 16px;
}

.page-header p {
  font-size: 20px;
  opacity: 0.9;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 60px 20px;
}

section {
  margin-bottom: 60px;
}

section h2 {
  font-size: 32px;
  margin-bottom: 32px;
  color: #1a1a2e;
  text-align: center;
}

.culture-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 24px;
}

.culture-card {
  background: white;
  padding: 32px;
  border-radius: 12px;
  text-align: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.culture-card h3 {
  font-size: 20px;
  margin-bottom: 12px;
  color: #667eea;
}

.culture-card p {
  color: #666;
  line-height: 1.6;
}

.benefits-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
}

.benefit-item {
  background: white;
  padding: 20px 24px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 16px;
  color: #333;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.benefit-icon {
  font-size: 28px;
}

.position-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.position-card {
  background: white;
  padding: 24px;
  border-radius: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: transform 0.2s;
}

.position-card:hover {
  transform: translateX(4px);
}

.position-info h3 {
  font-size: 18px;
  margin-bottom: 6px;
  color: #1a1a2e;
}

.position-info p {
  color: #666;
  font-size: 14px;
}

.position-tags {
  display: flex;
  gap: 8px;
}

.tag {
  background: #eef2ff;
  color: #667eea;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 13px;
  white-space: nowrap;
}

.apply-section {
  text-align: center;
}

.apply-section p {
  color: #666;
  font-size: 16px;
  margin-bottom: 12px;
}

.email-link {
  display: inline-block;
  font-size: 24px;
  color: #667eea;
  font-weight: bold;
  margin-bottom: 12px;
}

.apply-note {
  font-size: 14px;
  color: #999;
}
</style>
