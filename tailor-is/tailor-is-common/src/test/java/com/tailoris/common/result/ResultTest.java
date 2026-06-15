package com.tailoris.common.result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("统一返回结果测试")
class ResultTest {

    @Test
    @DisplayName("成功返回 - 无数据")
    void testSuccess() {
        Result<Void> result = Result.success();
        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("成功返回 - 带数据")
    void testSuccessWithData() {
        String data = "test data";
        Result<String> result = Result.success(data);
        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
        assertEquals(data, result.getData());
    }

    @Test
    @DisplayName("成功返回 - 复杂对象")
    void testSuccessWithComplexObject() {
        class User {
            String name;
            int age;
            User(String name, int age) {
                this.name = name;
                this.age = age;
            }
        }
        User user = new User("张三", 25);
        Result<User> result = Result.success(user);
        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertEquals("张三", result.getData().name);
        assertEquals(25, result.getData().age);
    }

    @Test
    @DisplayName("失败返回 - 仅消息")
    void testFailWithMessage() {
        Result<Void> result = Result.fail("操作失败");
        assertNotNull(result);
        assertEquals(500, result.getCode());
        assertEquals("操作失败", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("失败返回 - 自定义代码和消息")
    void testFailWithCodeAndMessage() {
        Result<Void> result = Result.fail(400, "参数错误");
        assertNotNull(result);
        assertEquals(400, result.getCode());
        assertEquals("参数错误", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("失败返回 - 使用ResultCode")
    void testFailWithResultCode() {
        Result<Void> result = Result.fail(ResultCode.BAD_REQUEST);
        assertNotNull(result);
        assertEquals(400, result.getCode());
        assertEquals("请求参数错误", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("失败返回 - 不同的ResultCode")
    void testFailWithDifferentResultCodes() {
        Result<Void> unauthorized = Result.fail(ResultCode.UNAUTHORIZED);
        assertEquals(401, unauthorized.getCode());

        Result<Void> forbidden = Result.fail(ResultCode.FORBIDDEN);
        assertEquals(403, forbidden.getCode());

        Result<Void> notFound = Result.fail(ResultCode.NOT_FOUND);
        assertEquals(404, notFound.getCode());

        Result<Void> businessError = Result.fail(ResultCode.BUSINESS_ERROR);
        assertEquals(4000, businessError.getCode());

        Result<Void> systemError = Result.fail(ResultCode.SYSTEM_ERROR);
        assertEquals(5000, systemError.getCode());
    }

    @Test
    @DisplayName("返回结果 - getter/setter")
    void testGettersSetters() {
        Result<String> result = new Result<>();
        result.setCode(200);
        result.setMessage("test");
        result.setData("data");

        assertEquals(200, result.getCode());
        assertEquals("test", result.getMessage());
        assertEquals("data", result.getData());
    }

    @Test
    @DisplayName("返回结果 - null数据")
    void testNullData() {
        Result<String> result = Result.success(null);
        assertNull(result.getData());
    }

    @Test
    @DisplayName("返回结果 - 空字符串")
    void testEmptyString() {
        Result<String> result = Result.success("");
        assertEquals("", result.getData());
    }
}
