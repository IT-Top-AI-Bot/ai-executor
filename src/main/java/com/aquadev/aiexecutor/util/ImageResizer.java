package com.aquadev.aiexecutor.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Slf4j
@UtilityClass
public class ImageResizer {

    public static byte[] resizeIfNeeded(byte[] content, String mimeType, int maxDimension) {
        if (content == null || content.length == 0 || mimeType == null || !mimeType.startsWith("image/")) {
            return content;
        }

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(content));
            if (image == null) {
                return content;
            }

            int width = image.getWidth();
            int height = image.getHeight();

            if (width <= maxDimension && height <= maxDimension && content.length < 1024 * 1024) {
                return content;
            }

            double scale = Math.min((double) maxDimension / width, (double) maxDimension / height);
            int newWidth = (int) (width * Math.min(scale, 1.0));
            int newHeight = (int) (height * Math.min(scale, 1.0));

            log.info("Resizing image from {}x{} ({} bytes) to {}x{}", width, height, content.length, newWidth, newHeight);

            BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = resized.createGraphics();
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, newWidth, newHeight);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(image, 0, 0, newWidth, newHeight, null);
            g2d.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(resized, "jpg", baos);
            byte[] result = baos.toByteArray();
            log.info("Resized image size: {} bytes", result.length);
            return result;
        } catch (Exception e) {
            log.warn("Failed to resize image, using original: {}", e.getMessage());
            return content;
        }
    }
}
