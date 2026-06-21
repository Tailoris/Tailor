package com.tailoris.copyright.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import com.tailoris.copyright.blockchain.BlockchainService;
import com.tailoris.copyright.dto.AuthorizationRequest;
import com.tailoris.copyright.dto.CopyrightRegisterRequest;
import com.tailoris.copyright.dto.CopyrightRegisterResponse;
import com.tailoris.copyright.dto.CopyrightVerifyRequest;
import com.tailoris.copyright.dto.CopyrightVerifyResponse;
import com.tailoris.copyright.entity.CopyrightAuthorization;
import com.tailoris.copyright.entity.CopyrightRecord;
import com.tailoris.copyright.service.CopyrightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@SaCheckLogin
@Tag(name = "版权管理", description = "版权登记、验证、证书生成、授权等接口")
@RestController
@RequestMapping("/api/v1/copyright")
@RequiredArgsConstructor
public class CopyrightController {

    private final CopyrightService copyrightService;
    private final BlockchainService blockchainService;

    @Operation(summary = "登记版权", description = "提交作品进行版权登记和区块链存证")
    @PostMapping("/register")
    public Result<CopyrightRecord> registerCopyright(@Valid @RequestBody CopyrightRegisterRequest request) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        CopyrightRecord record = copyrightService.registerCopyright(userId, request);
        return Result.success(record);
    }

    @Operation(summary = "登记版权（哈希计算）", description = "提交作品进行版权登记，自动计算SHA-256文件哈希")
    @PostMapping("/register-hash")
    public Result<CopyrightRegisterResponse> registerWithHash(@Valid @RequestBody CopyrightRegisterRequest request) {
        CopyrightRegisterResponse response = copyrightService.register(request);
        return Result.success(response);
    }

    @Operation(summary = "生成版权证书", description = "为已存证的版权生成证书")
    @GetMapping("/certificate")
    public Result<String> generateCertificate(@RequestParam Long copyrightId) {
        return Result.success(copyrightService.generateCertificate(copyrightId));
    }

    @Operation(summary = "验证版权", description = "验证版权存证信息")
    @GetMapping("/verify")
    public Result<String> verifyCopyright(@RequestParam Long copyrightId) {
        return Result.success(copyrightService.verifyCopyright(copyrightId));
    }

    @Operation(summary = "验证版权（哈希比对）", description = "通过文件哈希比对验证版权存证信息")
    @PostMapping("/verify-hash")
    public Result<CopyrightVerifyResponse> verifyByHash(@Valid @RequestBody CopyrightVerifyRequest request) {
        CopyrightVerifyResponse response = copyrightService.verify(request);
        return Result.success(response);
    }

    @Operation(summary = "查询我的版权列表", description = "分页查询用户的版权登记记录")
    @GetMapping("/list")
    public Result<PageResponse<CopyrightRecord>> listCopyrights(PageRequest pageRequest, @RequestParam(required = false) Integer status) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(copyrightService.listCopyrights(userId, pageRequest, status));
    }

    @Operation(summary = "查询版权详情", description = "根据ID查询版权登记详情")
    @GetMapping("/detail")
    public Result<CopyrightRecord> getCopyrightDetail(@RequestParam Long copyrightId) {
        return Result.success(copyrightService.getCopyrightDetail(copyrightId));
    }

    @Operation(summary = "版权授权", description = "为版权创建授权记录")
    @PostMapping("/authorize")
    public Result<CopyrightAuthorization> createAuthorization(@Valid @RequestBody AuthorizationRequest request) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        CopyrightAuthorization authorization = copyrightService.authorize(userId, request);
        return Result.success(authorization);
    }

    // ==================== 区块链存证操作（BlockchainService）====================

    @Operation(summary = "区块链直接存证", description = "直接通过区块链服务进行版权存证，返回交易哈希和区块信息")
    @PostMapping("/blockchain/register")
    public Result<Map<String, Object>> blockchainRegister(@Valid @RequestBody CopyrightRegisterRequest request) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();

        BlockchainService.CopyrightData data = new BlockchainService.CopyrightData();
        data.setBizId(String.valueOf(userId));
        data.setWorkName(request.getWorkName());
        data.setAuthorId(String.valueOf(userId));
        data.setAuthorName(request.getAuthorRealName());
        data.setFileHash(request.getFileHash());
        data.setFileType(request.getFileType());
        data.setFileSize(request.getFileSize());
        data.setCreationTimestamp(request.getCreationEndTime() != null
                ? request.getCreationEndTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                : System.currentTimeMillis());
        data.setMetadata(request.getDescription());

        Map<String, Object> result = blockchainService.registerCopyright(data);
        return Result.success(result);
    }

    @Operation(summary = "区块链批量存证", description = "批量提交版权进行区块链存证，使用Merkle树减少链上交易数量")
    @PostMapping("/blockchain/batch-register")
    public Result<Map<String, Object>> blockchainBatchRegister(
            @Valid @RequestBody List<CopyrightRegisterRequest> requests) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();

        List<BlockchainService.CopyrightData> dataList = requests.stream().map(request -> {
            BlockchainService.CopyrightData data = new BlockchainService.CopyrightData();
            data.setBizId(String.valueOf(userId));
            data.setWorkName(request.getWorkName());
            data.setAuthorId(String.valueOf(userId));
            data.setAuthorName(request.getAuthorRealName());
            data.setFileHash(request.getFileHash());
            data.setFileType(request.getFileType());
            data.setFileSize(request.getFileSize());
            data.setCreationTimestamp(request.getCreationEndTime() != null
                    ? request.getCreationEndTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    : System.currentTimeMillis());
            data.setMetadata(request.getDescription());
            return data;
        }).toList();

        Map<String, Object> result = blockchainService.batchRegister(dataList);
        return Result.success(result);
    }

    @Operation(summary = "区块链验证版权", description = "通过文件哈希在区块链上验证版权存证信息")
    @GetMapping("/blockchain/verify")
    public Result<Map<String, Object>> blockchainVerify(
            @Parameter(description = "文件SHA-256哈希") @RequestParam String hash) {
        Map<String, Object> result = blockchainService.verifyCopyright(hash);
        return Result.success(result);
    }

    @Operation(summary = "区块链证书查询", description = "通过交易哈希查询区块链存证证书信息")
    @GetMapping("/blockchain/certificate/{txHash}")
    public Result<Map<String, Object>> blockchainCertificate(
            @Parameter(description = "区块链交易哈希") @PathVariable String txHash) {
        Map<String, Object> result = blockchainService.queryCertificate(txHash);
        return Result.success(result);
    }

    @Operation(summary = "区块链健康检查", description = "检查区块链服务连接状态")
    @GetMapping("/blockchain/health")
    public Result<Map<String, String>> blockchainHealth() {
        boolean healthy = blockchainService.isHealthy();
        String platform = blockchainService.getPlatformName();
        Map<String, String> result = Map.of(
                "healthy", String.valueOf(healthy),
                "platform", platform
        );
        return Result.success(result);
    }
}
