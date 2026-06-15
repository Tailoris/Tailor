<template>
  <div class="profile-view">
    <div class="profile-layout">
      <aside class="profile-sidebar">
        <div class="user-card">
          <el-avatar :size="80" :src="userStore.userInfo?.avatar || 'https://via.placeholder.com/80x80'" />
          <h3>{{ userStore.userInfo?.nickName || userStore.userInfo?.username || '用户' }}</h3>
          <p>普通会员</p>
        </div>
        <el-menu :default-active="activeMenu" @select="handleMenuSelect">
          <el-menu-item index="profile">
            <el-icon><User /></el-icon>
            <span>个人资料</span>
          </el-menu-item>
          <el-menu-item index="address">
            <el-icon><Location /></el-icon>
            <span>收货地址</span>
          </el-menu-item>
          <el-menu-item index="favorites">
            <el-icon><Star /></el-icon>
            <span>我的收藏</span>
          </el-menu-item>
          <el-menu-item index="points">
            <el-icon><Trophy /></el-icon>
            <span>我的积分</span>
          </el-menu-item>
          <el-menu-item index="orders">
            <el-icon><Document /></el-icon>
            <span>订单历史</span>
          </el-menu-item>
        </el-menu>
      </aside>

      <main class="profile-main">
        <div v-if="activeMenu === 'profile'" class="tab-content">
          <h2>个人资料</h2>
          <el-form :model="profileForm" :rules="profileRules" ref="profileFormRef" label-width="100px">
            <el-form-item label="昵称" prop="nickName">
              <el-input v-model="profileForm.nickName" />
            </el-form-item>
            <el-form-item label="头像" prop="avatar">
              <el-input v-model="profileForm.avatar" placeholder="请输入头像URL" />
            </el-form-item>
            <el-form-item label="性别">
              <el-radio-group v-model="profileForm.gender">
                <el-radio :label="0">保密</el-radio>
                <el-radio :label="1">男</el-radio>
                <el-radio :label="2">女</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="生日">
              <el-date-picker v-model="profileForm.birthday" type="date" placeholder="选择生日" format="YYYY-MM-DD" value-format="YYYY-MM-DD" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="profileForm.email" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleUpdateProfile" :loading="updating">保存</el-button>
            </el-form-item>
          </el-form>

          <el-divider />

          <h2>修改密码</h2>
          <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="100px">
            <el-form-item label="旧密码" prop="oldPassword">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleUpdatePassword" :loading="updating">修改密码</el-button>
            </el-form-item>
          </el-form>
        </div>

        <div v-if="activeMenu === 'address'" class="tab-content">
          <h2>收货地址</h2>
          <el-button type="primary" @click="showAddressDialog = true" style="margin-bottom: 16px">新增地址</el-button>
          <div class="address-grid">
            <div v-for="addr in addressList" :key="addr.id" class="address-card">
              <div class="address-header">
                <span class="name">{{ addr.name }}</span>
                <span class="phone">{{ addr.phone }}</span>
                <el-tag v-if="addr.isDefault === 1" size="small" type="primary">默认</el-tag>
              </div>
              <div class="address-detail">
                {{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.detail }}
              </div>
              <div class="address-actions">
                <el-button v-if="addr.isDefault !== 1" type="primary" link size="small" @click="handleSetDefault(addr.id)">设为默认</el-button>
                <el-button type="danger" link size="small" @click="handleDeleteAddress(addr.id)">删除</el-button>
              </div>
            </div>
          </div>
          <el-empty v-if="addressList.length === 0" description="暂无收货地址" />
        </div>

        <div v-if="activeMenu === 'favorites'" class="tab-content">
          <h2>我的收藏</h2>
          <div class="product-grid">
            <ProductCard v-for="product in favorites" :key="product.id" :product="product" />
          </div>
          <el-empty v-if="favorites.length === 0" description="暂无收藏" />
        </div>

        <div v-if="activeMenu === 'points'" class="tab-content">
          <h2>我的积分</h2>
          <div class="points-summary">
            <div class="points-value">{{ points }}</div>
            <div class="points-label">可用积分</div>
          </div>
          <el-table :data="pointsRecords" border>
            <el-table-column prop="description" label="说明" />
            <el-table-column prop="points" label="积分变动">
              <template #default="{ row }">
                <span :class="{ positive: row.points > 0, negative: row.points < 0 }">
                  {{ row.points > 0 ? '+' : '' }}{{ row.points }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="时间">
              <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无数据" />
            </template>
          </el-table>
        </div>

        <div v-if="activeMenu === 'orders'" class="tab-content">
          <h2>订单历史</h2>
          <router-link to="/orders" class="view-orders-link">查看完整订单列表 →</router-link>
        </div>
      </main>
    </div>

    <el-dialog v-model="showAddressDialog" title="新增收货地址" width="500px">
      <el-form :model="newAddress" :rules="addressRules" ref="addressFormRef" label-width="80px">
        <el-form-item label="收货人" prop="name">
          <el-input v-model="newAddress.name" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="newAddress.phone" />
        </el-form-item>
        <el-form-item label="所在地区">
          <el-row :gutter="8">
            <el-col :span="8"><el-input v-model="newAddress.province" placeholder="省" /></el-col>
            <el-col :span="8"><el-input v-model="newAddress.city" placeholder="市" /></el-col>
            <el-col :span="8"><el-input v-model="newAddress.district" placeholder="区" /></el-col>
          </el-row>
        </el-form-item>
        <el-form-item label="详细地址" prop="detail">
          <el-input v-model="newAddress.detail" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddressDialog = false">取消</el-button>
        <el-button type="primary" @click="handleAddAddress">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { User, Location, Star, Trophy, Document } from '@element-plus/icons-vue'
import ProductCard from '@/components/ProductCard.vue'
import { useUserStore } from '@/store/user'
import { updateProfile, updatePassword, getFavorites, getPoints } from '@/api/user'
import { getAddresses, deleteAddress, setDefaultAddress, createAddress } from '@/api/address'
import { formatDate } from '@/utils/format'
import type { Address, Product } from '@/types'

const userStore = useUserStore()
const activeMenu = ref('profile')

const profileForm = ref({
  nickName: '',
  avatar: '',
  gender: 0,
  birthday: '',
  email: ''
})
const profileFormRef = ref<FormInstance>()
const updating = ref(false)

const profileRules: FormRules = {
  nickName: [{ max: 20, message: '昵称不超过20个字符', trigger: 'blur' }]
}

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const passwordFormRef = ref<FormInstance>()
const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码不少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== passwordForm.value.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const addressList = ref<Address[]>([])
const showAddressDialog = ref(false)
const addressFormRef = ref<FormInstance>()
const newAddress = ref({
  name: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  detail: ''
})
const addressRules: FormRules = {
  name: [{ required: true, message: '请输入收货人', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  detail: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}

const favorites = ref<Product[]>([])
const points = ref(0)
const pointsRecords = ref<{ id: number; points: number; type: number; description: string; createdAt: string }[]>([])

function handleMenuSelect(index: string) {
  activeMenu.value = index
  if (index === 'address') loadAddresses()
  if (index === 'favorites') loadFavorites()
  if (index === 'points') loadPoints()
}

async function handleUpdateProfile() {
  if (!profileFormRef.value) return
  await profileFormRef.value.validate(async (valid) => {
    if (valid) {
      updating.value = true
      try {
        await updateProfile(profileForm.value)
        ElMessage.success('资料更新成功')
        await userStore.fetchUserInfo()
      } catch {
        ElMessage.error('更新失败')
      } finally {
        updating.value = false
      }
    }
  })
}

async function handleUpdatePassword() {
  if (!passwordFormRef.value) return
  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      updating.value = true
      try {
        await updatePassword(passwordForm.value.oldPassword, passwordForm.value.newPassword)
        ElMessage.success('密码修改成功')
        passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
      } catch {
        ElMessage.error('修改失败')
      } finally {
        updating.value = false
      }
    }
  })
}

