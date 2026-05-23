package com.captcha;

import nu.pattern.OpenCV;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CaptchaGenerator {

    private static final int WIDTH  = 220;
    private static final int HEIGHT = 80;

    // 혼동하기 쉬운 문자(0/O, 1/l/I) 제외
    private static final String CHAR_POOL =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";

    private static final Random RANDOM = new Random();

    static {
        OpenCV.loadLocally();
    }

    // ── 공개 API ──────────────────────────────────────────────────────────────

    public static String generateRandomText(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHAR_POOL.charAt(RANDOM.nextInt(CHAR_POOL.length())));
        }
        return sb.toString();
    }

    /**
     * 6자리 CAPTCHA 이미지를 생성한다.
     * 1) Graphics2D로 문자를 렌더링
     * 2) OpenCV perspective warp 변환
     * 3) 문자 위를 가로지르는 완만한 곡선 합성
     */
    public static Mat generateCaptchaImage(String text) {
        BufferedImage img    = renderText(text);
        Mat           mat    = toMat(img);
        Mat           warped = applyPerspectiveWarp(mat);
        drawCurve(warped);
        return warped;
    }

    public static void saveImage(Mat image, String filePath) {
        Imgcodecs.imwrite(filePath, image);
    }

    // ── 내부 구현 ──────────────────────────────────────────────────────────────

    /** Java Graphics2D로 텍스트를 그린다 (각 글자별 위치·크기·회전에 무작위 변화). */
    private static BufferedImage renderText(String text) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D    g     = image.createGraphics();

        // 흰 배경
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 배경 노이즈 점
        g.setColor(new Color(210, 210, 210));
        for (int i = 0; i < 80; i++) {
            g.fillOval(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT), 2, 2);
        }

        // 글자 배치
        int charSpacing = (WIDTH - 20) / text.length();

        for (int i = 0; i < text.length(); i++) {
            int fontSize = 28 + RANDOM.nextInt(10);
            g.setFont(new Font("SansSerif", Font.BOLD, fontSize));

            // 짙은 무작위 색
            g.setColor(new Color(
                    RANDOM.nextInt(80) + 10,
                    RANDOM.nextInt(80) + 10,
                    RANDOM.nextInt(80) + 10
            ));

            int x = 10 + i * charSpacing + RANDOM.nextInt(6) - 3;
            int y = HEIGHT / 2 + fontSize / 3 + RANDOM.nextInt(8) - 4;

            // 글자마다 약간씩 회전 (-0.2 ~ +0.2 rad)
            AffineTransform saved = g.getTransform();
            double angle = (RANDOM.nextDouble() - 0.5) * 0.4;
            g.rotate(angle, x + fontSize / 3.0, HEIGHT / 2.0);
            g.drawString(String.valueOf(text.charAt(i)), x, y);
            g.setTransform(saved);
        }

        g.dispose();
        return image;
    }

    /** BufferedImage(BGR) → OpenCV Mat 변환. */
    private static Mat toMat(BufferedImage bi) {
        // TYPE_3BYTE_BGR은 OpenCV의 CV_8UC3(BGR)과 바이트 순서가 동일
        BufferedImage bgr = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D    g   = bgr.createGraphics();
        g.drawImage(bi, 0, 0, null);
        g.dispose();

        byte[] pixels = ((DataBufferByte) bgr.getRaster().getDataBuffer()).getData();
        Mat    mat    = new Mat(bgr.getHeight(), bgr.getWidth(), CvType.CV_8UC3);
        mat.put(0, 0, pixels);
        return mat;
    }

    /**
     * OpenCV getPerspectiveTransform으로 사각형 네 꼭짓점을 살짝 이동해
     * 이미지 전체를 왜곡한다.
     */
    private static Mat applyPerspectiveWarp(Mat src) {
        int w        = src.width();
        int h        = src.height();
        int maxShift = 10;          // 최대 이동 픽셀 (너무 크면 텍스트 잘림)

        MatOfPoint2f srcPts = new MatOfPoint2f(
                new Point(0,     0),
                new Point(w,     0),
                new Point(w,     h),
                new Point(0,     h)
        );

        // 각 꼭짓점을 독립적으로 조금씩 이동 → 사다리꼴 형태의 왜곡
        MatOfPoint2f dstPts = new MatOfPoint2f(
                new Point(RANDOM.nextInt(maxShift),         RANDOM.nextInt(maxShift)),
                new Point(w - RANDOM.nextInt(maxShift),     RANDOM.nextInt(maxShift)),
                new Point(w - RANDOM.nextInt(maxShift),     h - RANDOM.nextInt(maxShift)),
                new Point(RANDOM.nextInt(maxShift),         h - RANDOM.nextInt(maxShift))
        );

        Mat M      = Imgproc.getPerspectiveTransform(srcPts, dstPts);
        Mat result = new Mat();
        Imgproc.warpPerspective(src, result, M, new Size(w, h),
                Imgproc.INTER_LINEAR, Core.BORDER_CONSTANT, new Scalar(255, 255, 255));
        return result;
    }

    /**
     * 3차 베지어(Cubic Bezier) 곡선을 문자열 위로 그린다.
     * 제어점의 Y 편차를 작게 유지해 곡선이 과하게 구부러지지 않도록 한다.
     */
    private static void drawCurve(Mat image) {
        int w = image.width();
        int h = image.height();

        // 시작/끝/제어점 Y — 모두 세로 중앙 근처에서 소폭 변화
        int y0     = h / 2 + RANDOM.nextInt(22) - 11;
        int y3     = h / 2 + RANDOM.nextInt(22) - 11;
        int ctrl1Y = h / 2 + RANDOM.nextInt(14) -  7;   // ±7 px → 완만
        int ctrl2Y = h / 2 + RANDOM.nextInt(14) -  7;

        // 3차 베지어 X 제어점: 0, w/3, 2w/3, w-1
        int numSamples = 100;
        Point[] pts = new Point[numSamples];

        for (int i = 0; i < numSamples; i++) {
            double t  = (double) i / (numSamples - 1);
            double mt = 1.0 - t;

            double x = mt*mt*mt * 0
                     + 3*mt*mt*t * (w / 3.0)
                     + 3*mt*t*t  * (2.0 * w / 3.0)
                     + t*t*t     * (w - 1);

            double y = mt*mt*mt * y0
                     + 3*mt*mt*t * ctrl1Y
                     + 3*mt*t*t  * ctrl2Y
                     + t*t*t     * y3;

            pts[i] = new Point(x, y);
        }

        MatOfPoint       curvePoints = new MatOfPoint(pts);
        List<MatOfPoint> curves      = new ArrayList<>();
        curves.add(curvePoints);

        // 중간 밝기의 무작위 색 (너무 진하면 글자 판독 불가)
        Scalar color = new Scalar(
                RANDOM.nextInt(120) + 60,
                RANDOM.nextInt(120) + 60,
                RANDOM.nextInt(120) + 60
        );

        Imgproc.polylines(image, curves, false, color, 2, Imgproc.LINE_AA);
    }
}
