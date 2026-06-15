package com.tailoris.common.result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("返回码枚举测试")
class ResultCodeTest {

    @Test
    @DisplayName("SUCCESS枚举值")
    void testSuccess() {
        assertEquals(200, ResultCode.SUCCESS.getCode());
        assertEquals("操作成功", ResultCode.SUCCESS.getMessage());
    }

    @Test
    @DisplayName("FAIL枚举值")
    void testFail() {
        assertEquals(500, ResultCode.FAIL.getCode());
        assertEquals("操作失败", ResultCode.FAIL.getMessage());
    }

    @Test
    @DisplayName("BAD_REQUEST枚举值")
    void testBadRequest() {
        assertEquals(400, ResultCode.BAD_REQUEST.getCode());
        assertEquals("请求参数错误", ResultCode.BAD_REQUEST.getMessage());
    }

    @Test
    @DisplayName("UNAUTHORIZED枚举值")
    void testUnauthorized() {
        assertEquals(401, ResultCode.UNAUTHORIZED.getCode());
        assertEquals("未登录", ResultCode.UNAUTHORIZED.getMessage());
    }

    @Test
    @DisplayName("FORBIDDEN枚举值")
    void testForbidden() {
        assertEquals(403, ResultCode.FORBIDDEN.getCode());
        assertEquals("无权限", ResultCode.FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("NOT_FOUND枚举值")
    void testNotFound() {
        assertEquals(404, ResultCode.NOT_FOUND.getCode());
        assertEquals("资源不存在", ResultCode.NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("PARAM_ERROR枚举值")
    void testParamError() {
        assertEquals(400, ResultCode.PARAM_ERROR.getCode());
        assertEquals("参数错误", ResultCode.PARAM_ERROR.getMessage());
    }

    @Test
    @DisplayName("INTERNAL_ERROR枚举值")
    void testInternalError() {
        assertEquals(500, ResultCode.INTERNAL_ERROR.getCode());
        assertEquals("系统内部错误", ResultCode.INTERNAL_ERROR.getMessage());
    }

    @Test
    @DisplayName("BUSINESS_ERROR枚举值")
    void testBusinessError() {
        assertEquals(4000, ResultCode.BUSINESS_ERROR.getCode());
        assertEquals("业务异常", ResultCode.BUSINESS_ERROR.getMessage());
    }

    @Test
    @DisplayName("SYSTEM_ERROR枚举值")
    void testSystemError() {
        assertEquals(5000, ResultCode.SYSTEM_ERROR.getCode());
        assertEquals("系统异常", ResultCode.SYSTEM_ERROR.getMessage());
    }

    @Test
    @DisplayName("所有枚举值都有code和message")
    void testAllEnumsHaveCodeAndMessage() {
        for (ResultCode code : ResultCode.values()) {
            assertNotNull(code.getCode());
            assertNotNull(code.getMessage());
            assertFalse(code.getMessage().isEmpty());
        }
    }

    @Test
    @DisplayName("枚举值数量")
    void testEnumCount() {
        assertEquals(10, ResultCode.values().length);
    }

    @Test
    @DisplayName("valueOf方法")
    void testValueOf() {
        assertEquals(ResultCode.SUCCESS, ResultCode.valueOf("SUCCESS"));
        assertEquals(ResultCode.FAIL, ResultCode.valueOf("FAIL"));
        assertEquals(ResultCode.BAD_REQUEST, ResultCode.valueOf("BAD_REQUEST"));
    }
}
