package com.tailoris.product.task;

import com.tailoris.product.service.ProductViewCountSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 商品浏览量同步定时任务 - 修复 B-M33
 *
 * <p>定期将Redis中累积的浏览量同步到数据库，确保数据最终一致性。</p>
 *
 * @author Tailor IS Team
 * @since 1.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductViewCountSyncTask {

    private final ProductViewCountSyncService syncService;

    /**
     * 每5分钟同步一次浏览量到数据库
     */
    @Scheduled(fixedRate = 5 * 60 * 1000L, initialDelay = 5 * 60 * 1000L)
    public void syncViewCount() {
        try {
            int count = syncService.syncAllViewCounts();
            if (count > 0) {
                log.info("商品浏览量定时同步完成: 更新{}条记录", count);
            }
        } catch (Exception e) {
            log.error("商品浏览量定时同步失败", e);
        }
    }
}
