package com.tailoris.common;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 服务欢迎页（解决访问根路径 / 显示空白的问题）
 *
 *   GET /              - 欢迎页 (HTML)，展示服务状态、可用接口、监控链接
 *   GET /api           - API 目录 (HTML)
 *
 * 说明：本控制器返回纯 HTML，不影响其他 /api/* 的 JSON 接口。
 */
@RestController
public class WelcomeController {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String welcome() {
        String now = ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).format(FORMATTER);
        return "<!DOCTYPE html>\n" +
                "<html lang=\"zh-CN\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\"/>\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>\n" +
                "  <title>Tailor IS - User Service</title>\n" +
                "  <style>\n" +
                "    *{box-sizing:border-box;margin:0;padding:0}\n" +
                "    body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,'Helvetica Neue',Arial,sans-serif;background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);min-height:100vh;display:flex;align-items:center;justify-content:center;padding:20px}\n" +
                "    .card{background:#fff;border-radius:16px;box-shadow:0 20px 60px rgba(0,0,0,0.2);max-width:720px;width:100%;overflow:hidden}\n" +
                "    .header{background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);color:#fff;padding:32px;text-align:center}\n" +
                "    .header h1{font-size:28px;font-weight:700;margin-bottom:8px}\n" +
                "    .header p{opacity:.9;font-size:14px}\n" +
                "    .status{display:inline-flex;align-items:center;gap:8px;background:rgba(255,255,255,0.2);padding:8px 16px;border-radius:20px;margin-top:16px;font-size:13px}\n" +
                "    .status-dot{width:8px;height:8px;background:#4ade80;border-radius:50%;box-shadow:0 0 0 3px rgba(74,222,128,.3);animation:pulse 2s infinite}\n" +
                "    @keyframes pulse{0%,100%{box-shadow:0 0 0 3px rgba(74,222,128,.3)}50%{box-shadow:0 0 0 6px rgba(74,222,128,.1)}}\n" +
                "    .content{padding:32px}\n" +
                "    .section{margin-bottom:24px}\n" +
                "    .section:last-child{margin-bottom:0}\n" +
                "    .section-title{font-size:14px;font-weight:600;color:#374151;margin-bottom:12px;text-transform:uppercase;letter-spacing:.5px}\n" +
                "    .info-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(200px,1fr));gap:12px}\n" +
                "    .info-item{background:#f9fafb;border:1px solid #e5e7eb;border-radius:8px;padding:12px}\n" +
                "    .info-label{font-size:11px;color:#6b7280;text-transform:uppercase;letter-spacing:.5px;margin-bottom:4px}\n" +
                "    .info-value{font-size:14px;color:#111827;font-weight:500;word-break:break-all}\n" +
                "    ul.endpoints{list-style:none;display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:8px}\n" +
                "    ul.endpoints li{display:flex;align-items:center;gap:10px;padding:10px 12px;background:#f9fafb;border:1px solid #e5e7eb;border-radius:8px;font-size:13px;transition:all .15s}\n" +
                "    ul.endpoints li:hover{background:#eff6ff;border-color:#bfdbfe;transform:translateY(-1px)}\n" +
                "    .method{font-size:10px;font-weight:700;padding:3px 8px;border-radius:4px;flex-shrink:0}\n" +
                "    .method-get{background:#dbeafe;color:#1d4ed8}\n" +
                "    .method-post{background:#dcfce7;color:#15803d}\n" +
                "    .path{color:#111827;font-family:'SF Mono','Monaco','Menlo',monospace;font-size:12px}\n" +
                "    .links{display:flex;flex-wrap:wrap;gap:8px}\n" +
                "    .link{display:inline-flex;align-items:center;gap:6px;padding:8px 14px;background:#f3f4f6;color:#374151;text-decoration:none;border-radius:8px;font-size:13px;font-weight:500;transition:all .15s}\n" +
                "    .link:hover{background:#667eea;color:#fff;transform:translateY(-1px)}\n" +
                "    .footer{background:#f9fafb;padding:16px 32px;text-align:center;font-size:12px;color:#6b7280;border-top:1px solid #e5e7eb}\n" +
                "    .footer code{background:#e5e7eb;padding:2px 6px;border-radius:4px;color:#374151;font-size:11px}\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div class=\"card\">\n" +
                "    <div class=\"header\">\n" +
                "      <h1>✂️ Tailor IS · User Service</h1>\n" +
                "      <p>裁智云 — 用户认证与业务指标服务</p>\n" +
                "      <div class=\"status\"><span class=\"status-dot\"></span>服务运行中</div>\n" +
                "    </div>\n" +
                "    <div class=\"content\">\n" +
                "      <div class=\"section\">\n" +
                "        <div class=\"section-title\">服务信息</div>\n" +
                "        <div class=\"info-grid\">\n" +
                "          <div class=\"info-item\">\n" +
                "            <div class=\"info-label\">服务名称</div>\n" +
                "            <div class=\"info-value\">tailor-is-user</div>\n" +
                "          </div>\n" +
                "          <div class=\"info-item\">\n" +
                "            <div class=\"info-label\">启动时间</div>\n" +
                "            <div class=\"info-value\">" + now + "</div>\n" +
                "          </div>\n" +
                "          <div class=\"info-item\">\n" +
                "            <div class=\"info-label\">服务类型</div>\n" +
                "            <div class=\"info-value\">Spring Boot · REST API</div>\n" +
                "          </div>\n" +
                "          <div class=\"info-item\">\n" +
                "            <div class=\"info-label\">时区</div>\n" +
                "            <div class=\"info-value\">Asia/Shanghai (UTC+8)</div>\n" +
                "          </div>\n" +
                "        </div>\n" +
                "      </div>\n" +
                "      <div class=\"section\">\n" +
                "        <div class=\"section-title\">可用 API 端点</div>\n" +
                "        <ul class=\"endpoints\">\n" +
                "          <li><span class=\"method method-post\">POST</span><span class=\"path\">/api/auth/login</span></li>\n" +
                "          <li><span class=\"method method-post\">POST</span><span class=\"path\">/api/auth/logout</span></li>\n" +
                "          <li><span class=\"method method-get\">GET</span><span class=\"path\">/api/auth/health</span></li>\n" +
                "          <li><span class=\"method method-post\">POST</span><span class=\"path\">/api/password-reset/request</span></li>\n" +
                "          <li><span class=\"method method-post\">POST</span><span class=\"path\">/api/password-reset/confirm</span></li>\n" +
                "          <li><span class=\"method method-get\">GET</span><span class=\"path\">/api/metrics/snapshot</span></li>\n" +
                "          <li><span class=\"method method-post\">POST</span><span class=\"path\">/api/metrics/simulate</span></li>\n" +
                "          <li><span class=\"method method-get\">GET</span><span class=\"path\">/actuator/health</span></li>\n" +
                "          <li><span class=\"method method-get\">GET</span><span class=\"path\">/actuator/info</span></li>\n" +
                "          <li><span class=\"method method-get\">GET</span><span class=\"path\">/actuator/prometheus</span></li>\n" +
                "          <li><span class=\"method method-get\">GET</span><span class=\"path\">/actuator/metrics</span></li>\n" +
                "        </ul>\n" +
                "      </div>\n" +
                "      <div class=\"section\">\n" +
                "        <div class=\"section-title\">快速链接</div>\n" +
                "        <div class=\"links\">\n" +
                "          <a class=\"link\" href=\"/actuator/health\">🩺 健康检查</a>\n" +
                "          <a class=\"link\" href=\"/actuator/prometheus\">📊 Prometheus 指标</a>\n" +
                "          <a class=\"link\" href=\"/api/metrics/snapshot\">📈 业务指标快照</a>\n" +
                "          <a class=\"link\" href=\"/api/auth/health\">🔐 认证服务</a>\n" +
                "        </div>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "    <div class=\"footer\">\n" +
                "      使用 <code>curl -X POST /api/auth/login</code> 进行登录测试 · 监控请访问 Grafana\n" +
                "    </div>\n" +
                "  </div>\n" +
                "</body>\n" +
                "</html>";
    }

    @GetMapping(value = "/api", produces = MediaType.TEXT_HTML_VALUE)
    public String apiIndex() {
        return welcome();
    }
}
