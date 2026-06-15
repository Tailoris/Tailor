package com.tailoris.copyright.blockchain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("BlockchainClientRouter 单元测试")
@ExtendWith(MockitoExtension.class)
class BlockchainClientRouterTest {

    @Mock
    private BlockchainClient antChainClient;
    @Mock
    private BlockchainClient zhiXinClient;

    private BlockchainClientRouter router;

    @BeforeEach
    void setUp() {
        when(antChainClient.platformCode()).thenReturn("ANTCHAIN");
        when(zhiXinClient.platformCode()).thenReturn("ZHIXIN");

        List<BlockchainClient> clients = Arrays.asList(antChainClient, zhiXinClient);
        router = new BlockchainClientRouter(clients);
        router.init();
    }

    @Test
    @DisplayName("获取默认客户端 - ANTCHAIN")
    void testDefaultClient() {
        BlockchainClient client = router.defaultClient();
        assertNotNull(client);
        assertEquals("ANTCHAIN", client.platformCode());
    }

    @Test
    @DisplayName("根据平台代码获取客户端 - ANTCHAIN")
    void testGet_AntChain() {
        BlockchainClient client = router.get("ANTCHAIN");
        assertNotNull(client);
        assertEquals("ANTCHAIN", client.platformCode());
    }

    @Test
    @DisplayName("根据平台代码获取客户端 - ZHIXIN")
    void testGet_ZhiXin() {
        BlockchainClient client = router.get("ZHIXIN");
        assertNotNull(client);
        assertEquals("ZHIXIN", client.platformCode());
    }

    @Test
    @DisplayName("根据平台代码获取客户端 - 不存在返回默认")
    void testGet_NotExist_ReturnDefault() {
        BlockchainClient client = router.get("UNKNOWN");
        assertNotNull(client);
        assertEquals("ANTCHAIN", client.platformCode());
    }

    @Test
    @DisplayName("根据平台代码获取客户端 - null返回默认")
    void testGet_Null_ReturnDefault() {
        BlockchainClient client = router.get(null);
        assertNotNull(client);
        assertEquals("ANTCHAIN", client.platformCode());
    }

    @Test
    @DisplayName("获取所有客户端")
    void testAll() {
        List<BlockchainClient> clients = router.all();
        assertNotNull(clients);
        assertEquals(2, clients.size());
    }

    @Test
    @DisplayName("空客户端列表 - 默认客户端为null")
    void testEmptyClients() {
        BlockchainClientRouter emptyRouter = new BlockchainClientRouter(Collections.emptyList());
        emptyRouter.init();
        BlockchainClient client = emptyRouter.defaultClient();
        assertNull(client);
    }
}
