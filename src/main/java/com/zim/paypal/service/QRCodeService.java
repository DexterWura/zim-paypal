package com.zim.paypal.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Service for QR code generation
 * 
 * @author dexterwura
 */
@Service
@Slf4j
public class QRCodeService {

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private static final int QR_CODE_WIDTH = 300;
    private static final int QR_CODE_HEIGHT = 300;

    /**
     * Generate QR code image as byte array
     * 
     * @param text Text to encode in QR code
     * @return Byte array of PNG image
     */
    public byte[] generateQRCodeImage(String text) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, QR_CODE_WIDTH, QR_CODE_HEIGHT);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] pngData = pngOutputStream.toByteArray();

            log.debug("QR code generated for text: {}", text);
            return pngData;
        } catch (WriterException | IOException e) {
            log.error("Error generating QR code: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate QR code: " + e.getMessage(), e);
        }
    }

    /**
     * Generate QR code for payment link
     * 
     * @param linkCode Payment link code
     * @return Byte array of PNG image
     */
    public byte[] generatePaymentLinkQRCode(String linkCode) {
        String paymentUrl = baseUrl + "/pay/" + linkCode;
        return generateQRCodeImage(paymentUrl);
    }

    /**
     * Get payment URL for a link code
     * 
     * @param linkCode Payment link code
     * @return Full payment URL
     */
    public String getPaymentUrl(String linkCode) {
        return baseUrl + "/pay/" + linkCode;
    }
}

