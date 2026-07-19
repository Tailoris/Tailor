<template>
  <div class="info-page">
    <div class="page-header">
      <h1>支付方式</h1>
      <p>多种支付方式，安全便捷</p>
    </div>
    <div class="container">
      <section class="payment-methods">
        <h2>支持的支付方式</h2>
        <div class="method-grid">
          <div class="method-card">
            <div class="method-icon">
              💳
            </div>
            <h3>支付宝</h3>
            <p>支持支付宝扫码、网页支付，实时到账</p>
          </div>
          <div class="method-card">
            <div class="method-icon">
              📱
            </div>
            <h3>微信支付</h3>
            <p>支持微信扫码、H5支付，方便快捷</p>
          </div>
          <div class="method-card">
            <div class="method-icon">
              🏦
            </div>
            <h3>银行卡支付</h3>
            <p>支持主流银行借记卡、信用卡在线支付</p>
          </div>
          <div class="method-card">
            <div class="method-icon">
              💰
            </div>
            <h3>余额支付</h3>
            <p>使用账户余额支付，享受更多优惠</p>
          </div>
        </div>
      </section>

      <section class="security-section">
        <h2>支付安全保障</h2>
        <div class="security-list">
          <div class="security-item">
            <h3>🔒 SSL加密传输</h3>
            <p>所有支付信息采用SSL加密传输，确保数据安全</p>
          </div>
          <div class="security-item">
            <h3>🛡️ 风控系统</h3>
            <p>智能风控系统实时监控，防范支付风险</p>
          </div>
          <div class="security-item">
            <h3>✅ 资金监管</h3>
            <p>资金由第三方银行托管，交易更放心</p>
          </div>
          <div class="security-item">
            <h3>📞 24小时客服</h3>
            <p>支付问题随时咨询，专业客服为您解答</p>
          </div>
        </div>
      </section>

      <section class="faq-section">
        <h2>支付常见问题</h2>
        <div class="faq-list">
          <div class="faq-item">
            <h3>Q: 支付失败怎么办？</h3>
            <p>A: 请检查网络连接、银行卡余额是否充足，或更换支付方式重试。如仍无法解决，请联系客服</p>
          </div>
          <div class="faq-item">
            <h3>Q: 支付后多久到账？</h3>
            <p>A: 支付宝、微信支付实时到账，银行卡支付一般1-3个工作日</p>
          </div>
          <div class="faq-item">
            <h3>Q: 可以申请退款吗？</h3>
            <p>A: 订单未发货前可申请全额退款，已发货需先退货后再申请退款</p>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
// 支付方式页面
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

.method-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 24px;
}

.method-card {
  background: white;
  padding: 32px;
  border-radius: 12px;
  text-align: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transition: transform 0.3s;
}

.method-card:hover {
  transform: translateY(-4px);
}

.method-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.method-card h3 {
  font-size: 20px;
  margin-bottom: 12px;
  color: #1a1a2e;
}

.method-card p {
  color: #666;
  line-height: 1.6;
}

.security-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
}

.security-item {
  background: white;
  padding: 24px;
  border-radius: 12px;
  border-left: 4px solid #667eea;
}

.security-item h3 {
  font-size: 18px;
  margin-bottom: 12px;
  color: #1a1a2e;
}

.security-item p {
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
