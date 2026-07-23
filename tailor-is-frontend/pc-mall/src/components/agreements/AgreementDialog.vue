<template>
  <el-dialog
    v-model="visible"
    :title="title"
    width="700px"
    role="dialog"
    aria-modal="true"
    :aria-labelledby="'agreement-dialog-title'"
    class="agreement-dialog"
  >
    <template #header>
      <span
        :id="'agreement-dialog-title'"
        class="dialog-title"
      >{{ title }}</span>
    </template>
    <div class="agreement-wrapper">
      <UserAgreementContent v-if="type === 'user'" />
      <PrivacyPolicyContent v-else />
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import UserAgreementContent from './UserAgreementContent.vue';
import PrivacyPolicyContent from './PrivacyPolicyContent.vue';

const props = defineProps<{
  modelValue: boolean;
  type: 'user' | 'privacy';
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void;
}>();

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
});

const title = computed(() => (props.type === 'user' ? '用户协议' : '隐私政策'));
</script>

<style scoped>
.dialog-title {
  font-size: 18px;
  font-weight: 600;
  color: #1d39c4;
}

.agreement-wrapper {
  padding: 0 8px;
}

@media (max-width: 768px) {
  .agreement-dialog {
    width: 90% !important;
    margin: 0 auto;
  }
}
</style>
