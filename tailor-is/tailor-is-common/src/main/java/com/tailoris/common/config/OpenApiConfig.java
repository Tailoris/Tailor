package com.tailoris.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Tailor IS API")
                        .version("1.0.0")
                        .description("裁智云服装全产业平台 API 文档")
                        .contact(new Contact()
                                .name("Tailor IS Team")
                                .email("dev@tailoris.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:9000")
                                .description("开发环境"),
                        new Server()
                                .url("https://api.tailoris.com")
                                .description("生产环境")
                ))
                .tags(Arrays.asList(
                        new Tag().name("用户管理").description("用户注册、登录、信息管理"),
                        new Tag().name("认证管理").description("登录、注册、Token管理"),
                        new Tag().name("订单管理").description("订单创建、查询、支付"),
                        new Tag().name("商品管理").description("商品CRUD、库存管理"),
                        new Tag().name("支付服务").description("支付、退款、结算"),
                        new Tag().name("营销服务").description("优惠券、积分、秒杀"),
                        new Tag().name("AI服务").description("AI算法、体型识别"),
                        new Tag().name("版权服务").description("版权存证、侵权检测"),
                        new Tag().name("商户管理").description("商户入驻、店铺管理"),
                        new Tag().name("管理后台").description("平台管理接口"),
                        new Tag().name("纸样服务").description("纸样定制、设计"),
                        new Tag().name("社区服务").description("社区帖子、评论"),
                        new Tag().name("供应链服务").description("面料辅料供应"),
                        new Tag().name("消息服务").description("消息推送、通知"),
                        new Tag().name("数据分析").description("数据统计、报表"),
                        new Tag().name("学堂服务").description("在线课程、学习"),
                        new Tag().name("IM服务").description("即时通讯")
                ));
    }
}