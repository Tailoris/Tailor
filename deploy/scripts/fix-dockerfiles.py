#!/usr/bin/env python3
"""
修复所有微服务 Dockerfile，确保复制所有模块的 pom.xml
"""
import os
from pathlib import Path

# 所有模块列表
ALL_MODULES = [
    'tailor-is-common',
    'tailor-is-common-web',
    'tailor-is-api',
    'tailor-is-core-gateway',
    'tailor-is-lite-gateway',
    'tailor-is-user',
    'tailor-is-merchant',
    'tailor-is-product',
    'tailor-is-order',
    'tailor-is-payment',
    'tailor-is-marketing',
    'tailor-is-ai',
    'tailor-is-copyright',
    'tailor-is-community',
    'tailor-is-supply',
    'tailor-is-message',
    'tailor-is-message-im',
    'tailor-is-academy',
    'tailor-is-analytics',
    'tailor-is-pattern',
    'tailor-is-admin',
]

# 需要修复的服务模块
SERVICE_MODULES = [
    'tailor-is-user',
    'tailor-is-merchant',
    'tailor-is-product',
    'tailor-is-order',
    'tailor-is-payment',
    'tailor-is-marketing',
    'tailor-is-ai',
    'tailor-is-community',
    'tailor-is-core-gateway',
    'tailor-is-lite-gateway',
    'tailor-is-admin',
    'tailor-is-copyright',
    'tailor-is-supply',
    'tailor-is-message',
    'tailor-is-message-im',
    'tailor-is-academy',
    'tailor-is-analytics',
    'tailor-is-pattern',
]

def fix_dockerfile(service_module: str):
    """修复单个服务的 Dockerfile"""
    dockerfile_path = Path(f'/home/tailor/Tailoris/tailor-is/{service_module}/Dockerfile')
    
    if not dockerfile_path.exists():
        print(f"⚠️  {service_module}: Dockerfile 不存在，跳过")
        return False
    
    content = dockerfile_path.read_text()
    
    # 生成新的 pom.xml 复制部分
    pom_copy_lines = ['# 复制所有模块的 pom.xml（父 POM 引用所有模块）']
    pom_copy_lines.append('COPY pom.xml /build/pom.xml')
    for module in ALL_MODULES:
        pom_copy_lines.append(f'COPY {module}/pom.xml /build/{module}/pom.xml')
    
    # 生成新的源码复制部分
    src_copy_lines = ['# 复制源码（仅复制依赖模块和当前模块）']
    src_copy_lines.append('COPY tailor-is-common/src /build/tailor-is-common/src')
    src_copy_lines.append('COPY tailor-is-common-web/src /build/tailor-is-common-web/src')
    src_copy_lines.append(f'COPY {service_module}/src /build/{service_module}/src')
    
    # 替换 pom.xml 复制部分
    lines = content.split('\n')
    new_lines = []
    in_pom_section = False
    in_src_section = False
    pom_replaced = False
    src_replaced = False
    
    for i, line in enumerate(lines):
        # 检测 pom.xml 复制部分
        if 'COPY' in line and 'pom.xml' in line and '/build/' in line:
            if not pom_replaced:
                new_lines.extend(pom_copy_lines)
                pom_replaced = True
            continue
        
        # 检测源码复制部分
        if 'COPY' in line and '/src' in line and '/build/' in line:
            if not src_replaced:
                new_lines.extend(src_copy_lines)
                src_replaced = True
            continue
        
        new_lines.append(line)
    
    # 写回文件
    dockerfile_path.write_text('\n'.join(new_lines))
    print(f"✅ {service_module}: Dockerfile 已修复")
    return True

def main():
    print("🔧 开始修复所有微服务 Dockerfile...")
    print(f"📦 共 {len(SERVICE_MODULES)} 个服务模块需要修复")
    print()
    
    fixed_count = 0
    for service in SERVICE_MODULES:
        if fix_dockerfile(service):
            fixed_count += 1
    
    print()
    print(f"✅ 修复完成：{fixed_count}/{len(SERVICE_MODULES)} 个 Dockerfile")

if __name__ == '__main__':
    main()
