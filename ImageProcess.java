import java.awt.image.BufferedImage;
import java.awt.FlowLayout;
import java.io.IOException;
import java.util.Arrays;
import java.util.PriorityQueue;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

public class ImageProcess {
    BufferedImage image;
    int width, height;
    int imagePositionX, imagePositionY;
    int[] pixels;

    public ImageProcess(BufferedImage image) throws IOException {
        this.image = image;
        width = image.getWidth();
        height = image.getHeight();
        pixels = new int[width * height];
        imagePositionX = 0;
        imagePositionY = 0;
        DisplayImage(image, "Original");
    }

    public int[] ToGray() throws IOException {
        int[] res = new int[width * height];
        // int startX, int startY, int w, int h, int[] rgbArray, int offset, int
        // scansize
        image.getRGB(0, 0, width, height, pixels, 0, width); // 填入pixels
        for (int i = 0; i < width * height; i++) {
            int rgb = pixels[i];
            // 255,255,255
            // 0xffffff
            int red = (rgb & 0xff0000) >> 16;
            int green = (rgb & 0x00ff00) >> 8;
            int blue = rgb & 0x0000ff;
            // Gray = 0.299 * Red + 0.587 * Green + 0.114 * Blue
            int gray = (int) (0.299 * red + 0.587 * green + 0.114 * blue);
            pixels[i] = (0xff000000 | gray << 16 | gray << 8 | gray);
            res[i] = gray;
        }
        // image 轉 BfferedImage
        ImageToBuffer(pixels, "Gray");
        return res;
    }

    public void ToNegative(int[] gray_image) throws IOException {
        int[] res = new int[width * height];
        for (int i = 0; i < gray_image.length; i++) {
            int negative = 255 - gray_image[i];
            res[i] = (0xff000000 | negative << 16 | negative << 8 | negative);
        }
        ImageToBuffer(res, "ToNegative");
    }

    public int[] ToGamma(int[] gray_image, double gamma) throws IOException {
        int[] res = new int[width * height];
        int max = -1;
        int min = 256;

        for (int i = 0; i < pixels.length; i++) { // 找max and min
            max = Math.max(max, gray_image[i]);
            min = Math.min(min, gray_image[i]);
        }
        /*
         * Gamma公式
         * [(p(i,j)-min/max-min)^gamma]*255
         * p(i,j)為像素點之值，min為圖片中像素之最小值，max為圖片中像素之最大值
         */
        for (int i = 0; i < gray_image.length; i++) {
            double gamma_double = Math.pow((double) (gray_image[i] - min) / (max - min), gamma) * 255;
            int g = (int) gamma_double;
            pixels[i] = (0xff000000 | g << 16 | g << 8 | g);
            res[i] = g;
        }

        ImageToBuffer(pixels, "Gamma" + Double.toString(gamma));
        return res;
    }

    // 胡椒鹽
    public int[] ToPepper(int[] gamma) throws IOException {
        int[] res = new int[width * height];
        System.arraycopy(gamma, 0, res, 0, gamma.length);

        // 10% 0
        // 10% 255
        // 80% res[i]
        for (int i = 0; i < gamma.length; i++) {
            int peper = (int) (Math.random() * 10);
            if (peper == 0) {
                res[i] = 0;
            } else if (peper == 1) {
                res[i] = 255;
            }
            pixels[i] = (0xff000000 | res[i] << 16 | res[i] << 8 | res[i]);
        }
        ImageToBuffer(pixels, "Pepper");
        return res;
    }

    public void MedianFilter(int[] pepper, boolean isMedian) throws IOException {
        int[] res = new int[width * height];
        int[] median = new int[pepper.length];
        System.arraycopy(pepper, 0, median, 0, pepper.length);

        int[] dx = { -1, 0, 1 };
        PriorityQueue<Integer> min = new PriorityQueue<Integer>((a, b) -> (b - a));
        PriorityQueue<Integer> max = new PriorityQueue<Integer>((a, b) -> (a - b));
        for (int i = 1; i < width - 1; i++)
            for (int j = 1; j < height - 1; j++) {
                min.clear();
                max.clear();
                int maxValue = 0;
                for (int k = 0; k < 3; k++) {
                    for (int z = 0; z < 3; z++) {
                        int val = median[width * (j + dx[k]) + i + dx[z]];
                        if (isMedian) {
                            if (min.isEmpty() || val < min.peek()) {
                                min.offer(val);
                                if (min.size() > max.size() + 1) {
                                    max.offer(min.poll());
                                }
                            } else {
                                max.offer(val);
                                if (max.size() > min.size()) {
                                    min.offer(max.poll());
                                }
                            }
                        } else {
                            maxValue = Math.max(maxValue, val);
                        }
                    }
                }

                int medianValue = 0;
                if (isMedian) {
                    medianValue = min.peek();
                } else {
                    medianValue = maxValue;
                }
                res[width * j + i] = medianValue;
            }

        for (int i = 0; i < median.length; i++) {
            res[i] = (0xff000000 | res[i] << 16 | res[i] << 8 | res[i]);
        }
        ImageToBuffer(res, isMedian ? "MedianFilter" : "MaxFilter");
    }

