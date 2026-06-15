package com.tailoris.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ratelimit")
public class RateLimitConfig {

    private boolean enabled = true;

    private Global global = new Global();
    private IpLevel ipLevel = new IpLevel();
    private UserLevel userLevel = new UserLevel();
    private EndpointLevel endpointLevel = new EndpointLevel();

    private int defaultPermitsPerSecond = 10;
    private int redisExpireSeconds = 60;
    private String message = "请求过于频繁，请稍后再试";

    public static class Global {
        private int permitsPerSecond = 1000;
        public int getPermitsPerSecond() { return permitsPerSecond; }
        public void setPermitsPerSecond(int permitsPerSecond) { this.permitsPerSecond = permitsPerSecond; }
    }

    public static class IpLevel {
        private int permitsPerSecond = 20;
        private int capacitySeconds = 60;
        public int getPermitsPerSecond() { return permitsPerSecond; }
        public void setPermitsPerSecond(int permitsPerSecond) { this.permitsPerSecond = permitsPerSecond; }
        public int getCapacitySeconds() { return capacitySeconds; }
        public void setCapacitySeconds(int capacitySeconds) { this.capacitySeconds = capacitySeconds; }
    }

    public static class UserLevel {
        private int permitsPerSecond = 50;
        private int capacitySeconds = 60;
        public int getPermitsPerSecond() { return permitsPerSecond; }
        public void setPermitsPerSecond(int permitsPerSecond) { this.permitsPerSecond = permitsPerSecond; }
        public int getCapacitySeconds() { return capacitySeconds; }
        public void setCapacitySeconds(int capacitySeconds) { this.capacitySeconds = capacitySeconds; }
    }

    public static class EndpointLevel {
        private int permitsPerSecond = 30;
        private int capacitySeconds = 60;
        public int getPermitsPerSecond() { return permitsPerSecond; }
        public void setPermitsPerSecond(int permitsPerSecond) { this.permitsPerSecond = permitsPerSecond; }
        public int getCapacitySeconds() { return capacitySeconds; }
        public void setCapacitySeconds(int capacitySeconds) { this.capacitySeconds = capacitySeconds; }
    }

    public Global getGlobal() { return global; }
    public void setGlobal(Global global) { this.global = global; }
    public IpLevel getIpLevel() { return ipLevel; }
    public void setIpLevel(IpLevel ipLevel) { this.ipLevel = ipLevel; }
    public UserLevel getUserLevel() { return userLevel; }
    public void setUserLevel(UserLevel userLevel) { this.userLevel = userLevel; }
    public EndpointLevel getEndpointLevel() { return endpointLevel; }
    public void setEndpointLevel(EndpointLevel endpointLevel) { this.endpointLevel = endpointLevel; }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getDefaultPermitsPerSecond() {
        return defaultPermitsPerSecond;
    }

    public void setDefaultPermitsPerSecond(int defaultPermitsPerSecond) {
        this.defaultPermitsPerSecond = defaultPermitsPerSecond;
    }

    public int getRedisExpireSeconds() {
        return redisExpireSeconds;
    }

    public void setRedisExpireSeconds(int redisExpireSeconds) {
        this.redisExpireSeconds = redisExpireSeconds;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}