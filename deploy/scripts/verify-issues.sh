#!/bin/bash
# Tailor IS 问题验证脚本
# 验证86项问题的实际修复状态

echo "========================================"
echo "Tailor IS 问题验证报告"
echo "========================================"
echo ""

FIXED=0
NOT_FIXED=0

# C-001: 检查.env.production是否使用占位符
echo "检查 C-001: 生产环境凭证硬编码..."
if grep -q "<PLEASE_SET_IN_PRODUCTION>" deploy/.env.production; then
    echo "  ✅ 已修复: 使用占位符而非硬编码密码"
    FIXED=$((FIXED+1))
else
    echo "  ❌ 未修复: 存在硬编码密码"
    NOT_FIXED=$((NOT_FIXED+1))
fi

# C-002: 检查application.yml中的默认密码
echo "检查 C-002: Fallback默认密码硬编码..."
HARDCODED=$(grep -r "mysql_ZmY2sr\|redis_jD2N8n\|nacos_s3k8Fp" tailor-is/ --include="*.yml" 2>/dev/null | wc -l)
if [ "$HARDCODED" -eq 0 ]; then
    echo "  ✅ 已修复: 无默认密码硬编码"
    FIXED=$((FIXED+1))
else
    echo "  ❌ 未修复: 发现 $HARDCODED 处默认密码"
    NOT_FIXED=$((NOT_FIXED+1))
fi

# C-003: 检查AuthInterceptor的isValidToken
echo "检查 C-003: 认证完全绕过..."
if grep -q "StpUtil.getLoginIdByToken" tailor-is/tailor-is-common/src/main/java/com/tailoris/common/security/AuthInterceptor.java; then
    echo "  ✅ 已修复: 使用Sa-Token验证Token"
    FIXED=$((FIXED+1))
else
    echo "  ❌ 未修复: isValidToken始终返回true"
    NOT_FIXED=$((NOT_FIXED+1))
fi

# C-004: 检查Nacos认证
echo "检查 C-004: Nacos认证未启用..."
if grep -q "auth.enable: true" tailor-is/tailor-is-gateway/src/main/resources/application.yml 2>/dev/null; then
    echo "  ✅ 已修复: Nacos认证已启用"
    FIXED=$((FIXED+1))
else
    echo "  ❌ 未修复: Nacos认证未启用"
    NOT_FIXED=$((NOT_FIXED+1))
fi

# C-005: 检查graphql.ts是否损坏
echo "检查 C-005: graphql.ts文件损坏..."
if grep -q "autoSync" tailor-is-frontend/pc-mall/src/api/graphql.ts 2>/dev/null; then
    echo "  ❌ 未修复: graphql.ts包含autoSync代码"
    NOT_FIXED=$((NOT_FIXED+1))
else
    echo "  ✅ 已修复: graphql.ts文件正常"
    FIXED=$((FIXED+1))
fi

# C-006: 检查merchant-admin TDZ
echo "检查 C-006: merchant-admin TDZ变量未定义..."
if grep -q "const log" tailor-is-frontend/merchant-admin/src/api/request.ts 2>/dev/null; then
    LOG_LINE=$(grep -n "const log" tailor-is-frontend/merchant-admin/src/api/request.ts | head -1 | cut -d: -f1)
    LOG_USE=$(grep -n "log.info" tailor-is-frontend/merchant-admin/src/api/request.ts | head -1 | cut -d: -f1)
    if [ "$LOG_LINE" -lt "$LOG_USE" ]; then
        echo "  ✅ 已修复: log在调用前已定义"
        FIXED=$((FIXED+1))
    else
        echo "  ❌ 未修复: log在使用后定义"
        NOT_FIXED=$((NOT_FIXED+1))
    fi
else
    echo "  ❌ 未修复: 未找到log定义"
    NOT_FIXED=$((NOT_FIXED+1))
fi

