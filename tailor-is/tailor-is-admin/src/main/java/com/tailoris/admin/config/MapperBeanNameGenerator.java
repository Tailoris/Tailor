package com.tailoris.admin.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;

/**
 * 使用全限定类名作为Mapper Bean名,避免不同包中同名接口冲突
 */
public class MapperBeanNameGenerator extends AnnotationBeanNameGenerator {
    @Override
    public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
        String className = definition.getBeanClassName();
        if (className == null) {
            return super.generateBeanName(definition, registry);
        }
        // 用全限定类名作为Bean名
        return className;
    }
}
