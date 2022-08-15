
import java.io.File;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/**
 * main
 */

public class Main {
    public static void main(String[] args) {
        File file = new File("image/1.jpg");
        try {
            BufferedImage image = ImageIO.read(file);
            ImageProcess process = new ImageProcess(image);
            int[] gray = process.ToGray();
            process.ToNegative(gray);

            int[] Gamma1 = process.ToGamma(gray, 0.5);
            int[] Gamma2 = process.ToGamma(gray, 1);
            int[] Gamma3 = process.ToGamma(gray, 2);
            int[] Pepper = process.ToPepper(Gamma1);
            process.MedianFilter(Pepper, true);
            int[] L = process.LaplacianFilter(Gamma2);
            process.MedianFilter(L, false);
            // process.ToSobel(Gamma2);
            process.ToOTSU(Gamma3);
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}