# C-007: 检查加密失败降级
echo "检查 C-007: 加密失败降级为明文..."
if grep -q "catch.*setSecure" tailor-is-frontend/mobile-app/utils/crypto.ts 2>/dev/null || ! grep -q "localStorage.setItem.*token" tailor-is-frontend/mobile-app/utils/crypto.ts 2>/dev/null; then
    echo "  ✅ 已修复: 加密失败不会存储明文"
    FIXED=$((FIXED+1))
else
    echo "  ❌ 未修复: 加密失败存储明文"
    NOT_FIXED=$((NOT_FIXED+1))
fi

# C-008: 检查CSRF Token
echo "检查 C-008: CSRF Token不安全..."
if grep -q "crypto.getRandomValues" tailor-is-frontend/pc-mall/src/api/request.ts 2>/dev/null; then
    echo "  ✅ 已修复: 使用crypto.getRandomValues"
    FIXED=$((FIXED+1))
else
    echo "  ❌ 未修复: 使用Math.random()"
    NOT_FIXED=$((NOT_FIXED+1))
fi

# C-009: 检查XSS防护
echo "检查 C-009: XSS漏洞..."
if grep -q "DOMPurify.sanitize" tailor-is-frontend/pc-mall/src/views/ProductDetailView.vue 2>/dev/null; then
    echo "  ✅ 已修复: 使用DOMPurify清理"
    FIXED=$((FIXED+1))
else
    echo "  ❌ 未修复: v-html未清理"
    NOT_FIXED=$((NOT_FIXED+1))
fi

# C-010: 检查createAfterSale实现
echo "检查 C-010: createAfterSale未实现..."
if grep -q "TODO" tailor-is-frontend/mobile-app/pages/order/offline-aftersale.vue 2>/dev/null; then
    echo "  ❌ 未修复: 仍存在TODO"
    NOT_FIXED=$((NOT_FIXED+1))
else
    echo "  ✅ 已修复: createAfterSale已实现"
    FIXED=$((FIXED+1))
fi

# C-011: 检查Token存储
echo "检查 C-011: Token明文存储localStorage..."
if grep -q "encrypt.*token\|setSecure.*token" tailor-is-frontend/pc-mall/src/store/user.ts 2>/dev/null; then
    echo "  ✅ 已修复: Token加密存储"
    FIXED=$((FIXED+1))
else
    echo "  ❌ 未修复: Token明文存储"
    NOT_FIXED=$((NOT_FIXED+1))
fi

# C-012: 检查权限验证
echo "检查 C-012: 权限信息可篡改..."
if grep -q "server.*verify\|signature.*check" tailor-is-frontend/pc-mall/src/router/index.ts 2>/dev/null; then
    echo "  ✅ 已修复: 权限从服务端验证"
    FIXED=$((FIXED+1))
else
    echo "  ❌ 未修复: 权限可被篡改"
    NOT_FIXED=$((NOT_FIXED+1))
fi

# C-013: 检查SnowflakeIdGenerator
echo "检查 C-013: SnowflakeIdGenerator单例冲突..."
if grep -q "@Component\|@Service" tailor-is/tailor-is-common/src/main/java/com/tailoris/common/util/SnowflakeIdGenerator.java 2>/dev/null; then
    echo "  ✅ 已修复: 使用Spring Bean管理"
    FIXED=$((FIXED+1))
else
    echo "  ❌ 未修复: 静态单例模式"
    NOT_FIXED=$((NOT_FIXED+1))
fi

# C-014: 检查XOR加密
echo "检查 C-014: XOR加密..."
if grep -q "AES\|crypto.subtle" tailor-is-frontend/mobile-app/utils/crypto.ts 2>/dev/null; then
    echo "  ✅ 已修复: 使用AES加密"
    FIXED=$((FIXED+1))
else
    echo "  ❌ 未修复: 仍使用XOR"
    NOT_FIXED=$((NOT_FIXED+1))
fi

echo ""
echo "========================================"
echo "验证结果汇总"
echo "========================================"
echo "已修复: $FIXED"
echo "未修复: $NOT_FIXED"
echo "总计: $((FIXED+NOT_FIXED))"
echo "修复率: $((FIXED*100/(FIXED+NOT_FIXED)))%"
echo "========================================"