    public int[] LaplacianFilter(int[] gamma2) throws IOException {
        int[] res = new int[width * height];
        int cnt = 0;
        int[] dx = { -1, 0, 1 };
        int[] laplacMask = { 0, -1, 0, -1, 4, -1, 0, -1, 0 }; // 一階
        // int[] laplacMask = { -1, -1, -1, -1, 8, -1, -1, -1, -1 }; // 二階

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int total = 0;
                if (i == 0 || i == height - 1 || j == 0 || j == width - 1) {
                    pixels[cnt] = (0xff000000 | total << 16 | total << 8 | total);
                    res[cnt++] = total;
                    continue;
                }
                for (int k = 0; k < 3; k++) {
                    for (int z = 0; z < 3; z++) {
                        total += laplacMask[k * 3 + z] * gamma2[width * (i + dx[k]) + j + dx[z]];
                    }
                }
                if (total < 0)
                    total = 0;
                if (total > 255)
                    total = 255;
                pixels[cnt] = (0xff000000 | total << 16 | total << 8 | total);
                res[cnt++] = total;
            }
        }

        ImageToBuffer(pixels, "LaplacianFilter");
        return res;
    }

    public void ToSobel(int[] gamma2) throws IOException {
        int[][] Gx = new int[height][width];
        int[][] Gy = new int[height][width];
        int[][] G = new int[height][width];
        int[][] original = new int[height][width];
        int cnt = 0;
        System.out.println(gamma2.length);
        for (int i = 0; i < height; i++)
            for (int j = 0; j < width; j++)
                original[i][j] = gamma2[cnt++];
        cnt = 0;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (i == 0 || i == height - 1 || j == 0 || j == width - 1) {
                    Gx[i][j] = Gy[i][j] = G[i][j] = 0;
                } else {
                    Gy[i][j] = original[i + 1][j - 1] + 2 * original[i + 1][j] + original[i + 1][j + 1] -
                            original[i - 1][j - 1] - 2 * original[i - 1][j] - original[i - 1][j + 1];
                    Gx[i][j] = original[i - 1][j + 1] + 2 * original[i][j + 1] + original[i + 1][j + 1] -
                            original[i - 1][j - 1] - 2 * original[i][j - 1] - original[i + 1][j - 1];
                    // 公式（soble = sqrt[(x^2+y^2)]）
                    G[i][j] = (int) (Math.sqrt((Math.pow(Gx[i][j], 2) + Math.pow(Gy[i][j], 2))));
                }
                pixels[cnt++] = (0xff000000 | G[i][j] << 16 | G[i][j] << 8 | G[i][j]);
            }
        }
        ImageToBuffer(pixels, "Sobel");
    }

    // OTSU
    public void ToOTSU(int[] gamma3) throws IOException {
        int threashed = 0;
        int GrayScale = 256;
        int[] pixCount = new int[GrayScale];
        double[] pixPro = new double[GrayScale];
        double w0, w1, u0tmp, u1tmp, u0, u1, deltaTmp, deltaMax = 0;
        int[] res = new int[width * height];
        Arrays.fill(pixCount, 0);
        // 計算灰階值次數
        for (int i = 0; i < gamma3.length; i++) {
            pixCount[gamma3[i]]++;
        }
        // 在整張影像中的比例
        for (int i = 0; i < GrayScale; i++) {
            pixPro[i] = pixCount[i] * 1.0 / (width * height);
        }

        for (int i = 0; i < GrayScale; i++) {
            w0 = w1 = u0tmp = u1tmp = u0 = u1 = deltaTmp = 0;
            for (int j = 0; j < GrayScale; j++) {
                if (j <= i)// 背景
                {
                    w0 += pixPro[j];
                    u0tmp += j * pixPro[j];
                } else// 前景
                {
                    w1 += pixPro[j];
                    u1tmp += j * pixPro[j];
                }
            }
            u0 = u0tmp / w0;
            u1 = u1tmp / w1;
            deltaTmp = w0 * w1 * (u1 - u0) * (u1 - u0);
            if (deltaTmp > deltaMax) {
                deltaMax = deltaTmp;
                threashed = i;
            }
        }
        System.out.println(threashed);
        for (int i = 0; i < width * height; i++) {
            if (gamma3[i] >= threashed)
                res[i] = 255;
            else
                res[i] = 0;
            res[i] = (0xff000000 | res[i] << 16 | res[i] << 8 | res[i]);
        }

        ImageToBuffer(res, "ToOTSU");
    }

    private void ImageToBuffer(int[] val, String title) throws IOException {
        BufferedImage gama_image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB); // image 轉 BfferedImage
        gama_image.setRGB(0, 0, width, height, val, 0, width);
        DisplayImage(gama_image, title);
    }

    private void DisplayImage(BufferedImage image, String name) throws IOException {
        ImageIcon icon = new ImageIcon(image);
        JFrame frame = new JFrame(name);
        frame.setLayout(new FlowLayout());
        frame.setSize(width, height);
        JLabel lbl = new JLabel();
        lbl.setIcon(icon);
        frame.add(lbl, SwingConstants.CENTER);
        frame.setVisible(true);
        frame.setLocation(imagePositionX, imagePositionY);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        imagePositionX += width;
        if (imagePositionX > 1200) {
            imagePositionX = 100;
            imagePositionY += height;
        }
    }

}
