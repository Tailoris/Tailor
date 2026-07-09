package com.tailoris.coregateway.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

@Component
public class RouteChangeMonitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RouteChangeMonitor.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RouteDefinitionLocator routeDefinitionLocator;
    private Set<String> currentRouteIds = new HashSet<>();

    public RouteChangeMonitor(RouteDefinitionLocator routeDefinitionLocator) {
        this.routeDefinitionLocator = routeDefinitionLocator;
    }

    @PostConstruct
    public void init() {
        routeDefinitionLocator.getRouteDefinitions()
                .collectList()
                .subscribe(routeDefinitions -> {
                    routeDefinitions.forEach(route -> currentRouteIds.add(route.getId()));
                    LOGGER.info("[路由监控] 初始化完成，当前路由数量: {}", currentRouteIds.size());
                });
    }

    @EventListener(RefreshRoutesEvent.class)
    public void handleRefreshRoutes(RefreshRoutesEvent event) {
        routeDefinitionLocator.getRouteDefinitions()
                .collectList()
                .subscribe(newRoutes -> {
                    Set<String> newRouteIds = new HashSet<>();
                    newRoutes.forEach(route -> newRouteIds.add(route.getId()));

                    Set<String> addedRoutes = new HashSet<>(newRouteIds);
                    addedRoutes.removeAll(currentRouteIds);

                    Set<String> removedRoutes = new HashSet<>(currentRouteIds);
                    removedRoutes.removeAll(newRouteIds);

                    if (!addedRoutes.isEmpty() || !removedRoutes.isEmpty()) {
                        StringBuilder logMessage = new StringBuilder();
                        logMessage.append("[路由变更] ")
                                .append("时间: ").append(LocalDateTime.now().format(FORMATTER)).append(", ");

                        if (!addedRoutes.isEmpty()) {
                            logMessage.append("新增路由: ").append(addedRoutes).append(", ");
                        }

                        if (!removedRoutes.isEmpty()) {
                            logMessage.append("删除路由: ").append(removedRoutes).append(", ");
                        }

                        logMessage.append("当前总数: ").append(newRouteIds.size());

                        LOGGER.warn(logMessage.toString());
                    }

                    currentRouteIds = newRouteIds;
                });
    }
}