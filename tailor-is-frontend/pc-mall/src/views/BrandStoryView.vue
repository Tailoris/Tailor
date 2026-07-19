<template>
  <div class="brand-story-page">
    <div class="page-header">
      <h1>品牌故事</h1>
      <p>从传统到智能，裁智云的蜕变之路</p>
    </div>

    <div class="container">
      <section class="timeline-section">
        <h2>发展历程</h2>
        <div class="timeline">
          <div class="timeline-item">
            <div class="timeline-year">
              2020
            </div>
            <div class="timeline-content">
              <h3>项目启动</h3>
              <p>裁智云项目正式立项，致力于用 AI 技术革新传统服装产业</p>
            </div>
          </div>
          <div class="timeline-item">
            <div class="timeline-year">
              2021
            </div>
            <div class="timeline-content">
              <h3>平台上线</h3>
              <p>裁智云 1.0 版本正式上线，提供基础纸样设计功能</p>
            </div>
          </div>
          <div class="timeline-item">
            <div class="timeline-year">
              2022
            </div>
            <div class="timeline-content">
              <h3>AI 赋能</h3>
              <p>引入深度学习算法，实现 AI 智能纸样生成与优化</p>
            </div>
          </div>
          <div class="timeline-item">
            <div class="timeline-year">
              2023
            </div>
            <div class="timeline-content">
              <h3>生态构建</h3>
              <p>整合供应链、版权保护、在线教育等功能，构建完整产业生态</p>
            </div>
          </div>
          <div class="timeline-item">
            <div class="timeline-year">
              2024
            </div>
            <div class="timeline-content">
              <h3>全球化布局</h3>
              <p>支持多语言、多币种，服务全球服装产业客户</p>
            </div>
          </div>
        </div>
      </section>

      <section class="story-section">
        <h2>我们的故事</h2>
        <div class="story-content">
          <p>
            裁智云的诞生源于对传统服装产业痛点的深刻洞察。在数字化浪潮席卷全球的今天，
            服装产业仍然面临着设计效率低、版权保护难、供应链协同差等问题。
          </p>
          <p>
            我们的创始团队由资深服装行业专家、AI 技术专家和产品经理组成，
            他们深知传统服装人的困境，决心用科技的力量改变这一切。
          </p>
          <p>
            从第一行代码到服务万家企业，裁智云始终坚持"让设计更智能，让生产更高效"的使命，
            不断迭代创新，为全球服装产业提供数字化转型的完整解决方案。
          </p>
        </div>
      </section>

      <section class="values-section">
        <h2>品牌价值</h2>
        <div class="values-grid">
          <div class="value-card">
            <div class="value-icon">
              创新
            </div>
            <h3>技术创新</h3>
            <p>持续投入 AI、区块链等前沿技术研发，保持行业领先地位</p>
          </div>
          <div class="value-card">
            <div class="value-icon">
              品质
            </div>
            <h3>品质至上</h3>
            <p>严格把控产品质量，为每一位用户提供可靠的服务</p>
          </div>
          <div class="value-card">
            <div class="value-icon">
              共赢
            </div>
            <h3>合作共赢</h3>
            <p>与产业链各方携手共进，构建健康可持续的产业生态</p>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
// 品牌故事页面
</script>

<style scoped>
.brand-story-page {
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
  margin-bottom: 40px;
  color: #1a1a2e;
  text-align: center;
}

.timeline {
  position: relative;
  padding-left: 40px;
}

.timeline::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 2px;
  background: #667eea;
}

.timeline-item {
  position: relative;
  margin-bottom: 40px;
  padding-left: 40px;
}

.timeline-item::before {
  content: '';
  position: absolute;
  left: -8px;
  top: 8px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #667eea;
  border: 3px solid white;
  box-shadow: 0 0 0 2px #667eea;
}

.timeline-year {
  font-size: 24px;
  font-weight: bold;
  color: #667eea;
  margin-bottom: 8px;
}

.timeline-content h3 {
  font-size: 20px;
  margin-bottom: 8px;
  color: #1a1a2e;
}

.timeline-content p {
  color: #666;
  line-height: 1.6;
}

.story-content p {
  font-size: 16px;
  line-height: 1.8;
  color: #666;
  margin-bottom: 20px;
  text-align: justify;
}

.values-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 24px;
}

.value-card {
  background: white;
  padding: 32px;
  border-radius: 12px;
  text-align: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transition: transform 0.3s;
}

.value-card:hover {
  transform: translateY(-4px);
}

.value-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: bold;
}

.value-card h3 {
  font-size: 20px;
  margin-bottom: 12px;
  color: #1a1a2e;
}

.value-card p {
  color: #666;
  line-height: 1.6;
}
</style>
