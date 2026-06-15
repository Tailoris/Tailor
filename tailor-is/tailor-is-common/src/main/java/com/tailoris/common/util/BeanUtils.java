package com.tailoris.common.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class BeanUtils {

    private BeanUtils() {
    }

    public static void copyProperties(Object source, Object target) {
        if (source == null || target == null) {
            return;
        }
        try {
            BeanInfo sourceBeanInfo = Introspector.getBeanInfo(source.getClass(), Object.class);
            PropertyDescriptor[] sourceDescriptors = sourceBeanInfo.getPropertyDescriptors();

            BeanInfo targetBeanInfo = Introspector.getBeanInfo(target.getClass(), Object.class);
            java.util.Map<String, PropertyDescriptor> targetDescriptorMap = new java.util.HashMap<>();
            for (PropertyDescriptor pd : targetBeanInfo.getPropertyDescriptors()) {
                targetDescriptorMap.put(pd.getName(), pd);
            }

            for (PropertyDescriptor sourcePd : sourceDescriptors) {
                Method readMethod = sourcePd.getReadMethod();
                if (readMethod == null) {
                    continue;
                }

                PropertyDescriptor targetPd = targetDescriptorMap.get(sourcePd.getName());
                if (targetPd != null) {
                    Method writeMethod = targetPd.getWriteMethod();
                    if (writeMethod != null && readMethod.getReturnType().isAssignableFrom(writeMethod.getParameterTypes()[0])) {
                        Object value = readMethod.invoke(source);
                        writeMethod.invoke(target, value);
                    }
                }
            }
        } catch (IntrospectionException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Copy properties failed", e);
        }
    }

    public static <T> T copyProperties(Object source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            copyProperties(source, target);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("Copy properties failed", e);
        }
    }

    public static <S, T> List<T> copyList(Collection<S> sourceList, Class<T> targetClass) {
        if (StringUtils.isBlank(sourceList)) {
            return new ArrayList<>();
        }
        List<T> targetList = new ArrayList<>(sourceList.size());
        for (S source : sourceList) {
            targetList.add(copyProperties(source, targetClass));
        }
        return targetList;
    }

    public static Map<String, Object> beanToMap(Object bean) {
        if (bean == null) {
            return null;
        }
        try {
            Map<String, Object> map = new java.util.HashMap<>();
            BeanInfo beanInfo = Introspector.getBeanInfo(bean.getClass(), Object.class);
            PropertyDescriptor[] descriptors = beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor descriptor : descriptors) {
                Method readMethod = descriptor.getReadMethod();
                if (readMethod != null) {
                    Object value = readMethod.invoke(bean);
                    map.put(descriptor.getName(), value);
                }
            }
            return map;
        } catch (IntrospectionException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Convert bean to map failed", e);
        }
    }

    public static <T> T mapToBean(Map<String, Object> map, Class<T> beanClass) {
        if (StringUtils.isBlank(map)) {
            return null;
        }
        try {
            T bean = beanClass.getDeclaredConstructor().newInstance();
            BeanInfo beanInfo = Introspector.getBeanInfo(beanClass, Object.class);
            PropertyDescriptor[] descriptors = beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor descriptor : descriptors) {
                Object value = map.get(descriptor.getName());
                if (value != null) {
                    Method writeMethod = descriptor.getWriteMethod();
                    if (writeMethod != null) {
                        writeMethod.invoke(bean, value);
                    }
                }
            }
            return bean;
        } catch (Exception e) {
            throw new RuntimeException("Convert map to bean failed", e);
        }
    }
}
