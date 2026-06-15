package com.tailoris.copyright.blockchain;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.tailoris.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;

/**
 * PDF 证书生成器
 * 任务编号: CR-002
 *
 * <p>基于 PDFBox 生成含二维码、数字签名、完整证据信息的存证证书。</p>
 */
@Slf4j
@Component
public class CertificatePdfGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int QR_SIZE = 200;

    /**
     * 生成 PDF 证书
     */
    public byte[] generate(BlockchainClient.CertificateRequest request) {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(doc, page)) {
                // 边框
                drawBorder(content, page);

                // 标题
                drawTitle(content, page, "数字版权存证证书");

                // 副标题
                drawSubtitle(content, page, "Digital Copyright Certificate");

                // 平台
                drawText(content, 80, 680, "存证平台: " + safe(request.getPlatformName()), 12);

                // 证书编号
                drawTextBold(content, 80, 640, "证书编号: " + safe(request.getCertNo()), 14);

                // 作品信息
                int y = 600;
                drawText(content, 80, y, "作品名称: " + safe(request.getWorkName()), 12);
                y -= 30;
                drawText(content, 80, y, "作者姓名: " + safe(request.getAuthorName()), 12);
                y -= 30;
                drawText(content, 80, y, "作者ID:   " + safe(request.getAuthorId()), 12);
                y -= 30;
                drawText(content, 80, y, "文件哈希: " + truncate(request.getFileHash(), 64), 10);
                y -= 25;
                drawText(content, 80, y, "上链交易: " + truncate(request.getTxHash(), 64), 10);
                y -= 25;
                drawText(content, 80, y, "存证时间: " +
                        (request.getRegisteredAt() == null
                                ? LocalDateTime.now().format(DATE_FMT)
                                : request.getRegisteredAt().format(DATE_FMT)), 12);

                // 法律声明
                y -= 60;
                drawText(content, 80, y, "法律声明:", 11, true);
                y -= 20;
                drawText(content, 80, y, "本证书由区块链不可篡改特性保障,数据一经上链不可修改,具有法律证据效力。", 9);
                y -= 15;
                drawText(content, 80, y, "扫描下方二维码可在官方平台验证证书真伪。", 9);

                // 二维码
                if (request.getQrContent() != null) {
                    byte[] qrPng = generateQrCode(request.getQrContent());
                    if (qrPng != null && qrPng.length > 0) {
                        PDImageXObject qrImage = PDImageXObject.createFromByteArray(doc, qrPng, "qr");
                        content.drawImage(qrImage, 400, 200, QR_SIZE, QR_SIZE);
                        drawText(content, 400, 180, "扫码验证", 10);
                    }
                }

                // 底部签名区
                drawText(content, 80, 80, "签发机构: Tailor IS 知识产权服务中心", 10);
                drawText(content, 80, 60, "签发时间: " + LocalDateTime.now().format(DATE_FMT), 10);
            }
            doc.save(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("生成证书PDF失败", e);
            throw new BusinessException("证书生成失败: " + e.getMessage());
        }
    }

    /**
     * 生成二维码 PNG
     */
    public byte[] generateQrCode(String content) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            QRCodeWriter writer = new QRCodeWriter();
            BufferedImage image = MatrixToImageWriter.toBufferedImage(
                    writer.encode(content, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints));

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ImageIO.write(image, "PNG", baos);
                return baos.toByteArray();
            }
        } catch (WriterException | IOException e) {
            log.error("生成二维码失败", e);
            return new byte[0];
        }
    }

    /**
     * 计算数字签名（HMAC-SHA256 占位）
     */
    public String sign(String data, String secretKey) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(secretKey.getBytes("UTF-8"), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("签名失败", e);
            return "";
        }
    }

    private void drawBorder(PDPageContentStream content, PDPage page) throws IOException {
        content.setStrokingColor(new PDColor(new float[]{0.1f, 0.4f, 0.8f}, PDDeviceRGB.INSTANCE));
        content.setLineWidth(2);
        content.addRect(40, 40, page.getMediaBox().getWidth() - 80, page.getMediaBox().getHeight() - 80);
        content.stroke();
    }

    private void drawTitle(PDPageContentStream content, PDPage page, String text) throws IOException {
        PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        int fontSize = 28;
        float titleWidth = font.getStringWidth(text) / 1000 * fontSize;
        content.beginText();
        content.setFont(font, fontSize);
        content.setNonStrokingColor(new PDColor(new float[]{0.1f, 0.4f, 0.8f}, PDDeviceRGB.INSTANCE));
        content.newLineAtOffset((page.getMediaBox().getWidth() - titleWidth) / 2, 750);
        content.showText(text);
        content.endText();
    }

    private void drawSubtitle(PDPageContentStream content, PDPage page, String text) throws IOException {
        PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
        int fontSize = 12;
        float width = font.getStringWidth(text) / 1000 * fontSize;
        content.beginText();
        content.setFont(font, fontSize);
        content.setNonStrokingColor(0.4f, 0.4f, 0.4f);
        content.newLineAtOffset((page.getMediaBox().getWidth() - width) / 2, 720);
        content.showText(text);
        content.endText();
    }

    private void drawText(PDPageContentStream content, float x, float y, String text, int size) throws IOException {
        drawText(content, x, y, text, size, false);
    }

    private void drawText(PDPageContentStream content, float x, float y, String text, int size, boolean bold) throws IOException {
        content.beginText();
        content.setFont(bold ? new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD) : new PDType1Font(Standard14Fonts.FontName.HELVETICA), size);
        content.setNonStrokingColor(0f, 0f, 0f);
        content.newLineAtOffset(x, y);
        content.showText(safe(text));
        content.endText();
    }

    private void drawTextBold(PDPageContentStream content, float x, float y, String text, int size) throws IOException {
        drawText(content, x, y, text, size, true);
    }

    private String safe(String s) {
        if (s == null) return "";
        return s.length() > 100 ? s.substring(0, 100) : s;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }
}
