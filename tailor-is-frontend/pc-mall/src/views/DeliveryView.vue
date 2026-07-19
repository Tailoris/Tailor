<template>
  <div class="delivery-page">
    <div class="page-header">
      <h1>配送说明</h1>
      <p>快速、安全、准时的配送服务</p>
    </div>

    <div class="container">
      <section class="delivery-methods">
        <h2>配送方式</h2>
        <div class="method-cards">
          <div class="method-card">
            <div class="icon">
              🚚
            </div>
            <h3>标准配送</h3>
            <p class="time">
              3-5 个工作日
            </p>
            <p class="price">
              订单满 299 元免运费
            </p>
            <p class="desc">
              适用于普通订单，覆盖全国大部分地区
            </p>
          </div>
          <div class="method-card">
            <div class="icon">
              ⚡
            </div>
            <h3>加急配送</h3>
            <p class="time">
              1-2 个工作日
            </p>
            <p class="price">
              运费 30 元
            </p>
            <p class="desc">
              适用于紧急订单，优先处理发货
            </p>
          </div>
          <div class="method-card">
            <div class="icon">
              📦
            </div>
            <h3>电子交付</h3>
            <p class="time">
              即时交付
            </p>
            <p class="price">
              免费
            </p>
            <p class="desc">
              纸样文件、设计图纸等数字化产品即时下载
            </p>
          </div>
        </div>
      </section>

      <section class="delivery-process">
        <h2>配送流程</h2>
        <div class="process-steps">
          <div class="step">
            <div class="step-number">
              1
            </div>
            <h3>订单确认</h3>
            <p>确认订单信息并完成支付</p>
          </div>
          <div class="step">
            <div class="step-number">
              2
            </div>
            <h3>备货处理</h3>
            <p>商家备货并打包（1-2 个工作日）</p>
          </div>
          <div class="step">
            <div class="step-number">
              3
            </div>
            <h3>物流配送</h3>
            <p>快递公司揽收并运输</p>
          </div>
          <div class="step">
            <div class="step-number">
              4
            </div>
            <h3>签收确认</h3>
            <p>收到商品并确认签收</p>
          </div>
        </div>
      </section>

      <section class="delivery-areas">
        <h2>配送范围</h2>
        <div class="area-info">
          <p><strong>国内配送：</strong>覆盖全国 31 个省、自治区、直辖市，包括偏远地区</p>
          <p><strong>国际配送：</strong>支持全球 30+ 国家和地区，具体运费和时效请咨询客服</p>
          <p><strong>电子交付：</strong>全球可用，购买后即时下载</p>
        </div>
      </section>

      <section class="faq-section">
        <h2>常见问题</h2>
        <div class="faq-list">
          <div class="faq-item">
            <h3>如何查询物流信息？</h3>
            <p>在"我的订单"页面点击对应订单，即可查看物流跟踪信息和快递公司联系方式。</p>
          </div>
          <div class="faq-item">
            <h3>配送地址可以修改吗？</h3>
            <p>订单未发货前可以修改配送地址，发货后需联系客服协调处理。</p>
          </div>
          <div class="faq-item">
            <h3>收到商品有问题怎么办？</h3>
            <p>请在签收后 7 天内联系客服，提供商品照片，我们将为您办理退换货。</p>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
// 配送说明页面
</script>

<style scoped>
.delivery-page {
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

.method-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
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

.method-card .icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.method-card h3 {
  font-size: 20px;
  margin-bottom: 12px;
  color: #1a1a2e;
}

.method-card .time {
  font-size: 18px;
  color: #667eea;
  font-weight: bold;
  margin-bottom: 8px;
}

.method-card .price {
  font-size: 16px;
  color: #333;
  margin-bottom: 12px;
}

.method-card .desc {
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
  position: relative;
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

.area-info {
  background: white;
  padding: 32px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.area-info p {
  margin-bottom: 12px;
  color: #666;
  line-height: 1.8;
}

.area-info strong {
  color: #1a1a2e;
}

.faq-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.faq-item {
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.faq-item h3 {
  font-size: 18px;
  margin-bottom: 12px;
  color: #1a1a2e;
}

.faq-item p {
  color: #666;
  line-height: 1.6;
}
</style>
