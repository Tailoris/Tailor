package com.tailoris.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DataSyncStrategyConfig 单元测试
 */
@DisplayName("DataSyncStrategyConfig 单元测试")
class DataSyncStrategyConfigTest {

    @Test
    @DisplayName("订单应为核心数据-实时同步")
    void getSyncLevel_order_shouldBeRealTime() {
        DataSyncStrategyConfig.SyncLevel level = DataSyncStrategyConfig.getSyncLevel("order");

        assertThat(level).isEqualTo(DataSyncStrategyConfig.SyncLevel.REAL_TIME);
    }

    @Test
    @DisplayName("商品应为核心数据-实时同步")
    void getSyncLevel_product_shouldBeRealTime() {
        assertThat(DataSyncStrategyConfig.getSyncLevel("product"))
                .isEqualTo(DataSyncStrategyConfig.SyncLevel.REAL_TIME);
    }

    @Test
    @DisplayName("社区帖子应为非核心数据-近实时同步")
    void getSyncLevel_communityPost_shouldBeNearRealTime() {
        assertThat(DataSyncStrategyConfig.getSyncLevel("community_post"))
                .isEqualTo(DataSyncStrategyConfig.SyncLevel.NEAR_REAL_TIME);
    }

    @Test
    @DisplayName("报表应为批量同步")
    void getSyncLevel_report_shouldBeBatch() {
        assertThat(DataSyncStrategyConfig.getSyncLevel("report"))
                .isEqualTo(DataSyncStrategyConfig.SyncLevel.BATCH);
    }

    @Test
    @DisplayName("未知类型应默认为近实时同步")
    void getSyncLevel_unknown_shouldDefaultToNearRealTime() {
        assertThat(DataSyncStrategyConfig.getSyncLevel("unknown_type"))
                .isEqualTo(DataSyncStrategyConfig.SyncLevel.NEAR_REAL_TIME);
    }

    @Test
    @DisplayName("null类型应默认为近实时同步")
    void getSyncLevel_null_shouldDefaultToNearRealTime() {
        assertThat(DataSyncStrategyConfig.getSyncLevel(null))
                .isEqualTo(DataSyncStrategyConfig.SyncLevel.NEAR_REAL_TIME);
    }

    @Test
    @DisplayName("空字符串应默认为近实时同步")
    void getSyncLevel_empty_shouldDefaultToNearRealTime() {
        assertThat(DataSyncStrategyConfig.getSyncLevel(""))
                .isEqualTo(DataSyncStrategyConfig.SyncLevel.NEAR_REAL_TIME);
    }

    @Test
    @DisplayName("isCoreData-核心数据应返回true")
    void isCoreData_coreData_shouldReturnTrue() {
        assertThat(DataSyncStrategyConfig.isCoreData("order")).isTrue();
        assertThat(DataSyncStrategyConfig.isCoreData("product")).isTrue();
        assertThat(DataSyncStrategyConfig.isCoreData("payment")).isTrue();
    }

    @Test
    @DisplayName("isCoreData-非核心数据应返回false")
    void isCoreData_nonCoreData_shouldReturnFalse() {
        assertThat(DataSyncStrategyConfig.isCoreData("community_post")).isFalse();
        assertThat(DataSyncStrategyConfig.isCoreData("report")).isFalse();
    }

    @Test
    @DisplayName("getCoreDataTypes应返回非空集合")
    void getCoreDataTypes_shouldReturnNonEmpty() {
        assertThat(DataSyncStrategyConfig.getCoreDataTypes())
                .isNotEmpty()
                .contains("order", "product", "user");
    }

    @Test
    @DisplayName("getNonCoreDataTypes应返回非空集合")
    void getNonCoreDataTypes_shouldReturnNonEmpty() {
        assertThat(DataSyncStrategyConfig.getNonCoreDataTypes())
                .isNotEmpty()
                .contains("community_post", "academy_course");
    }

    @Test
    @DisplayName("SyncLevel枚举应有正确的label")
    void syncLevel_shouldHaveCorrectLabel() {
        assertThat(DataSyncStrategyConfig.SyncLevel.REAL_TIME.getLabel()).isEqualTo("实时同步");
        assertThat(DataSyncStrategyConfig.SyncLevel.NEAR_REAL_TIME.getLabel()).isEqualTo("近实时同步");
        assertThat(DataSyncStrategyConfig.SyncLevel.BATCH.getLabel()).isEqualTo("批量同步");
    }

    @Test
    @DisplayName("SyncLevel枚举应有正确的默认间隔")
    void syncLevel_shouldHaveCorrectDefaultInterval() {
        assertThat(DataSyncStrategyConfig.SyncLevel.REAL_TIME.getDefaultIntervalSeconds()).isEqualTo(0);
        assertThat(DataSyncStrategyConfig.SyncLevel.NEAR_REAL_TIME.getDefaultIntervalSeconds()).isEqualTo(300);
        assertThat(DataSyncStrategyConfig.SyncLevel.BATCH.getDefaultIntervalSeconds()).isEqualTo(3600);
    }
}
