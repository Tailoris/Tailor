<template>
  <div class="merchant-backend-page">
    <div class="page-header">
      <h1>商家后台</h1>
      <p>一站式店铺经营管理平台</p>
    </div>
    <div class="container">
      <section class="intro-section">
        <h2>后台功能概览</h2>
        <p class="intro-text">
          商家后台是裁智云为入驻商家打造的专属经营管理系统，集成订单管理、商品管理、
          数据分析、客户运营等核心功能，帮助商家高效运营店铺。
        </p>
      </section>

      <section class="features-section">
        <h2>核心模块</h2>
        <div class="feature-grid">
          <div class="feature-card">
            <div class="icon">
              📊
            </div>
            <h3>数据看板</h3>
            <p>实时销售数据、订单趋势、客户画像、经营指标一目了然</p>
          </div>
          <div class="feature-card">
            <div class="icon">
              🛍️
            </div>
            <h3>商品管理</h3>
            <p>商品发布、库存管理、价格调整、批量操作，支持多规格 SKU</p>
          </div>
          <div class="feature-card">
            <div class="icon">
              📦
            </div>
            <h3>订单管理</h3>
            <p>订单处理、发货跟踪、售后处理、退款管理全流程</p>
          </div>
          <div class="feature-card">
            <div class="icon">
              💬
            </div>
            <h3>客户运营</h3>
            <p>客户分组、会员管理、营销触达、复购提升</p>
          </div>
          <div class="feature-card">
            <div class="icon">
              💰
            </div>
            <h3>财务管理</h3>
            <p>收入明细、提现管理、发票开具、对账报表</p>
          </div>
          <div class="feature-card">
            <div class="icon">
              📢
            </div>
            <h3>营销中心</h3>
            <p>优惠券、满减活动、限时折扣、推广投放</p>
          </div>
        </div>
      </section>

      <section class="access-section">
        <h2>如何进入商家后台</h2>
        <div class="access-steps">
          <div class="step">
            <div class="step-number">
              1
            </div>
            <h3>完成入驻</h3>
            <p>提交入驻申请并通过审核</p>
          </div>
          <div class="step">
            <div class="step-number">
              2
            </div>
            <h3>登录账号</h3>
            <p>使用商家账号登录平台</p>
          </div>
          <div class="step">
            <div class="step-number">
              3
            </div>
            <h3>进入后台</h3>
            <p>点击顶部导航"商家中心"进入</p>
          </div>
        </div>
        <div class="cta-section">
          <p>还未入驻？立即申请成为裁智云商家</p>
          <router-link
            to="/merchant-apply"
            class="cta-button"
          >
            立即申请入驻
          </router-link>
        </div>
      </section>

      <section class="support-section">
        <h2>商家支持</h2>
        <div class="support-cards">
          <div class="support-card">
            <h3>新手教程</h3>
            <p>详细的操作指南，快速上手后台功能</p>
          </div>
          <div class="support-card">
            <h3>视频教程</h3>
            <p>视频演示核心功能操作流程</p>
          </div>
          <div class="support-card">
            <h3>专属客服</h3>
            <p>商家专属客服通道，优先响应</p>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
// 商家后台页面
</script>

<style scoped>
.merchant-backend-page { min-height: 100vh; background: #f5f7fa; }
.page-header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; text-align: center; padding: 80px 20px; }
.page-header h1 { font-size: 48px; margin-bottom: 16px; }
.page-header p { font-size: 20px; opacity: 0.9; }
.container { max-width: 1200px; margin: 0 auto; padding: 60px 20px; }
section { margin-bottom: 60px; }
section h2 { font-size: 32px; margin-bottom: 24px; color: #1a1a2e; text-align: center; }
.intro-text { text-align: center; color: #666; line-height: 1.8; max-width: 800px; margin: 0 auto; font-size: 16px; }
.feature-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 24px; }
.feature-card { background: white; padding: 32px; border-radius: 12px; text-align: center; box-shadow: 0 2px 12px rgba(0,0,0,0.08); transition: transform 0.3s; }
.feature-card:hover { transform: translateY(-4px); }
.feature-card .icon { font-size: 48px; margin-bottom: 16px; }
.feature-card h3 { font-size: 20px; margin-bottom: 12px; color: #1a1a2e; }
.feature-card p { color: #666; line-height: 1.6; }
.access-steps { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 24px; margin-bottom: 40px; }
.step { background: white; padding: 24px; border-radius: 12px; text-align: center; }
.step-number { width: 48px; height: 48px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 24px; font-weight: bold; margin: 0 auto 16px; }
.step h3 { font-size: 18px; margin-bottom: 8px; color: #1a1a2e; }
.step p { color: #666; }
.cta-section { text-align: center; background: white; padding: 40px; border-radius: 12px; box-shadow: 0 2px 12px rgba(0,0,0,0.08); }
.cta-section p { font-size: 18px; color: #1a1a2e; margin-bottom: 20px; }
.cta-button { display: inline-block; padding: 12px 32px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; text-decoration: none; border-radius: 24px; font-weight: 600; transition: transform 0.3s; }
.cta-button:hover { transform: scale(1.05); }
.support-cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 24px; }
.support-card { background: white; padding: 24px; border-radius: 12px; border-left: 4px solid #667eea; }
.support-card h3 { font-size: 18px; margin-bottom: 12px; color: #1a1a2e; }
.support-card p { color: #666; line-height: 1.6; }
</style>
