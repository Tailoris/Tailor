package com.tailoris.coregateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class EncodingResponseFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            HttpHeaders headers = exchange.getResponse().getHeaders();
            MediaType contentType = headers.getContentType();
            
            if (contentType != null) {
                String contentTypeStr = contentType.toString();
                if (!contentTypeStr.contains("charset")) {
                    if (contentType.isCompatibleWith(MediaType.APPLICATION_JSON) ||
                        contentType.isCompatibleWith(MediaType.TEXT_PLAIN) ||
                        contentType.isCompatibleWith(MediaType.TEXT_HTML) ||
                        contentType.toString().contains("javascript") ||
                        contentType.toString().contains("json")) {
                        headers.setContentType(new MediaType(contentType, java.nio.charset.StandardCharsets.UTF_8));
                        log.debug("Added UTF-8 charset to Content-Type: {}", headers.getContentType());
                    }
                }
            } else {
                headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
            }
            
            headers.add(HttpHeaders.CONTENT_ENCODING, "UTF-8");
        }));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}