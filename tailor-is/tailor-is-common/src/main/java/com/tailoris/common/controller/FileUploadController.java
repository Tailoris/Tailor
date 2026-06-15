package com.tailoris.common.controller;

import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.oss.FileUploadService;
import com.tailoris.common.oss.UploadResult;
import com.tailoris.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 通用文件上传 Controller - PRD-003.
 *
 * <p>提供多业务场景的文件上传能力：</p>
 * <ul>
 *   <li>/api/upload/{bizType}  单文件上传</li>
 *   <li>/api/upload/{bizType}/batch  批量上传</li>
 *   <li>/api/upload/{bizType}/delete  删除文件</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "文件上传", description = "通用文件上传（OSS/本地）")
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService fileUploadService;

    @Operation(summary = "上传文件", description = "bizType: product/review/avatar/identity/...")
    @PostMapping("/{bizType}")
    public Result<UploadResult> upload(
            @Parameter(description = "业务类型") @org.springframework.web.bind.annotation.PathVariable String bizType,
            @RequestPart("file") MultipartFile file) throws IOException {
        validateBizType(bizType);
        UploadResult result = fileUploadService.upload(file, bizType);
        log.info("文件上传: bizType={}, key={}, size={}",
                bizType, result.getObjectKey(), result.getSize());
        return Result.success(result);
    }

    @Operation(summary = "批量上传文件")
    @PostMapping("/{bizType}/batch")
    public Result<List<UploadResult>> uploadBatch(
            @PathVariable String bizType,
            @RequestPart("files") List<MultipartFile> files) throws IOException {
        validateBizType(bizType);
        List<UploadResult> results = fileUploadService.uploadBatch(files, bizType);
        return Result.success(results);
    }

    @Operation(summary = "删除文件")
    @PostMapping("/delete")
    public Result<Boolean> delete(@RequestParam String key) {
        boolean ok = fileUploadService.delete(key);
        return Result.success(ok);
    }

    private void validateBizType(String bizType) {
        // 白名单校验，防止恶意传参
        java.util.Set<String> allowed = java.util.Set.of("product", "review", "avatar",
                "identity", "shop", "common");
        if (!allowed.contains(bizType)) {
            throw new BusinessException("不支持的业务类型: " + bizType);
        }
    }
}
