<template>
  <div class="profile-page">
    <el-row :gutter="20">
      <el-col
        :xs="24"
        :md="8"
      >
        <el-card class="profile-card">
          <div class="avatar-section">
            <el-avatar
              :size="100"
              :src="profile?.avatar"
            >
              <el-icon :size="50">
                <User />
              </el-icon>
            </el-avatar>
            <h3 class="username">
              {{ profile?.realName || profile?.username }}
            </h3>
            <p class="role-tag">
              <el-tag
                v-for="role in profile?.roles"
                :key="role.roleCode"
                type="primary"
                effect="light"
                size="small"
              >
                {{ role.roleName }}
              </el-tag>
            </p>
          </div>
          <el-divider />
          <div class="info-list">
            <div class="info-item">
              <span class="info-label">用户名</span>
              <span class="info-value">{{ profile?.username }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">手机号</span>
              <span class="info-value">{{ profile?.phone || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">邮箱</span>
              <span class="info-value">{{ profile?.email || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">上次登录</span>
              <span class="info-value">{{ formatDateTime(profile?.lastLoginTime) }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col
        :xs="24"
        :md="16"
      >
        <el-tabs
          v-model="activeTab"
          class="profile-tabs"
        >
          <el-tab-pane
            label="基本信息"
            name="basic"
          >
            <el-card>
              <el-form
                ref="basicFormRef"
                :model="basicForm"
                :rules="basicRules"
                label-width="100px"
                class="profile-form"
              >
                <el-form-item
                  label="真实姓名"
                  prop="realName"
                >
                  <el-input
                    v-model="basicForm.realName"
                    placeholder="请输入真实姓名"
                    maxlength="32"
                  />
                </el-form-item>
                <el-form-item
                  label="手机号"
                  prop="phone"
                >
                  <el-input
                    v-model="basicForm.phone"
                    placeholder="请输入手机号"
                    maxlength="11"
                  />
                </el-form-item>
                <el-form-item
                  label="邮箱"
                  prop="email"
                >
                  <el-input
                    v-model="basicForm.email"
                    placeholder="请输入邮箱"
                    maxlength="64"
                  />
                </el-form-item>
                <el-form-item>
                  <el-button
                    type="primary"
                    :loading="saving"
                    @click="handleSaveBasic"
                  >
                    保存修改
                  </el-button>
                  <el-button @click="loadProfile">
                    重置
                  </el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </el-tab-pane>
          <el-tab-pane
            label="修改密码"
            name="password"
          >
            <el-card>
              <el-form
                ref="passwordFormRef"
                :model="passwordForm"
                :rules="passwordRules"
                label-width="100px"
                class="profile-form"
              >
                <el-form-item
                  label="旧密码"
                  prop="oldPassword"
                >
                  <el-input
                    v-model="passwordForm.oldPassword"
                    type="password"
                    placeholder="请输入旧密码"
                    show-password
                    maxlength="32"
                  />
                </el-form-item>
                <el-form-item
                  label="新密码"
                  prop="newPassword"
                >
                  <el-input
                    v-model="passwordForm.newPassword"
                    type="password"
                    placeholder="请输入新密码（6-32位）"
                    show-password
                    maxlength="32"
                  />
                </el-form-item>
                <el-form-item
                  label="确认新密码"
                  prop="confirmPassword"
                >
                  <el-input
                    v-model="passwordForm.confirmPassword"
                    type="password"
                    placeholder="请再次输入新密码"
                    show-password
                    maxlength="32"
                  />
                </el-form-item>
                <el-form-item>
                  <el-button
                    type="primary"
                    :loading="changingPwd"
                    @click="handleChangePassword"
                  >
                    修改密码
                  </el-button>
                  <el-button @click="resetPasswordForm">
                    重置
                  </el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </el-tab-pane>
        </el-tabs>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { User } from '@element-plus/icons-vue';
import { getProfile, updateProfile, updatePassword, type AdminProfile } from '@/api/profile';
import { useRouter } from 'vue-router';

const router = useRouter();
const activeTab = ref('basic');
const profile = ref<AdminProfile | null>(null);
const saving = ref(false);
const changingPwd = ref(false);

const basicFormRef = ref<FormInstance>();
const basicForm = reactive({
  realName: '',
  phone: '',
  email: '',
});

const basicRules: FormRules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' },
  ],
};

const passwordFormRef = ref<FormInstance>();
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
});

const validateConfirmPassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'));
  } else {
    callback();
  }
};

const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为6-32位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
};

async function loadProfile() {
  try {
    const data = await getProfile();
    if (data) {
      profile.value = data;
      basicForm.realName = data.realName || '';
      basicForm.phone = data.phone || '';
      basicForm.email = data.email || '';
    }
  } catch {
    ElMessage.error('获取用户信息失败');
  }
}

async function handleSaveBasic() {
  if (!basicFormRef.value) return;
  await basicFormRef.value.validate(async (valid) => {
    if (!valid) return;
    saving.value = true;
    try {
      await updateProfile({
        realName: basicForm.realName,
        phone: basicForm.phone,
        email: basicForm.email,
      });
      ElMessage.success('保存成功');
      loadProfile();
    } catch {
      ElMessage.error('保存失败');
    } finally {
      saving.value = false;
    }
  });
}

function resetPasswordForm() {
  passwordForm.oldPassword = '';
  passwordForm.newPassword = '';
  passwordForm.confirmPassword = '';
  passwordFormRef.value?.clearValidate();
}

async function handleChangePassword() {
  if (!passwordFormRef.value) return;
  await passwordFormRef.value.validate(async (valid) => {
    if (!valid) return;
    try {
      await ElMessageBox.confirm('确定要修改密码吗？修改后需要重新登录。', '确认修改', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      });
    } catch {
      return;
    }
    changingPwd.value = true;
    try {
      await updatePassword(passwordForm.oldPassword, passwordForm.newPassword);
      ElMessage.success('密码修改成功，请重新登录');
      localStorage.removeItem('token');
      localStorage.removeItem('userInfo');
      router.push('/login');
    } catch {
      ElMessage.error('密码修改失败');
    } finally {
      changingPwd.value = false;
    }
  });
}

function formatDateTime(time?: string): string {
  if (!time) return '-';
  return time.replace('T', ' ').slice(0, 19);
}

onMounted(() => {
  loadProfile();
});
</script>

<style scoped>
.profile-page {
  padding: 20px;
}

.profile-card {
  text-align: center;
}

.avatar-section {
  padding: 20px 0 10px;
}

.username {
  margin: 12px 0 8px;
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.role-tag {
  margin: 0;
  display: flex;
  gap: 6px;
  justify-content: center;
  flex-wrap: wrap;
}

.info-list {
  padding: 0 10px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  font-size: 13px;
  border-bottom: 1px solid #f3f4f6;
}

.info-item:last-child {
  border-bottom: none;
}

.info-label {
  color: #6b7280;
}

.info-value {
  color: #1f2937;
  font-weight: 500;
}

.profile-tabs {
  margin-top: 0;
}

.profile-form {
  max-width: 500px;
  padding: 10px 0;
}
</style>
