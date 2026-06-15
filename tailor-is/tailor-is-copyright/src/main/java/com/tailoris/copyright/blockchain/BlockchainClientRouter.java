package com.tailoris.copyright.blockchain;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 区块链客户端路由器
 * 任务编号: CR-001
 *
 * <p>按平台编码分发请求，支持多链并行/热切换/降级。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlockchainClientRouter {

    private final List<BlockchainClient> clients;
    private final Map<String, BlockchainClient> clientMap = new HashMap<>();

    @PostConstruct
    public void init() {
        for (BlockchainClient client : clients) {
            clientMap.put(client.platformCode(), client);
        }
        log.info("已加载 {} 个区块链客户端: {}", clientMap.size(), clientMap.keySet());
    }

    public BlockchainClient get(String platformCode) {
        if (platformCode == null) {
            return defaultClient();
        }
        return clientMap.getOrDefault(platformCode, defaultClient());
    }

    public BlockchainClient defaultClient() {
        BlockchainClient defaultClient = clientMap.get("ANTCHAIN");
        if (defaultClient == null && !clients.isEmpty()) {
            return clients.get(0);
        }
        return defaultClient;
    }

    public List<BlockchainClient> all() {
        return clients;
    }
}
