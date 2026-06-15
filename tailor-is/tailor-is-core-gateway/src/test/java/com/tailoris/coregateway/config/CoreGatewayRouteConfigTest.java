package com.tailoris.coregateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.BooleanSpec;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.PredicateSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("CoreGatewayRouteConfig 单元测试")
class CoreGatewayRouteConfigTest {

    @Test
    @DisplayName("路由配置不为空")
    void testCustomRouteLocator_NotNull() {
        CoreGatewayRouteConfig config = new CoreGatewayRouteConfig();
        RouteLocatorBuilder builder = mock(RouteLocatorBuilder.class);
        RouteLocatorBuilder.Builder routesBuilder = mock(RouteLocatorBuilder.Builder.class);
        when(builder.routes()).thenReturn(routesBuilder);
        when(routesBuilder.route(anyString(), any())).thenReturn(routesBuilder);
        when(routesBuilder.build()).thenReturn(mock(RouteLocator.class));

        RouteLocator routeLocator = config.customRouteLocator(builder);

        assertNotNull(routeLocator);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    @DisplayName("所有 10 条路由均已定义且 Lambda 可执行")
    void testAllRoutesAreDefinedAndInvocable() {
        CoreGatewayRouteConfig config = new CoreGatewayRouteConfig();
        RouteLocatorBuilder builder = mock(RouteLocatorBuilder.class);
        RouteLocatorBuilder.Builder routesBuilder = mock(RouteLocatorBuilder.Builder.class);
        when(builder.routes()).thenReturn(routesBuilder);

        ArgumentCaptor<Function> fnCaptor = ArgumentCaptor.forClass(Function.class);
        when(routesBuilder.route(anyString(), fnCaptor.capture())).thenReturn(routesBuilder);
        when(routesBuilder.build()).thenReturn(mock(RouteLocator.class));

        RouteLocator routeLocator = config.customRouteLocator(builder);
        assertNotNull(routeLocator);

        // 验证 10 条路由全部注册
        List<Function> fns = fnCaptor.getAllValues();
        assertEquals(10, fns.size(), "应定义 10 条路由");

        // 构建 mock 链: PredicateSpec → BooleanSpec → GatewayFilterSpec → Route.Builder
        PredicateSpec predicateSpec = mock(PredicateSpec.class);
        BooleanSpec booleanSpec = mock(BooleanSpec.class);
        GatewayFilterSpec filterSpec = mock(GatewayFilterSpec.class);
        Route.Builder routeBuilder = mock(Route.Builder.class);

        // path() 返回 BooleanSpec
        when(predicateSpec.path(any(String[].class))).thenReturn(booleanSpec);
        // BooleanSpec 的 filters() 方法会调用传入的 lambda
        when(booleanSpec.filters(any(Function.class))).thenAnswer(inv -> {
            Function<GatewayFilterSpec, GatewayFilterSpec> innerFn = inv.getArgument(0);
            return innerFn.apply(filterSpec);
        });
        when(filterSpec.stripPrefix(anyInt())).thenReturn(filterSpec);
        when(filterSpec.uri(anyString())).thenReturn(routeBuilder);

        // 逐条执行路由 Lambda, 覆盖所有 path/filters/uri 调用
        for (Function fn : fns) {
            Object result = fn.apply(predicateSpec);
            assertNotNull(result, "每条路由 Lambda 应返回非空 Route.Builder");
        }

        // 验证 path() 被调用了 10 次 (每条路由一次)
        verify(predicateSpec, times(10)).path(any(String[].class));
        // 验证 uri() 被调用了 10 次
        verify(filterSpec, times(10)).uri(anyString());
        // 验证 stripPrefix(0) 被调用了 10 次
        verify(filterSpec, times(10)).stripPrefix(0);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    @DisplayName("路由 builder.routes() 和 build() 均被调用")
    void testBuilderChaining() {
        CoreGatewayRouteConfig config = new CoreGatewayRouteConfig();
        RouteLocatorBuilder builder = mock(RouteLocatorBuilder.class);
        RouteLocatorBuilder.Builder routesBuilder = mock(RouteLocatorBuilder.Builder.class);
        RouteLocator expectedLocator = mock(RouteLocator.class);

        when(builder.routes()).thenReturn(routesBuilder);
        when(routesBuilder.route(anyString(), any(Function.class))).thenReturn(routesBuilder);
        when(routesBuilder.build()).thenReturn(expectedLocator);

        RouteLocator result = config.customRouteLocator(builder);

        assertSame(expectedLocator, result);
        verify(builder).routes();
        verify(routesBuilder).build();
    }
}
