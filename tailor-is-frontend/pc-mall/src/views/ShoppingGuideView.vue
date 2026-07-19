<template>
  <div class="info-page">
    <div class="page-header">
      <h1>购物指南</h1>
      <p>帮助您快速了解购物流程</p>
    </div>
    <div class="container">
      <section class="guide-section">
        <h2>购物流程</h2>
        <div class="flow-steps">
          <div class="step-item">
            <div class="step-number">
              1
            </div>
            <div class="step-content">
              <h3>注册登录</h3>
              <p>注册账号并完善个人信息，享受更多会员服务</p>
            </div>
          </div>
          <div class="step-item">
            <div class="step-number">
              2
            </div>
            <div class="step-content">
              <h3>浏览选购</h3>
              <p>浏览纸样、面料、成衣等商品，选择心仪商品加入购物车</p>
            </div>
          </div>
          <div class="step-item">
            <div class="step-number">
              3
            </div>
            <div class="step-content">
              <h3>确认订单</h3>
              <p>核对商品信息、收货地址、配送方式，确认订单详情</p>
            </div>
          </div>
          <div class="step-item">
            <div class="step-number">
              4
            </div>
            <div class="step-content">
              <h3>支付订单</h3>
              <p>支持支付宝、微信、银行卡等多种支付方式</p>
            </div>
          </div>
          <div class="step-item">
            <div class="step-number">
              5
            </div>
            <div class="step-content">
              <h3>收货评价</h3>
              <p>确认收货后，对商品进行评价，帮助其他买家</p>
            </div>
          </div>
        </div>
      </section>

      <section class="tips-section">
        <h2>购物小贴士</h2>
        <div class="tips-grid">
          <div class="tip-card">
            <h3>如何选择合适的纸样？</h3>
            <p>根据您的身材尺寸、款式偏好选择对应尺码和风格的纸样，可查看商品详情中的尺寸表</p>
          </div>
          <div class="tip-card">
            <h3>纸样定制服务</h3>
            <p>提供个性化纸样定制服务，根据您的需求量身定制，详情可咨询客服</p>
          </div>
          <div class="tip-card">
            <h3>版权保护说明</h3>
            <p>平台所有纸样均受版权保护，购买后仅限个人使用，禁止二次传播</p>
          </div>
          <div class="tip-card">
            <h3>优惠券使用</h3>
            <p>下单前可领取优惠券，结算时自动抵扣，部分商品不支持叠加使用</p>
          </div>
        </div>
      </section>

      <section class="faq-section">
        <h2>常见问题</h2>
        <div class="faq-list">
          <div class="faq-item">
            <h3>Q: 购买纸样后可以修改吗？</h3>
            <p>A: 购买的纸样为电子版，您可以下载后根据需要进行调整，但请勿二次传播</p>
          </div>
          <div class="faq-item">
            <h3>Q: 如何申请发票？</h3>
            <p>A: 订单完成后可在"我的订单"中申请电子发票，填写开票信息后 1-3 个工作日内开具</p>
          </div>
          <div class="faq-item">
            <h3>Q: 支持哪些支付方式？</h3>
            <p>A: 目前支持支付宝、微信支付、银联卡等主流支付方式</p>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
// 购物指南页面
</script>

<style scoped>
.info-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.page-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  text-align: center;
  padding: 60px 20px;
}

.page-header h1 {
  font-size: 42px;
  margin-bottom: 12px;
}

.page-header p {
  font-size: 18px;
  opacity: 0.9;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 50px 20px;
}

section {
  margin-bottom: 50px;
}

section h2 {
  font-size: 28px;
  margin-bottom: 30px;
  color: #1a1a2e;
  text-align: center;
}

.flow-steps {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.step-item {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
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
  flex-shrink: 0;
}

.step-content h3 {
  font-size: 20px;
  margin-bottom: 8px;
  color: #1a1a2e;
}

.step-content p {
  color: #666;
  line-height: 1.6;
}

.tips-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
}

.tip-card {
  background: white;
  padding: 24px;
  border-radius: 12px;
  border-left: 4px solid #667eea;
}

.tip-card h3 {
  font-size: 18px;
  margin-bottom: 12px;
  color: #1a1a2e;
}

.tip-card p {
  color: #666;
  line-height: 1.6;
}

.faq-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.faq-item {
  background: white;
  padding: 20px 24px;
  border-radius: 10px;
}

.faq-item h3 {
  font-size: 16px;
  margin-bottom: 8px;
  color: #667eea;
}

.faq-item p {
  color: #666;
  line-height: 1.6;
}
</style>
