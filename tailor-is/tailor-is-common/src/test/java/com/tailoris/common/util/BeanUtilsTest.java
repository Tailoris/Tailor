package com.tailoris.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BeanUtils 测试")
class BeanUtilsTest {

    public static class SourceBean {
        private String name;
        private Integer age;
        private String email;

        public SourceBean() {}

        public SourceBean(String name, Integer age, String email) {
            this.name = name;
            this.age = age;
            this.email = email;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class TargetBean {
        private String name;
        private Integer age;
        private String address;

        public TargetBean() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
    }

    @Nested
    @DisplayName("copyProperties 测试")
    class CopyPropertiesTests {

        @Test
        @DisplayName("复制同名属性")
        void testCopyPropertiesMatchingFields() {
            SourceBean source = new SourceBean("张三", 25, "zhangsan@example.com");
            TargetBean target = new TargetBean();

            BeanUtils.copyProperties(source, target);

            assertEquals("张三", target.getName());
            assertEquals(25, target.getAge());
            assertNull(target.getAddress());
        }

        @Test
        @DisplayName("源对象为 null 时不抛出异常")
        void testCopyPropertiesNullSource() {
            TargetBean target = new TargetBean();
            assertDoesNotThrow(() -> BeanUtils.copyProperties(null, target));
        }

        @Test
        @DisplayName("目标对象为 null 时不抛出异常")
        void testCopyPropertiesNullTarget() {
            SourceBean source = new SourceBean("张三", 25, "zhangsan@example.com");
            Object target = null;
            assertDoesNotThrow(() -> BeanUtils.copyProperties(source, target));
        }

        @Test
        @DisplayName("复制属性到指定 Class")
        void testCopyPropertiesToClass() {
            SourceBean source = new SourceBean("李四", 30, "lisi@example.com");
            TargetBean target = BeanUtils.copyProperties(source, TargetBean.class);

            assertNotNull(target);
            assertEquals("李四", target.getName());
            assertEquals(30, target.getAge());
        }

        @Test
        @DisplayName("源对象为 null 时返回 null")
        void testCopyPropertiesToClassNullSource() {
            TargetBean target = BeanUtils.copyProperties(null, TargetBean.class);
            assertNull(target);
        }
    }

    @Nested
    @DisplayName("copyList 测试")
    class CopyListTests {

        @Test
        @DisplayName("复制对象列表")
        void testCopyList() {
            List<SourceBean> sourceList = Arrays.asList(
                new SourceBean("张三", 25, "zhangsan@example.com"),
                new SourceBean("李四", 30, "lisi@example.com")
            );

            List<TargetBean> targetList = BeanUtils.copyList(sourceList, TargetBean.class);

            assertEquals(2, targetList.size());
            assertEquals("张三", targetList.get(0).getName());
            assertEquals("李四", targetList.get(1).getName());
        }

        @Test
        @DisplayName("空列表返回空列表")
        void testCopyListEmpty() {
            List<TargetBean> targetList = BeanUtils.copyList(new ArrayList<>(), TargetBean.class);
            assertNotNull(targetList);
            assertTrue(targetList.isEmpty());
        }

        @Test
        @DisplayName("null 列表返回空列表")
        void testCopyListNull() {
            List<TargetBean> targetList = BeanUtils.copyList(null, TargetBean.class);
            assertNotNull(targetList);
            assertTrue(targetList.isEmpty());
        }
    }

    @Nested
    @DisplayName("beanToMap 测试")
    class BeanToMapTests {

        @Test
        @DisplayName("Bean 转 Map")
        void testBeanToMap() {
            SourceBean bean = new SourceBean("王五", 28, "wangwu@example.com");
            Map<String, Object> map = BeanUtils.beanToMap(bean);

            assertNotNull(map);
            assertEquals("王五", map.get("name"));
            assertEquals(28, map.get("age"));
            assertEquals("wangwu@example.com", map.get("email"));
        }

        @Test
        @DisplayName("null Bean 返回 null")
        void testBeanToMapNull() {
            Map<String, Object> map = BeanUtils.beanToMap(null);
            assertNull(map);
        }
    }

    @Nested
    @DisplayName("mapToBean 测试")
    class MapToBeanTests {

        @Test
        @DisplayName("Map 转 Bean")
        void testMapToBean() {
            Map<String, Object> map = new HashMap<>();
            map.put("name", "赵六");
            map.put("age", 35);
            map.put("email", "zhaoliu@example.com");

            SourceBean bean = BeanUtils.mapToBean(map, SourceBean.class);

            assertNotNull(bean);
            assertEquals("赵六", bean.getName());
            assertEquals(35, bean.getAge());
            assertEquals("zhaoliu@example.com", bean.getEmail());
        }

        @Test
        @DisplayName("null Map 返回 null")
        void testMapToBeanNull() {
            SourceBean bean = BeanUtils.mapToBean(null, SourceBean.class);
            assertNull(bean);
        }

        @Test
        @DisplayName("空 Map 返回 null（因为 isBlank 判断为空）")
        void testMapToBeanEmpty() {
            SourceBean bean = BeanUtils.mapToBean(new HashMap<>(), SourceBean.class);
            assertNull(bean);
        }
    }
}
