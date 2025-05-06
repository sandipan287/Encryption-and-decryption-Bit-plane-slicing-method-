//using bitplane slicing no image generates
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;

public class ImageEncryption {
    private static final String[] DNA_RULES = {
        "ACTG", "ACGT", "AGCT", "AGTC", "ATCG", "ATGC",
        "CAGT", "CATG", "CGAT", "CGTA", "CTAG", "CTGA",
        "GACT", "GATC", "GCAT", "GCTA", "GTAC", "GTCA",
        "TACG", "TAGC", "TCAG", "TCGA", "TGAC", "TGCA"
    };

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("IMAGE ENCRYPTION");

        try {
            System.out.print("Input image path: ");
            File inputFile = new File(sc.nextLine());
            BufferedImage inputImg = ImageIO.read(inputFile);

            System.out.print("Output path for encrypted image: ");
            String encryptedOutputPath = sc.nextLine();

            System.out.print("Encryption key: ");
            long key = sc.nextLong();
            sc.nextLine(); // consume newline

            BufferedImage encryptedImg = encryptImage(inputImg, key);
            ImageIO.write(encryptedImg, "PNG", new File(encryptedOutputPath));
            System.out.println("Encryption successful!");

            System.out.print("Output path for encrypted bit planes: ");
            String encryptedBitPlanePath = sc.nextLine();
            displayBitPlanes(encryptedImg, encryptedBitPlanePath);

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Encryption failed: " + e.getMessage());
        } finally {
            sc.close();
        }
    }

    private static BufferedImage encryptImage(BufferedImage img, long key) {
        int width = img.getWidth();
        int height = img.getHeight();
        BufferedImage encryptedImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int prevPixel = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                long pixelKey = key ^ (x << 32) ^ (y << 16);

                r = transformPixel(r, pixelKey, prevPixel);
                g = transformPixel(g, pixelKey + 1, r);
                b = transformPixel(b, pixelKey + 2, g);

                r = dnaSubstitute(r, pixelKey);
                g = dnaSubstitute(g, pixelKey + 1);
                b = dnaSubstitute(b, pixelKey + 2);

                prevPixel = b;
                encryptedImg.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }

        return scrambleImage(encryptedImg, key);
    }

    private static int transformPixel(int pixel, long key, int prevPixel) {
        pixel ^= prevPixel;
        pixel = ((pixel << 1) | (pixel >>> 7)) & 0xFF;
        pixel ^= (int) (key & 0xFF);
        return pixel;
    }

    private static int dnaSubstitute(int pixel, long key) {
        int ruleIdx = (int) ((key >> 8) % DNA_RULES.length);
        String rule = DNA_RULES[ruleIdx];
        int rotate = (int) (key & 0b11);
        rule = rule.substring(rotate) + rule.substring(0, rotate);

        StringBuilder dna = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            int val = (pixel >> (6 - i * 2)) & 0b11;
            dna.append(rule.charAt(val));
        }

        // Reverse for encryption symmetry
        dna.reverse();

        int result = 0;
        for (int i = 0; i < 4; i++) {
            int val = rule.indexOf(dna.charAt(i));
            if (val == -1) throw new IllegalArgumentException("Invalid DNA base in encryption.");
            result = (result << 2) | val;
        }

        return result;
    }

    private static BufferedImage scrambleImage(BufferedImage img, long seed) {
        int width = img.getWidth();
        int height = img.getHeight();
        int totalPixels = width * height;
        int[] permutation = new int[totalPixels];
        for (int i = 0; i < totalPixels; i++) permutation[i] = i;

        Random rand = new Random(seed);
        for (int i = totalPixels - 1; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            int temp = permutation[i];
            permutation[i] = permutation[j];
            permutation[j] = temp;
        }

        int[] originalPixels = new int[totalPixels];
        img.getRGB(0, 0, width, height, originalPixels, 0, width);
        int[] scrambledPixels = new int[totalPixels];
        for (int i = 0; i < totalPixels; i++) {
            scrambledPixels[permutation[i]] = originalPixels[i];
        }

        BufferedImage scrambledImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        scrambledImg.setRGB(0, 0, width, height, scrambledPixels, 0, width);
        return scrambledImg;
    }

    public static void displayBitPlanes(BufferedImage img, String outputDirectory) {
        int width = img.getWidth();
        int height = img.getHeight();

        File dir = new File(outputDirectory);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                System.err.println("Failed to create output directory: " + outputDirectory);
                return;
            }
        }

        for (int bit = 0; bit < 8; bit++) {
            BufferedImage bitPlane = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = img.getRGB(x, y);
                    int gray = (rgb >> 16) & 0xFF; // Using red channel
                    int bitValue = (gray >> bit) & 1;
                    int binaryColor = (bitValue == 1) ? 0xFFFFFF : 0x000000;
                    bitPlane.setRGB(x, y, binaryColor);
                }
            }
            String filename = String.format("%s/bitplane_encryption_%d.png", outputDirectory, bit);
            File outputFile = new File(filename);
            try {
                ImageIO.write(bitPlane, "PNG", outputFile);
                System.out.println("Bit plane " + bit + " saved to " + filename);
            } catch (IOException e) {
                System.err.println("Error saving bit plane " + bit + ": " + e.getMessage());
            }
        }
    }
}
