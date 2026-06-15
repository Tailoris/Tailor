package com.tailoris.common.client;

import com.tailoris.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "tailor-is-user", path = "/api/user", fallbackFactory = UserClientFallbackFactory.class)
public interface UserClient {

    @GetMapping("/profile/{userId}")
    Result<Map<String, Object>> getUserProfile(@PathVariable Long userId);
}