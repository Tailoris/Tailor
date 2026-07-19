<template>
  <div class="aftersale-page">
    <div class="page-header">
      <h1>售后服务</h1>
      <p>专业、高效、贴心的售后保障</p>
    </div>

    <div class="container">
      <section class="service-policy">
        <h2>售后政策</h2>
        <div class="policy-cards">
          <div class="policy-card">
            <div class="icon">
              🔄
            </div>
            <h3>7 天无理由退换</h3>
            <p>收到商品 7 天内，未使用商品可申请无理由退换货</p>
          </div>
          <div class="policy-card">
            <div class="icon">
              🛠️
            </div>
            <h3>15 天技术支持</h3>
            <p>纸样定制、设计服务提供 15 天免费技术支持与修改</p>
          </div>
          <div class="policy-card">
            <div class="icon">
              💰
            </div>
            <h3>33-34-33 分期付款</h3>
            <p>大额订单支持分期付款，首付 33%、中期 34%、尾款 33%</p>
          </div>
        </div>
      </section>

      <section class="service-process">
        <h2>售后流程</h2>
        <div class="process-steps">
          <div class="step">
            <div class="step-number">
              1
            </div>
            <h3>提交申请</h3>
            <p>在"我的订单"页面点击"申请售后"，填写售后原因</p>
          </div>
          <div class="step">
            <div class="step-number">
              2
            </div>
            <h3>客服审核</h3>
            <p>客服 24 小时内审核售后申请，与您沟通确认</p>
          </div>
          <div class="step">
            <div class="step-number">
              3
            </div>
            <h3>处理执行</h3>
            <p>根据售后类型进行退换货、退款或技术修改</p>
          </div>
          <div class="step">
            <div class="step-number">
              4
            </div>
            <h3>完成确认</h3>
            <p>售后处理完成，确认 satisfaction 并评价</p>
          </div>
        </div>
      </section>

      <section class="service-types">
        <h2>售后类型</h2>
        <div class="type-list">
          <div class="type-item">
            <h3>退货退款</h3>
            <ul>
              <li>商品存在质量问题</li>
              <li>与描述严重不符</li>
              <li>7 天内未使用商品</li>
              <li>退款原路返回，3-5 个工作日到账</li>
            </ul>
          </div>
          <div class="type-item">
            <h3>换货服务</h3>
            <ul>
              <li>商品尺码不合适</li>
              <li>颜色与预期不符</li>
              <li>存在轻微瑕疵</li>
              <li>免费换货，运费由平台承担</li>
            </ul>
          </div>
          <div class="type-item">
            <h3>技术修改</h3>
            <ul>
              <li>纸样尺寸需要调整</li>
              <li>设计细节需要优化</li>
              <li>工艺说明需要补充</li>
              <li>15 天内免费修改 2 次</li>
            </ul>
          </div>
        </div>
      </section>

      <section class="contact-section">
        <h2>联系售后</h2>
        <div class="contact-info">
          <p><strong>客服热线：</strong>400-888-8888（工作时间：9:00-18:00）</p>
          <p><strong>在线客服：</strong>点击页面右下角在线客服图标</p>
          <p><strong>邮箱支持：</strong>aftersale@tailoris.com</p>
          <p><strong>工单系统：</strong>在"个人中心 - 我的工单"提交售后工单</p>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
// 售后服务页面
</script>

<style scoped>
.aftersale-page {
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

.policy-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 24px;
}

.policy-card {
  background: white;
  padding: 32px;
  border-radius: 12px;
  text-align: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transition: transform 0.3s;
}

.policy-card:hover {
  transform: translateY(-4px);
}

.policy-card .icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.policy-card h3 {
  font-size: 20px;
  margin-bottom: 12px;
  color: #1a1a2e;
}

.policy-card p {
  color: #666;
  line-height: 1.6;
}

.process-steps {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 24px;
}

.step {
  background: white;
  padding: 24px;
  border-radius: 12px;
  text-align: center;
}

.step-number {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: bold;
  margin: 0 auto 16px;
}

.step h3 {
  font-size: 18px;
  margin-bottom: 8px;
  color: #1a1a2e;
}

.step p {
  color: #666;
  line-height: 1.6;
}

.type-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 24px;
}

.type-item {
  background: white;
  padding: 32px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.type-item h3 {
  font-size: 20px;
  margin-bottom: 16px;
  color: #1a1a2e;
}

.type-item ul {
  list-style: none;
  padding: 0;
}

.type-item ul li {
  padding: 8px 0;
  color: #666;
  line-height: 1.6;
  border-bottom: 1px solid #f0f0f0;
}

.type-item ul li:last-child {
  border-bottom: none;
}

.contact-info {
  background: white;
  padding: 32px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.contact-info p {
  margin-bottom: 12px;
  color: #666;
  line-height: 1.8;
}

.contact-info strong {
  color: #1a1a2e;
}
</style>
