package com.tailoris.academy.serverless;

import com.tailoris.academy.AcademyApplication;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import java.io.IOException;

/**
 * 学堂服务 Serverless 函数入口处理器
 *
 * 用于在阿里云函数计算 (FC) / 腾讯云 SCF 中运行 Spring Boot 应用。
 * 通过轻量级容器封装整个 Spring Boot 上下文，实现 HTTP 请求转发。
 *
 * 使用方式：
 *  - Handler 配置: com.tailoris.academy.serverless.AcademyFunctionHandler::handleRequest
 *  - 运行时: custom-container (推荐) 或 java11/java17
 */
public class AcademyFunctionHandler {

    private volatile static DispatcherServlet dispatcherServlet;
    private volatile static ApplicationContextWrapper contextWrapper;

    /**
     * 函数入口方法
     *
     * @param request  HTTP 请求对象 (由 FC Runtime 注入)
     * @param response HTTP 响应对象
     * @throws IOException 当 IO 操作失败时抛出
     */
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ensureInitialized();
        try {
            dispatcherServlet.service(request, response);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json; charset=utf-8");
            response.getWriter().write("{\"error\":\"Internal Server Error\",\"message\":\"" + e.getMessage() + "\"}");
        }
    }

    /**
     * 确保 Spring 应用上下文已初始化（单例延迟加载）
     */
    private synchronized void ensureInitialized() {
        if (dispatcherServlet != null) {
            return;
        }

        // 创建轻量级 Spring 上下文
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.setAllowBeanDefinitionOverriding(true);

        // 设置 serverless profile
        context.getEnvironment().setDefaultProfiles("serverless");

        // 配置 Spring Boot 属性用于 Serverless 环境
        context.getEnvironment().getSystemProperties().put("spring.main.web-application-type", "servlet");
        context.getEnvironment().getSystemProperties().put("spring.main.lazy-initialization", "true");
        context.getEnvironment().getSystemProperties().put("spring.cloud.nacos.discovery.register-enabled", "false");
        context.getEnvironment().getSystemProperties().put("spring.cloud.nacos.config.import-check.enabled", "false");
        context.getEnvironment().getSystemProperties().put("server.port", "0");

        // 启动 Spring Boot 应用
        SpringApplicationBuilder builder = new SpringApplicationBuilder(AcademyApplication.class)
                .web(org.springframework.boot.WebApplicationType.SERVLET)
                .properties("server.port=0")
                .profiles("serverless");
        contextWrapper = new ApplicationContextWrapper(builder.run());

        dispatcherServlet = contextWrapper.getDispatcherServlet();
    }

    /**
     * 应用上下文包装器，提取 DispatcherServlet 用于请求处理
     */
    private static class ApplicationContextWrapper {
        private final org.springframework.context.ConfigurableApplicationContext applicationContext;
        private final DispatcherServlet dispatcherServlet;

        public ApplicationContextWrapper(org.springframework.context.ConfigurableApplicationContext applicationContext) {
            this.applicationContext = applicationContext;
            this.dispatcherServlet = applicationContext.getBean(DispatcherServlet.class);
        }

        public DispatcherServlet getDispatcherServlet() {
            return dispatcherServlet;
        }

        public void close() {
            if (applicationContext != null) {
                applicationContext.close();
            }
        }
    }
}
