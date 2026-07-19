<template>
  <div class="merchant-rules-page">
    <div class="page-header">
      <h1>商家规则</h1>
      <p>规范经营，共建良好生态</p>
    </div>
    <div class="container">
      <section class="rules-section">
        <h2>入驻资质</h2>
        <div class="rule-cards">
          <div class="rule-card">
            <div class="icon">
              📋
            </div>
            <h3>企业资质</h3>
            <ul>
              <li>合法注册的企业营业执照</li>
              <li>服装行业相关经营许可</li>
              <li>品牌授权或自有品牌证明</li>
              <li>税务登记证明</li>
            </ul>
          </div>
          <div class="rule-card">
            <div class="icon">
              👤
            </div>
            <h3>个人设计师</h3>
            <ul>
              <li>有效身份证明</li>
              <li>3 年以上设计经验证明</li>
              <li>作品集审核通过</li>
              <li>质保金缴纳</li>
            </ul>
          </div>
          <div class="rule-card">
            <div class="icon">
              🏭
            </div>
            <h3>制造商/版师</h3>
            <ul>
              <li>生产资质证明</li>
              <li>设备清单及产能证明</li>
              <li>质控体系文件</li>
              <li>过往合作案例</li>
            </ul>
          </div>
        </div>
      </section>

      <section class="conduct-section">
        <h2>经营规范</h2>
        <div class="conduct-list">
          <div class="conduct-item">
            <h3>商品发布规范</h3>
            <p>商品信息必须真实准确，图片与实物一致，价格明码标价，不得虚假宣传或误导消费者。</p>
          </div>
          <div class="conduct-item">
            <h3>知识产权保护</h3>
            <p>严禁销售侵权商品，上传作品需确保原创性或已获得授权，平台接入区块链版权存证。</p>
          </div>
          <div class="conduct-item">
            <h3>售后服务标准</h3>
            <p>遵守 7 天无理由退换、15 天技术支持政策，及时响应客户售后需求，维护良好口碑。</p>
          </div>
          <div class="conduct-item">
            <h3>交易诚信</h3>
            <p>禁止刷单、虚假交易、恶意竞争等行为，违者将受到警告、降权、清退等处罚。</p>
          </div>
        </div>
      </section>

      <section class="fee-section">
        <h2>费用标准</h2>
        <div class="fee-table">
          <table>
            <thead>
              <tr>
                <th>费用类型</th>
                <th>标准</th>
                <th>说明</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>入驻保证金</td>
                <td>5,000 - 50,000 元</td>
                <td>根据经营类目不同，退出时全额退还</td>
              </tr>
              <tr>
                <td>平台服务费</td>
                <td>3% - 8%</td>
                <td>按订单成交额收取，根据类目差异</td>
              </tr>
              <tr>
                <td>技术服务费</td>
                <td>100 元/月</td>
                <td>包含 AI 设计工具、数据分析等</td>
              </tr>
              <tr>
                <td>推广费用</td>
                <td>按投放计费</td>
                <td>可选服务，按实际投放结算</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="penalty-section">
        <h2>违规处罚</h2>
        <div class="penalty-list">
          <div class="penalty-item">
            <span class="level level-1">轻微违规</span>
            <p>警告整改，限期 7 天内纠正</p>
          </div>
          <div class="penalty-item">
            <span class="level level-2">一般违规</span>
            <p>商品下架、店铺降权、扣除保证金 10%</p>
          </div>
          <div class="penalty-item">
            <span class="level level-3">严重违规</span>
            <p>店铺停业整顿 30 天、扣除保证金 50%</p>
          </div>
          <div class="penalty-item">
            <span class="level level-4">特别严重</span>
            <p>清退出驻、扣除全部保证金、永不合作</p>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
// 商家规则页面
</script>

<style scoped>
.merchant-rules-page {
  min-height: 100vh;
  background: #f5f7fa;
}
.page-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  text-align: center;
  padding: 80px 20px;
}
.page-header h1 { font-size: 48px; margin-bottom: 16px; }
.page-header p { font-size: 20px; opacity: 0.9; }
.container { max-width: 1200px; margin: 0 auto; padding: 60px 20px; }
section { margin-bottom: 60px; }
section h2 { font-size: 32px; margin-bottom: 24px; color: #1a1a2e; text-align: center; }
.rule-cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 24px; }
.rule-card { background: white; padding: 32px; border-radius: 12px; box-shadow: 0 2px 12px rgba(0,0,0,0.08); }
.rule-card .icon { font-size: 48px; margin-bottom: 16px; }
.rule-card h3 { font-size: 20px; margin-bottom: 12px; color: #1a1a2e; }
.rule-card ul { list-style: none; padding: 0; }
.rule-card ul li { padding: 6px 0; color: #666; line-height: 1.6; border-bottom: 1px solid #f0f0f0; }
.rule-card ul li:last-child { border-bottom: none; }
.conduct-list { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 24px; }
.conduct-item { background: white; padding: 24px; border-radius: 12px; border-left: 4px solid #667eea; box-shadow: 0 2px 12px rgba(0,0,0,0.08); }
.conduct-item h3 { font-size: 18px; margin-bottom: 12px; color: #1a1a2e; }
.conduct-item p { color: #666; line-height: 1.6; }
.fee-table { background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 2px 12px rgba(0,0,0,0.08); }
.fee-table table { width: 100%; border-collapse: collapse; }
.fee-table th, .fee-table td { padding: 16px; text-align: left; border-bottom: 1px solid #f0f0f0; }
.fee-table th { background: #f8f9fa; color: #1a1a2e; font-weight: 600; }
.fee-table td { color: #666; }
.penalty-list { display: flex; flex-direction: column; gap: 16px; }
.penalty-item { background: white; padding: 20px; border-radius: 12px; display: flex; align-items: center; gap: 20px; box-shadow: 0 2px 12px rgba(0,0,0,0.08); }
.level { padding: 6px 16px; border-radius: 20px; color: white; font-weight: 600; white-space: nowrap; }
.level-1 { background: #f0ad4e; }
.level-2 { background: #ff9800; }
.level-3 { background: #f44336; }
.level-4 { background: #9c27b0; }
.penalty-item p { color: #666; margin: 0; }
</style>