async function loadAddresses() {
  try {
    addressList.value = await getAddresses()
  } catch {
    addressList.value = []
  }
}

async function handleAddAddress() {
  if (!addressFormRef.value) return
  await addressFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await createAddress(newAddress.value)
        ElMessage.success('地址添加成功')
        showAddressDialog.value = false
        loadAddresses()
        newAddress.value = { name: '', phone: '', province: '', city: '', district: '', detail: '' }
      } catch {
        ElMessage.error('添加失败')
      }
    }
  })
}

async function handleDeleteAddress(id: number) {
  try {
    await ElMessageBox.confirm('确定删除该地址？', '提示', { type: 'warning' })
    await deleteAddress(id)
    ElMessage.success('删除成功')
    loadAddresses()
  } catch {
    // cancelled
  }
}

async function handleSetDefault(id: number) {
  try {
    await setDefaultAddress(id)
    ElMessage.success('设置成功')
    loadAddresses()
  } catch {
    ElMessage.error('设置失败')
  }
}

async function loadFavorites() {
  try {
    const res = await getFavorites()
    favorites.value = res.records
  } catch {
    favorites.value = []
  }
}

async function loadPoints() {
  try {
    const res = await getPoints()
    points.value = res.points
    pointsRecords.value = res.records
  } catch {
    points.value = 0
    pointsRecords.value = []
  }
}

onMounted(() => {
  if (userStore.userInfo) {
    profileForm.value = {
      nickName: userStore.userInfo.nickName || '',
      avatar: userStore.userInfo.avatar || '',
      gender: userStore.userInfo.gender,
      birthday: userStore.userInfo.birthday || '',
      email: userStore.userInfo.email || ''
    }
  }
})
</script>

<style scoped>
.profile-view {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}
.profile-layout {
  display: flex;
  gap: 24px;
}
.profile-sidebar {
  width: 220px;
  flex-shrink: 0;
}
.user-card {
  text-align: center;
  padding: 24px 16px;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 16px;
}
.user-card h3 {
  margin: 12px 0 4px;
  font-size: 16px;
  color: #333;
}
.user-card p {
  font-size: 12px;
  color: #999;
  margin: 0;
}
.profile-main {
  flex: 1;
  min-width: 0;
  background: #fff;
  border-radius: 8px;
  padding: 24px;
}
.tab-content h2 {
  font-size: 20px;
  color: #333;
  margin: 0 0 20px;
}
.address-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.address-card {
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 16px;
}
.address-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.address-header .name {
  font-weight: 600;
}
.address-detail {
  font-size: 14px;
  color: #666;
  line-height: 1.5;
  margin-bottom: 12px;
}
.address-actions {
  display: flex;
  gap: 8px;
}
.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}
.points-summary {
  text-align: center;
  padding: 32px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  color: #fff;
  margin-bottom: 24px;
}
.points-value {
  font-size: 48px;
  font-weight: 700;
}
.points-label {
  font-size: 14px;
  opacity: 0.8;
}
.positive {
  color: #52c41a;
  font-weight: 600;
}
.negative {
  color: #f5222d;
  font-weight: 600;
}
.view-orders-link {
  color: #1d39c4;
  text-decoration: none;
}
</style>
