<template>
  <div class="product-table">
    <el-table :data="products" v-loading="loading" stripe style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" sortable />
      <el-table-column label="商品图片" width="100">
        <template #default="{ row }">
          <el-image
            :src="row.images?.[0] || 'https://via.placeholder.com/60x60'"
            fit="cover"
            style="width: 60px; height: 60px; border-radius: 4px"
          />
        </template>
      </el-table-column>
      <el-table-column prop="name" label="商品名称" min-width="180" show-overflow-tooltip />
      <el-table-column prop="category" label="分类" width="120" />
      <el-table-column label="类型" width="80">
        <template #default="{ row }">
          {{ row.type === 'physical' ? '实物' : '虚拟' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType[row.status]" size="small">
            {{ statusLabel[row.status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="160" sortable />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="$emit('edit', row.id)">编辑</el-button>
          <el-button
            v-if="row.status === 2"
            link
            type="warning"
            size="small"
            @click="$emit('updateStatus', row.id, 3)"
          >
            下架
          </el-button>
          <el-button
            v-else-if="row.status === 3"
            link
            type="success"
            size="small"
            @click="$emit('updateStatus', row.id, 2)"
          >
            上架
          </el-button>
          <el-button link type="danger" size="small" @click="$emit('delete', row.id)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无数据" />
      </template>
    </el-table>
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="$emit('pageChange', { page: currentPage, pageSize })"
        @size-change="$emit('pageChange', { page: 1, pageSize })"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { Product } from '@/types'

defineProps<{
  products: Product[]
  loading: boolean
  total: number
  currentPage: number
  pageSize: number
}>()

defineEmits<{
  edit: [id: number]
  delete: [id: number]
  updateStatus: [id: number, status: number]
  pageChange: [params: { page: number; pageSize: number }]
}>()

const currentPage = ref(1)
const pageSize = ref(10)

const statusType: Record<number, string> = {
  0: 'info',
  1: 'warning',
  2: 'success',
  3: 'danger',
}

const statusLabel: Record<number, string> = {
  0: '草稿',
  1: '待审核',
  2: '上架',
  3: '下架',
}
</script>

<style scoped>
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
