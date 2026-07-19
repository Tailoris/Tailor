<template>
  <div class="contact-page">
    <div class="page-header">
      <h1>联系我们</h1>
      <p>我们随时为您提供帮助</p>
    </div>
    <div class="container">
      <div class="contact-grid">
        <section class="contact-info-section">
          <h2>联系方式</h2>
          <div class="info-cards">
            <div class="info-card">
              <div class="icon">
                📞
              </div>
              <h3>客服热线</h3>
              <p class="primary">
                400-888-8888
              </p>
              <p class="sub">
                工作时间：9:00-18:00（周一至周五）
              </p>
            </div>
            <div class="info-card">
              <div class="icon">
                📧
              </div>
              <h3>邮箱支持</h3>
              <p class="primary">
                support@tailoris.com
              </p>
              <p class="sub">
                24 小时内回复
              </p>
            </div>
            <div class="info-card">
              <div class="icon">
                💬
              </div>
              <h3>在线客服</h3>
              <p class="primary">
                点击右下角图标
              </p>
              <p class="sub">
                实时在线咨询
              </p>
            </div>
            <div class="info-card">
              <div class="icon">
                📍
              </div>
              <h3>公司地址</h3>
              <p class="primary">
                上海市浦东新区张江高科技园区
              </p>
              <p class="sub">
                裁智云大厦 18 楼
              </p>
            </div>
          </div>
        </section>

        <section class="form-section">
          <h2>在线留言</h2>
          <form
            class="contact-form"
            @submit.prevent="submitForm"
          >
            <div class="form-row">
              <div class="form-group">
                <label>姓名</label>
                <input
                  v-model="form.name"
                  type="text"
                  placeholder="请输入您的姓名"
                  required
                >
              </div>
              <div class="form-group">
                <label>电话</label>
                <input
                  v-model="form.phone"
                  type="tel"
                  placeholder="请输入联系电话"
                  required
                >
              </div>
            </div>
            <div class="form-group">
              <label>邮箱</label>
              <input
                v-model="form.email"
                type="email"
                placeholder="请输入邮箱地址"
                required
              >
            </div>
            <div class="form-group">
              <label>咨询类型</label>
              <select
                v-model="form.type"
                required
              >
                <option value="">
                  请选择
                </option>
                <option value="business">
                  商务合作
                </option>
                <option value="service">
                  服务咨询
                </option>
                <option value="complaint">
                  投诉建议
                </option>
                <option value="other">
                  其他
                </option>
              </select>
            </div>
            <div class="form-group">
              <label>留言内容</label>
              <textarea
                v-model="form.message"
                rows="5"
                placeholder="请详细描述您的需求"
                required
              />
            </div>
            <button
              type="submit"
              class="submit-btn"
            >
              提交留言
            </button>
          </form>
        </section>
      </div>

      <section class="social-section">
        <h2>关注我们</h2>
        <div class="social-list">
          <div class="social-item">
            <div class="icon">
              💬
            </div>
            <p>微信公众号</p>
            <span class="account">TailorIS_Official</span>
          </div>
          <div class="social-item">
            <div class="icon">
              🐦
            </div>
            <p>官方微博</p>
            <span class="account">@裁智云官方</span>
          </div>
          <div class="social-item">
            <div class="icon">
              📱
            </div>
            <p>抖音号</p>
            <span class="account">tailoris_official</span>
          </div>
          <div class="social-item">
            <div class="icon">
              🔗
            </div>
            <p>LinkedIn</p>
            <span class="account">Tailor IS</span>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';

const form = ref({
  name: '',
  phone: '',
  email: '',
  type: '',
  message: ''
});

const submitForm = () => {
  alert('感谢您的留言，我们会尽快与您联系！');
  form.value = { name: '', phone: '', email: '', type: '', message: '' };
};
</script>

<style scoped>
.contact-page { min-height: 100vh; background: #f5f7fa; }
.page-header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; text-align: center; padding: 80px 20px; }
.page-header h1 { font-size: 48px; margin-bottom: 16px; }
.page-header p { font-size: 20px; opacity: 0.9; }
.container { max-width: 1200px; margin: 0 auto; padding: 60px 20px; }
section { margin-bottom: 60px; }
section h2 { font-size: 32px; margin-bottom: 24px; color: #1a1a2e; text-align: center; }
.contact-grid { display: grid; grid-template-columns: 1fr; gap: 40px; }
.info-cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 24px; }
.info-card { background: white; padding: 32px; border-radius: 12px; text-align: center; box-shadow: 0 2px 12px rgba(0,0,0,0.08); transition: transform 0.3s; }
.info-card:hover { transform: translateY(-4px); }
.info-card .icon { font-size: 48px; margin-bottom: 16px; }
.info-card h3 { font-size: 18px; margin-bottom: 12px; color: #1a1a2e; }
.info-card .primary { font-size: 16px; color: #667eea; font-weight: 600; margin-bottom: 4px; }
.info-card .sub { font-size: 13px; color: #999; }
.contact-form { background: white; padding: 40px; border-radius: 12px; box-shadow: 0 2px 12px rgba(0,0,0,0.08); max-width: 700px; margin: 0 auto; }
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.form-group { margin-bottom: 20px; }
.form-group label { display: block; margin-bottom: 8px; color: #1a1a2e; font-weight: 500; }
.form-group input, .form-group select, .form-group textarea { width: 100%; padding: 12px; border: 1px solid #e0e0e0; border-radius: 8px; font-size: 14px; box-sizing: border-box; }
.form-group input:focus, .form-group select:focus, .form-group textarea:focus { outline: none; border-color: #667eea; }
.submit-btn { width: 100%; padding: 14px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; border: none; border-radius: 8px; font-size: 16px; font-weight: 600; cursor: pointer; }
.social-list { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 24px; }
.social-item { background: white; padding: 24px; border-radius: 12px; text-align: center; box-shadow: 0 2px 12px rgba(0,0,0,0.08); }
.social-item .icon { font-size: 40px; margin-bottom: 12px; }
.social-item p { color: #1a1a2e; font-weight: 600; margin-bottom: 4px; }
.social-item .account { color: #667eea; font-size: 14px; }
</style>
