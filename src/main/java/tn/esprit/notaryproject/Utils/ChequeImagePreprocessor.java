package tn.esprit.notaryproject.Utils;
import org.springframework.stereotype.Component;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
public class ChequeImagePreprocessor {

    public byte[] preprocessChequeImage(byte[] imageData) throws IOException {
        System.out.println("=== DÉBUT PRÉTRAITEMENT IMAGE ===");

        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageData));
        System.out.println("Image originale: " + originalImage.getWidth() + "x" + originalImage.getHeight());

        // Pipeline de prétraitement optimisé pour les chèques
        BufferedImage processed = originalImage;

        // 1. Redimensionnement pour améliorer la résolution
        processed = resizeImage(processed, processed.getWidth() * 2, processed.getHeight() * 2);
        System.out.println("Après redimensionnement: " + processed.getWidth() + "x" + processed.getHeight());

        // 2. Correction de l'orientation
        processed = autoDeskew(processed);

        // 3. Conversion en niveaux de gris
        processed = convertToGrayScale(processed);

        // 4. Renforcement du contraste spécifique aux chèques
        processed = enhanceChequeContrast(processed);

        // 5. Réduction du bruit
        processed = reduceNoise(processed);

        // 6. Seuillage adaptatif
        processed = adaptiveThreshold(processed);

        // 7. Amélioration des caractères
        processed = enhanceText(processed);

        System.out.println("=== FIN PRÉTRAITEMENT IMAGE ===");

        return bufferedImageToByteArray(processed);
    }

    private BufferedImage resizeImage(BufferedImage original, int newWidth, int newHeight) {
        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.drawImage(original, 0, 0, newWidth, newHeight, null);
        g.dispose();

        return resized;
    }

    private BufferedImage autoDeskew(BufferedImage image) {
        // Détection simple de l'orientation pour les chèques
        // Les chèques sont généralement en orientation paysage
        if (image.getHeight() > image.getWidth()) {
            // Rotation 90 degrés si en portrait
            double rotationRequired = Math.toRadians(90);
            double locationX = image.getWidth() / 2.0;
            double locationY = image.getHeight() / 2.0;
            AffineTransform tx = AffineTransform.getRotateInstance(rotationRequired, locationX, locationY);
            AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BICUBIC);
            return op.filter(image, null);
        }
        return image;
    }

    private BufferedImage convertToGrayScale(BufferedImage image) {
        BufferedImage gray = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return gray;
    }

    private BufferedImage enhanceChequeContrast(BufferedImage image) {
        // Amélioration du contraste spécifique aux documents financiers
        BufferedImage contrasted = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());

        int[] lut = new int[256];
        // Courbe de contraste S-shaped pour préserver les détails
        for (int i = 0; i < 256; i++) {
            if (i < 50) lut[i] = 0;
            else if (i > 200) lut[i] = 255;
            else lut[i] = (int) (255.0 / (200 - 50) * (i - 50));
        }

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int rgb = image.getRGB(x, y);
                int gray = rgb & 0xFF;
                int newGray = lut[gray];
                int newRgb = (newGray << 16) | (newGray << 8) | newGray;
                contrasted.setRGB(x, y, newRgb);
            }
        }
        return contrasted;
    }

    private BufferedImage reduceNoise(BufferedImage image) {
        // Filtre médian 3x3 pour réduire le bruit
        BufferedImage denoised = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());

        for (int x = 1; x < image.getWidth() - 1; x++) {
            for (int y = 1; y < image.getHeight() - 1; y++) {
                int[] window = new int[9];
                int index = 0;

                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        window[index++] = image.getRGB(x + dx, y + dy) & 0xFF;
                    }
                }

                java.util.Arrays.sort(window);
                int median = window[4];
                int newRgb = (median << 16) | (median << 8) | median;
                denoised.setRGB(x, y, newRgb);
            }
        }
        return denoised;
    }

    private BufferedImage adaptiveThreshold(BufferedImage image) {
        // Seuillage adaptatif pour gérer les variations d'éclairage
        BufferedImage binary = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);

        int blockSize = 15; // Taille du voisinage
        int constant = 10;  // Constante à soustraire

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int gray = image.getRGB(x, y) & 0xFF;

                // Calcul de la moyenne locale
                int sum = 0;
                int count = 0;

                for (int dx = -blockSize/2; dx <= blockSize/2; dx++) {
                    for (int dy = -blockSize/2; dy <= blockSize/2; dy++) {
                        int nx = x + dx;
                        int ny = y + dy;

                        if (nx >= 0 && nx < image.getWidth() && ny >= 0 && ny < image.getHeight()) {
                            sum += image.getRGB(nx, ny) & 0xFF;
                            count++;
                        }
                    }
                }

                int mean = sum / count;
                int threshold = mean - constant;

                int newValue = (gray > threshold) ? 255 : 0;
                int newRgb = (newValue << 16) | (newValue << 8) | newValue;
                binary.setRGB(x, y, newRgb);
            }
        }
        return binary;
    }

    private BufferedImage enhanceText(BufferedImage image) {
        // Amélioration spécifique du texte
        BufferedImage enhanced = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());

        // Filtre de netteté
        float[] sharpenMatrix = {
                0, -1, 0,
                -1, 5, -1,
                0, -1, 0
        };

        Kernel kernel = new Kernel(3, 3, sharpenMatrix);
        ConvolveOp op = new ConvolveOp(kernel);
        return op.filter(image, enhanced);
    }

    private byte[] bufferedImageToByteArray(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    // Méthode utilitaire pour sauvegarder l'image traitée (débogage)
    public void saveProcessedImage(byte[] imageData, String filename) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
        ImageIO.write(image, "png", new java.io.File(filename));
        System.out.println("Image sauvegardée: " + filename);
    }
}