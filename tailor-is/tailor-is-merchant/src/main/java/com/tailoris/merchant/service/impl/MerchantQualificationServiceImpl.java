package com.tailoris.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.merchant.dto.QualificationAuditRequest;
import com.tailoris.merchant.entity.MerchantQualification;
import com.tailoris.merchant.exception.MerchantBusinessException;
import com.tailoris.merchant.mapper.MerchantQualificationMapper;
import com.tailoris.merchant.service.MerchantQualificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.tailoris.merchant.constant.MerchantConstants.*;

/**
 * 商家资质审核服务实现 - MER-001.
 *
 * <h3>资质类型 qualification_type</h3>
 * <ul>
 *   <li>1: 营业执照（必填）</li>
 *   <li>2: 法人身份证（必填）</li>
 *   <li>3: 银行开户许可证</li>
 *   <li>4: 行业资质（如食品/医疗）</li>
 *   <li>5: 商标注册证</li>
 *   <li>6: 授权委托书</li>
 *   <li>7: 其他</li>
 * </ul>
 *
 * <h3>OCR 识别</h3>
 * <p>营业执照 OCR 识别统一社会信用代码（18位）+ 法人姓名 + 注册资本。
 * 通过正则表达式本地化识别（生产应接入阿里云 OCR API）。</p>
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantQualificationServiceImpl implements MerchantQualificationService {

    /** 统一社会信用代码正则：18位（数字+大写字母） */
    private static final Pattern CREDIT_CODE_PATTERN = Pattern.compile("[0-9A-HJ-NPQRTUWXY]{18}");

    /** 身份证号正则：18位（最后一位X） */
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("[1-9]\\d{5}(?:18|19|20)\\d{2}(?:0\\d|1[0-2])(?:[0-2]\\d|3[01])\\d{3}[0-9Xx]");

    private final MerchantQualificationMapper merchantQualificationMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MerchantQualification uploadQualification(Long merchantId, MerchantQualification qualification) {
        qualification.setMerchantId(merchantId);
        qualification.setAuditStatus(QUALIFICATION_AUDIT_STATUS_PENDING);

        // 资质文件基础校验
        validateQualification(qualification);

        // OCR 识别（生产环境接入真实OCR）
        if (qualification.getCertUrl() != null && !qualification.getCertUrl().isEmpty()) {
            try {
                OcrResult ocrResult = mockOcr(qualification);
                if (ocrResult != null) {
                    if (ocrResult.creditCode != null) {
                        qualification.setCertNo(ocrResult.creditCode);
                    }
                    if (ocrResult.legalName != null && qualification.getCertName() == null) {
                        qualification.setCertName(ocrResult.legalName);
                    }
                }
            } catch (Exception e) {
                log.warn("OCR识别失败，但不影响上传: {}", e.getMessage());
            }
        }

        merchantQualificationMapper.insert(qualification);
        log.info("资质上传成功, merchantId: {}, qualificationId: {}", merchantId, qualification.getId());
        return qualification;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditQualification(QualificationAuditRequest request) {
        MerchantQualification qualification = merchantQualificationMapper.selectById(request.getQualificationId());
        if (qualification == null) {
            throw new MerchantBusinessException("资质不存在");
        }

        qualification.setAuditStatus(request.getAuditStatus());
        qualification.setAuditRemark(request.getAuditRemark());
        qualification.setAuditTime(LocalDateTime.now());

        merchantQualificationMapper.updateById(qualification);
        log.info("资质审核完成, qualificationId: {}, auditStatus: {}",
                request.getQualificationId(), request.getAuditStatus());
    }

    @Override
    public List<MerchantQualification> listQualifications(Long merchantId) {
        LambdaQueryWrapper<MerchantQualification> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MerchantQualification::getMerchantId, merchantId)
                .orderByDesc(MerchantQualification::getCreateTime);
        return merchantQualificationMapper.selectList(queryWrapper);
    }

    /**
     * 校验必填项与文件类型.
     */
    private void validateQualification(MerchantQualification q) {
        if (q.getCertType() == null) {
            throw new MerchantBusinessException("资质类型不能为空");
        }
        if (q.getCertUrl() == null || q.getCertUrl().isEmpty()) {
            throw new MerchantBusinessException("资质文件不能为空");
        }
        // 校验文件URL合法性
        if (!q.getCertUrl().startsWith("https://") && !q.getCertUrl().startsWith("http://")
                && !q.getCertUrl().startsWith("/upload/")) {
            throw new MerchantBusinessException("资质文件URL不合法");
        }
        // 校验统一社会信用代码（如果已填）
        if (q.getCertNo() != null && !q.getCertNo().isEmpty()) {
            Matcher m = CREDIT_CODE_PATTERN.matcher(q.getCertNo());
            if (!m.matches()) {
                throw new MerchantBusinessException("统一社会信用代码格式不正确");
            }
        }
    }

    /**
     * 模拟OCR识别（生产应替换为真实OCR API）.
     *
     * <p>实际接入方式：</p>
     * <ul>
     *   <li>阿里云 OCR：https://market.aliyun.com/products/57124001/cmapi020020.html</li>
     *   <li>百度 OCR：https://cloud.baidu.com/product/ocr</li>
     *   <li>腾讯云 OCR：https://cloud.tencent.com/product/ocr</li>
     * </ul>
     */
    private OcrResult mockOcr(MerchantQualification q) {
        if (q.getCertType() == null) {
            return null;
        }
        // 实际生产应使用以下代码调用OCR服务：
        // OcrClient client = new OcrClient(accessKey, secret);
        // OcrResponse resp = client.recognizeBusinessLicense(q.getCertUrl());
        // return new OcrResult(resp.getCreditCode(), resp.getLegalName(), resp.getRegisterCapital());

        // 本地mock实现：从已有字段推断
        OcrResult result = new OcrResult();
        if (q.getCertNo() == null) {
            result.creditCode = null;  // 需要OCR提取
        } else {
            result.creditCode = q.getCertNo();
        }
        return result;
    }

    /**
     * 校验身份证号（公开方法供身份证类型资质使用）.
     */
    public boolean validateIdCard(String idCard) {
        if (idCard == null) {
            return false;
        }
        return ID_CARD_PATTERN.matcher(idCard).matches();
    }

    private static class OcrResult {
        String creditCode;
        String legalName;
        String registerCapital;
    }
}
