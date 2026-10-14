package de.lukasbreuer.clarity.api.business.invoice.reading;

import lombok.RequiredArgsConstructor;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * A PNG of black text on white, large enough for Tesseract to read.
 */
@RequiredArgsConstructor(staticName = "of")
final class ScannedInvoice {
  private final List<String> lines;

  byte[] png() throws IOException {
    var image = new BufferedImage(1240, 700, BufferedImage.TYPE_INT_RGB);
    var graphics = image.createGraphics();
    draw(graphics, image);
    graphics.dispose();
    var png = new ByteArrayOutputStream();
    ImageIO.write(image, "png", png);
    return png.toByteArray();
  }

  private void draw(Graphics2D graphics, BufferedImage image) {
    graphics.setColor(Color.WHITE);
    graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
    graphics.setColor(Color.BLACK);
    graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
      RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 36));
    for (var index = 0; index < lines.size(); index++) {
      graphics.drawString(lines.get(index), 80, 120 + index * 80);
    }
  }
}