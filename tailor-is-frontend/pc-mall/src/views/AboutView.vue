<template>
  <div class="about-page">
    <div class="page-header">
      <h1>关于裁智云</h1>
      <p>服装产业数字化转型的引领者</p>
    </div>

    <div class="container">
      <section class="intro-section">
        <h2>平台简介</h2>
        <p>
          裁智云（Tailor IS）是一个专注于服装全产业链的数字化服务平台，致力于通过 AI 技术、
          区块链存证、大数据分析等前沿科技，为服装设计师、版师、制造商、品牌商提供从设计、
          打版、生产到销售的全流程数字化解决方案。
        </p>
        <p>
          我们整合了纸样设计、服装设计、面料供应链、订单交易、版权保护等核心功能，
          帮助服装企业实现降本增效，推动传统服装产业向智能化、数字化方向转型升级。
        </p>
      </section>

      <section class="mission-section">
        <h2>使命愿景</h2>
        <div class="mission-cards">
          <div class="mission-card">
            <div class="icon">
              🎯
            </div>
            <h3>使命</h3>
            <p>用科技赋能服装产业，让设计更智能，让生产更高效</p>
          </div>
          <div class="mission-card">
            <div class="icon">
              👁️
            </div>
            <h3>愿景</h3>
            <p>成为全球领先的服装产业数字化服务平台</p>
          </div>
          <div class="mission-card">
            <div class="icon">
              💎
            </div>
            <h3>价值观</h3>
            <p>创新驱动、品质至上、客户为先、合作共赢</p>
          </div>
        </div>
      </section>

      <section class="features-section">
        <h2>核心优势</h2>
        <div class="feature-list">
          <div class="feature-item">
            <h3>AI 智能设计</h3>
            <p>基于深度学习的 AI 纸样生成与优化系统，大幅提升设计效率</p>
          </div>
          <div class="feature-item">
            <h3>区块链版权保护</h3>
            <p>设计作品上链存证，确保知识产权安全，防止侵权抄袭</p>
          </div>
          <div class="feature-item">
            <h3>全链路数字化</h3>
            <p>从设计到生产的全流程数字化管理，数据实时同步</p>
          </div>
          <div class="feature-item">
            <h3>全球化服务</h3>
            <p>支持多语言、多币种，服务全球服装产业客户</p>
          </div>
        </div>
      </section>

      <section class="stats-section">
        <h2>平台数据</h2>
        <div class="stats-grid">
          <div class="stat-item">
            <div class="stat-number">
              10,000+
            </div>
            <div class="stat-label">
              注册用户
            </div>
          </div>
          <div class="stat-item">
            <div class="stat-number">
              50,000+
            </div>
            <div class="stat-label">
              纸样设计
            </div>
          </div>
          <div class="stat-item">
            <div class="stat-number">
              1,000+
            </div>
            <div class="stat-label">
              合作企业
            </div>
          </div>
          <div class="stat-item">
            <div class="stat-number">
              30+
            </div>
            <div class="stat-label">
              服务国家
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
// 关于裁智云页面
</script>

<style scoped>
.about-page {
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
  margin-bottom: 24px;
  color: #1a1a2e;
  text-align: center;
}

.intro-section p {
  font-size: 16px;
  line-height: 1.8;
  color: #666;
  margin-bottom: 16px;
  text-align: justify;
}

.mission-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 24px;
}

.mission-card {
  background: white;
  padding: 32px;
  border-radius: 12px;
  text-align: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transition: transform 0.3s;
}

.mission-card:hover {
  transform: translateY(-4px);
}

.mission-card .icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.mission-card h3 {
  font-size: 20px;
  margin-bottom: 12px;
  color: #1a1a2e;
}

.mission-card p {
  color: #666;
  line-height: 1.6;
}

.feature-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 24px;
}

.feature-item {
  background: white;
  padding: 24px;
  border-radius: 12px;
  border-left: 4px solid #667eea;
}

.feature-item h3 {
  font-size: 18px;
  margin-bottom: 12px;
  color: #1a1a2e;
}

.feature-item p {
  color: #666;
  line-height: 1.6;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 24px;
  background: white;
  padding: 40px;
  border-radius: 12px;
}

.stat-item {
  text-align: center;
}

.stat-number {
  font-size: 48px;
  font-weight: bold;
  color: #667eea;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 16px;
  color: #666;
}
</style>
