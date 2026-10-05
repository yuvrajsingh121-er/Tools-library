import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class SteganoEngine {

    // End-of-message delimiter to know where secret text ends
    private static final String DELIMITER = "#####";

    // 1. Secret Message ko Image ke Red Channel LSB me Encode karna
    public static void encode(String sourcePath, String outputPath, String message) throws IOException {
        File file = new File(sourcePath);
        if (!file.exists()) {
            System.err.println("Error: Source image file not found!");
            return;
        }

        BufferedImage img = ImageIO.read(file);
        
        // Append delimiter to secret message
        message += DELIMITER;
        byte[] msgBytes = message.getBytes();

        int width = img.getWidth();
        int height = img.getHeight();
        
        // Check capacity
        int totalPixels = width * height;
        if (msgBytes.length * 8 > totalPixels) {
            System.err.println("Error: Image is too small to hold this message!");
            return;
        }

        int msgIndex = 0;
        int bitIndex = 0;

        outerLoop:
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (msgIndex >= msgBytes.length) break outerLoop;

                int pixel = img.getRGB(x, y);

                int alpha = (pixel >> 24) & 0xff;
                int red   = (pixel >> 16) & 0xff;
                int green = (pixel >> 8)  & 0xff;
                int blue  =  pixel        & 0xff;

                // Extract single bit from message byte (from left to right)
                int currentBit = (msgBytes[msgIndex] >> (7 - bitIndex)) & 1;

                // Set the lowest bit (LSB) of Red channel to our secret bit
                red = (red & 0xFE) | currentBit;

                // Re-build pixel integer
                int newPixel = (alpha << 24) | (red << 16) | (green << 8) | blue;
                img.setRGB(x, y, newPixel);

                bitIndex++;
                if (bitIndex == 8) {
                    bitIndex = 0;
                    msgIndex++;
                }
            }
        }

        ImageIO.write(img, "png", new File(outputPath));
        System.out.println("\n [SUCCESS] Message hidden successfully!");
        System.out.println(" Saved to: " + outputPath);
    }

    // 2. Encoded Image se Secret Message Extract karna
    public static String decode(String imagePath) throws IOException {
        File file = new File(imagePath);
        if (!file.exists()) {
            return "Error: Image file not found!";
        }

        BufferedImage img = ImageIO.read(file);
        int width = img.getWidth();
        int height = img.getHeight();

        StringBuilder extractedText = new StringBuilder();
        byte currentByte = 0;
        int bitCount = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = img.getRGB(x, y);
                int red = (pixel >> 16) & 0xff;

                // Get Red LSB
                int lsb = red & 1;
                currentByte = (byte) ((currentByte << 1) | lsb);
                bitCount++;

                if (bitCount == 8) {
                    char c = (char) currentByte;
                    extractedText.append(c);

                    // Check for delimiter match
                    if (extractedText.toString().endsWith(DELIMITER)) {
                        return extractedText.substring(0, extractedText.length() - DELIMITER.length());
                    }

                    bitCount = 0;
                    currentByte = 0;
                }
            }
        }

        return "No hidden message found or incorrect image!";
    }

    // CLI Interface
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("==========================================");
        System.out.println("     JAVA IMAGE STEGANOGRAPHY TOOL        ");
        System.out.println("==========================================");
        System.out.println("1. Hide Secret Message (Encode)");
        System.out.println("2. Reveal Secret Message (Decode)");
        System.out.print("Select an option (1/2): ");

        int choice = scanner.nextInt();
        scanner.nextLine(); // consume newline

        try {
            if (choice == 1) {
                System.out.print("Enter Input PNG Image path (e.g., input.png): ");
                String inputPath = scanner.nextLine();

                System.out.print("Enter Output Image path (e.g., output.png): ");
                String outputPath = scanner.nextLine();

                System.out.print("Enter Secret Message: ");
                String secretMsg = scanner.nextLine();

                encode(inputPath, outputPath, secretMsg);

            } else if (choice == 2) {
                System.out.print("Enter Encoded Image path (e.g., output.png): ");
                String targetPath = scanner.nextLine();

                String revealedMsg = decode(targetPath);
                System.out.println("\n------------------------------------------");
                System.out.println(" Extracted Message: " + revealedMsg);
                System.out.println("------------------------------------------");

            } else {
                System.out.println("Invalid choice!");
            }
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
        } finally {
            scanner.close();
        }
    }
}
