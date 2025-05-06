//using bitplane slicing method
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.Random;

public class ImageDecryption {
    private static final String[] DNA_RULES = {
            "ACTG", "ACGT", "AGCT", "AGTC", "ATCG", "ATGC",
            "CAGT", "CATG", "CGAT", "CGTA", "CTAG", "CTGA",
            "GACT", "GATC", "GCAT", "GCTA", "GTAC", "GTCA",
            "TACG", "TAGC", "TCAG", "TCGA", "TGAC", "TGCA"
    };

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("IMAGE DECRYPTION");

        try {
            System.out.print("Input encrypted image path: ");
            File encryptedFile = new File(sc.nextLine());
            BufferedImage encryptedImg = ImageIO.read(encryptedFile);

            System.out.print("Output path for decrypted image: ");
            String decryptedOutputPath = sc.nextLine();

            System.out.print("Decryption key: ");
            long key = sc.nextLong();
            sc.nextLine(); // consume newline

            BufferedImage decryptedImg = decryptImage(encryptedImg, key);
            ImageIO.write(decryptedImg, "PNG", new File(decryptedOutputPath));
            System.out.println("Decryption successful!");

            System.out.print("Output path for decrypted bit planes: ");
            String decryptedBitPlanePath = sc.nextLine();
            displayBitPlanes(decryptedImg, decryptedBitPlanePath);

            // Reconstruct image from bit planes
            System.out.print("Reconstruct and save grayscale image from bit-planes? (y/n): ");
            String response = sc.nextLine().trim();
            if (response.equalsIgnoreCase("y")) {
                BufferedImage reconstructed = reconstructFromBitPlanes(decryptedBitPlanePath, decryptedImg.getWidth(), decryptedImg.getHeight());
                String reconstructedPath = decryptedBitPlanePath + "/reconstructed_grayscale.png";
                ImageIO.write(reconstructed, "PNG", new File(reconstructedPath));
                System.out.println("Reconstructed grayscale image saved to: " + reconstructedPath);
            }

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Decryption failed: " + e.getMessage());
        } finally {
            sc.close();
        }
    }

    private static BufferedImage decryptImage(BufferedImage img, long key) {
        int width = img.getWidth();
        int height = img.getHeight();
        int[] pixels = new int[width * height];
        img.getRGB(0, 0, width, height, pixels, 0, width);
        BufferedImage decryptedImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int prevPixel = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                int rgb = pixels[index];
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                long pixelKey = key ^ ((long) x << 32) ^ ((long) y << 16);

                b = dnaSubstituteReverse(b, pixelKey + 2);
                g = dnaSubstituteReverse(g, pixelKey + 1);
                r = dnaSubstituteReverse(r, pixelKey);

                b = transformPixelReverse(b, pixelKey + 2, g);
                g = transformPixelReverse(g, pixelKey + 1, r);
                r = transformPixelReverse(r, pixelKey, prevPixel);
                prevPixel = b;

                int finalGray = r; // Assuming grayscale was stored in red channel
                int grayRGB = (finalGray << 16) | (finalGray << 8) | finalGray;
                decryptedImg.setRGB(x, y, grayRGB);
            }
        }
        return unscrambleImage(decryptedImg, key);
    }

    private static int transformPixelReverse(int pixel, long key, int prevPixel) {
        pixel ^= (int) (key & 0xFF);
        pixel = ((pixel >> 1) | (pixel << 7)) & 0xFF;
        pixel ^= prevPixel;
        return pixel;
    }

    private static int dnaSubstituteReverse(int pixel, long key) {
        int ruleIdx = (int) ((key >> 8) % DNA_RULES.length);
        String rule = DNA_RULES[ruleIdx];
        int rotate = (int) (key & 0b11);
        rule = rule.substring(rotate) + rule.substring(0, rotate);

        StringBuilder dna = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            int val = (pixel >> (6 - i * 2)) & 0b11;
            dna.append(rule.charAt(val));
        }

        dna.reverse();
        int result = 0;
        for (int i = 0; i < 4; i++) {
            result = (result << 2) | rule.indexOf(dna.charAt(i));
        }
        return result;
    }

    private static BufferedImage unscrambleImage(BufferedImage img, long seed) {
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

        int[] scrambledPixels = new int[totalPixels];
        img.getRGB(0, 0, width, height, scrambledPixels, 0, width);
        int[] unscrambledPixels = new int[totalPixels];
        for (int i = 0; i < totalPixels; i++) {
            if (permutation[i] >= 0 && permutation[i] < totalPixels) {
                unscrambledPixels[i] = scrambledPixels[permutation[i]];
            } else {
                System.err.println("Error: permutation[" + i + "] = " + permutation[i] + " is out of bounds.");
                unscrambledPixels[i] = 0;
            }
        }
        BufferedImage unscrambledImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        unscrambledImg.setRGB(0, 0, width, height, unscrambledPixels, 0, width);
        return unscrambledImg;
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
                    int gray = (rgb >> 16) & 0xFF;
                    int bitValue = (gray >> bit) & 1;
                    int binaryColor = (bitValue == 1) ? 0xFFFFFF : 0x000000;
                    bitPlane.setRGB(x, y, binaryColor);
                }
            }
            File outputFile = new File(String.format("%s/bitplane_decryption_%d.png", outputDirectory, bit));
            try {
                ImageIO.write(bitPlane, "PNG", outputFile);
                System.out.println("Bit plane " + bit + " saved to " + outputFile.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("Error saving bit plane " + bit + ": " + e.getMessage());
            }
        }
    }

    public static BufferedImage reconstructFromBitPlanes(String bitPlaneDirectory, int width, int height) throws IOException {
        BufferedImage reconstructed = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        BufferedImage[] bitPlanes = new BufferedImage[8];
        for (int i = 0; i < 8; i++) {
            File f = new File(String.format("%s/bitplane_decryption_%d.png", bitPlaneDirectory, i));
            bitPlanes[i] = ImageIO.read(f);
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = 0;
                for (int bit = 0; bit < 8; bit++) {
                    int val = bitPlanes[bit].getRGB(x, y) & 0xFFFFFF;
                    int bitVal = (val == 0xFFFFFF) ? 1 : 0;
                    pixel |= (bitVal << bit);
                }
                int grayRGB = (pixel << 16) | (pixel << 8) | pixel;
                reconstructed.setRGB(x, y, grayRGB);
            }
        }

        return reconstructed;
    }
}
