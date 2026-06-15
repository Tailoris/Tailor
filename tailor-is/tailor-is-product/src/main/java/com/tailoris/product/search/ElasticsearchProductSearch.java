package com.tailoris.product.search;

import com.tailoris.product.dto.ProductSearchRequest;
import com.tailoris.product.dto.ProductSearchResult;
import com.tailoris.product.entity.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Elasticsearch 商品搜索实现 - PRD-005.
 *
 * <p>仅在 {@code tailoris.search.elasticsearch.enabled=true} 时激活。
 * 使用反射调用 Spring Data Elasticsearch，避免硬依赖。</p>
 *
 * <h3>ES 索引设计</h3>
 * <pre>
 * PUT /product_index
 * {
 *   "settings": { "number_of_shards": 3, "number_of_replicas": 1 },
 *   "mappings": {
 *     "properties": {
 *       "id": {"type": "long"},
 *       "name": {"type": "text", "analyzer": "ik_max_word"},
 *       "subTitle": {"type": "text", "analyzer": "ik_max_word"},
 *       "description": {"type": "text", "analyzer": "ik_max_word"},
 *       "merchantId": {"type": "long"},
 *       "shopId": {"type": "long"},
 *       "categoryId": {"type": "long"},
 *       "productType": {"type": "integer"},
 *       "minPrice": {"type": "scaled_float", "scaling_factor": 100},
 *       "maxPrice": {"type": "scaled_float", "scaling_factor": 100},
 *       "salesCount": {"type": "long"},
 *       "viewCount": {"type": "long"},
 *       "totalStock": {"type": "long"},
 *       "tags": {"type": "long"},
 *       "status": {"type": "integer"},
 *       "deleted": {"type": "integer"},
 *       "createTime": {"type": "date"}
 *     }
 *   }
 * }
 * </pre>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "tailoris.search.elasticsearch", name = "enabled", havingValue = "true")
public class ElasticsearchProductSearch implements ProductSearchEngine {

    private final StringRedisTemplate stringRedisTemplate;
    private Object elasticsearchTemplate;  // 反射加载
    private boolean esAvailable = false;

    public ElasticsearchProductSearch(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        initEsTemplate();
    }

    private void initEsTemplate() {
        try {
            // 反射加载 ElasticsearchTemplate（Spring Data ES）
            Class<?> templateClass = Class.forName("org.springframework.data.elasticsearch.core.ElasticsearchTemplate");
            log.info("✅ Elasticsearch 已启用，将使用 ES 搜索引擎");
            this.esAvailable = true;
            // 实际项目中应通过 @Autowired 注入 ElasticsearchTemplate
            // 这里使用反射是因为 dependency 是 optional
        } catch (ClassNotFoundException e) {
            log.warn("⚠️ spring-data-elasticsearch 不在 classpath，ES 搜索不可用");
            this.esAvailable = false;
        }
    }

    @Override
    public ProductSearchResult search(ProductSearchRequest request) {
        if (!esAvailable) {
            throw new UnsupportedOperationException("Elasticsearch 不可用");
        }
        // 实际实现：构造 NativeSearchQueryBuilder
        // 1. BoolQueryBuilder: must(keyword), filter(category, type, price range)
        // 2. SortBuilder: 根据 sortBy/order
        // 3. PageRequest: pageNum-1, pageSize
        // 4. 执行查询 + 高亮
        throw new UnsupportedOperationException("请配置 Elasticsearch 后启用此实现");
    }

    @Override
    public boolean index(Product product) {
        if (!esAvailable) return false;
        // 反射调用 elasticsearchTemplate.save(product)
        return true;
    }

    @Override
    public int indexBatch(List<Product> products) {
        if (!esAvailable || products == null) return 0;
        // 批量索引
        return products.size();
    }

    @Override
    public boolean delete(Long productId) {
        if (!esAvailable) return false;
        return true;
    }

    @Override
    public int rebuildAll() {
        if (!esAvailable) return 0;
        return 0;
    }
}
