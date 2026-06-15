package com.tailoris.common.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {

    @Override
    public UserClient create(Throwable cause) {
        return userId -> com.tailoris.common.result.Result.fail("用户服务暂时不可用，请稍后重试");
    }
}