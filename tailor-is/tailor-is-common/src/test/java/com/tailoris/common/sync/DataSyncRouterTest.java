package com.tailoris.common.sync;

import com.tailoris.common.config.DataSyncStrategyConfig;
import com.tailoris.common.config.DataSyncStrategyConfig.SyncLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

/**
 * DataSyncRouter 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DataSyncRouter 单元测试")
class DataSyncRouterTest {

    @Mock
    private RealTimeDataSync realTimeDataSync;

    @Mock
    private NearRealTimeSyncScheduler nearRealTimeSyncScheduler;

    private DataSyncRouter router;

    @BeforeEach
    void setUp() {
        router = new DataSyncRouter(realTimeDataSync, nearRealTimeSyncScheduler);
    }

    @Test
    @DisplayName("核心数据应路由到实时同步")
    void routeSync_coreData_shouldRouteToRealTime() {
        SyncLevel level = router.routeSync("order", "create", Map.of("id", "123"));

        assertThat(level).isEqualTo(SyncLevel.REAL_TIME);
        verify(realTimeDataSync).publishAfterCommit(eq("order"), eq("create"), any());
    }

    @Test
    @DisplayName("非核心数据应路由到近实时同步")
    void routeSync_nonCoreData_shouldRouteToNearRealTime() {
        SyncLevel level = router.routeSync("community_post", "create", Map.of("id", "123"));

        assertThat(level).isEqualTo(SyncLevel.NEAR_REAL_TIME);
    }

    @Test
    @DisplayName("批量数据应跳过即时路由")
    void routeSync_batchData_shouldSkipImmediateRouting() {
        SyncLevel level = router.routeSync("report", "create", Map.of("id", "123"));

        assertThat(level).isEqualTo(SyncLevel.BATCH);
    }

    @Test
    @DisplayName("未知数据类型应默认使用近实时同步")
    void routeSync_unknownType_shouldDefaultToNearRealTime() {
        SyncLevel level = router.routeSync("unknown_type", "create", Map.of());

        assertThat(level).isEqualTo(SyncLevel.NEAR_REAL_TIME);
    }

    @Test
    @DisplayName("forceRealTimeSync应强制使用实时同步")
    void forceRealTimeSync_shouldForceRealTime() {
        router.forceRealTimeSync("community_post", "create", Map.of("id", "123"));

        verify(realTimeDataSync).publishAfterCommit(eq("community_post"), eq("create"), any());
    }

    @Test
    @DisplayName("triggerNearRealTimeSync应记录日志")
    void triggerNearRealTimeSync_shouldLog() {
        router.triggerNearRealTimeSync("community_post", "create", Map.of("id", "123"));
    }

    @Test
    @DisplayName("getRouteDescription-实时同步应返回正确描述")
    void getRouteDescription_realTime_shouldReturnCorrectDescription() {
        String desc = router.getRouteDescription("order");

        assertThat(desc).contains("实时推送");
        assertThat(desc).contains("RabbitMQ");
    }

    @Test
    @DisplayName("getRouteDescription-近实时同步应返回正确描述")
    void getRouteDescription_nearRealTime_shouldReturnCorrectDescription() {
        String desc = router.getRouteDescription("community_post");

        assertThat(desc).contains("定时轮询");
        assertThat(desc).contains("5 min");
    }

    @Test
    @DisplayName("getRouteDescription-批量同步应返回正确描述")
    void getRouteDescription_batch_shouldReturnCorrectDescription() {
        String desc = router.getRouteDescription("report");

        assertThat(desc).contains("离线批处理");
        assertThat(desc).contains("hourly");
    }
}
