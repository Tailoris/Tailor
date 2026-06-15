package com.tailoris.common.auth;

import java.util.Objects;

/**
 * 简单的用户模型（仅用于演示/测试环境的内存用户库）
 * 生产环境请替换为 JPA/MyBatis 数据库查询
 */
public class DemoUser {
    private String username;
    private String password;
    private String role;     // admin / user / merchant
    private boolean locked;

    public DemoUser() {}
    public DemoUser(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DemoUser)) return false;
        DemoUser demoUser = (DemoUser) o;
        return Objects.equals(username, demoUser.username);
    }
    @Override
    public int hashCode() { return Objects.hash(username); }
}
