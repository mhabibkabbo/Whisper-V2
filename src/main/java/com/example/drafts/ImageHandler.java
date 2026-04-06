package com.example.drafts;

import javafx.scene.image.Image;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.*;

public class ImageHandler {

    public static Image getProfileImage(int userId) {

        byte[] imageBytes = RemoteApi.getProfilePicture(userId);

        if (imageBytes != null && imageBytes.length > 0) {
            return new Image(new ByteArrayInputStream(imageBytes));
        } else {
            return new Image(
                    ImageHandler.class.getResourceAsStream("/com/example/drafts/icons/user.png")
            );
        }
    }

    public static byte[] processProfileImage(File file) throws IOException {

        BufferedImage original = ImageIO.read(file);

        int targetSize = 256;

        java.awt.Image scaled = original.getScaledInstance(
                targetSize,
                targetSize,
                java.awt.Image.SCALE_SMOOTH
        );

        BufferedImage resized = new BufferedImage(
                targetSize,
                targetSize,
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(scaled, 0, 0, null);
        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();

        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.7f);

        ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
        writer.setOutput(ios);
        writer.write(null, new IIOImage(resized, null, null), param);

        writer.dispose();
        ios.close();

        return baos.toByteArray();
    }
}