package com.captcha;

import org.opencv.core.Mat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) throws IOException {
        String outputDir  = args.length > 0 ? args[0] : "output";
        int    count      = args.length > 1 ? Integer.parseInt(args[1]) : 5;

        Files.createDirectories(Paths.get(outputDir));

        System.out.printf("CAPTCHA 이미지 %d장 생성 → %s%n%n", count, outputDir);

        for (int i = 0; i < count; i++) {
            String text    = CaptchaGenerator.generateRandomText(6);
            Mat    captcha = CaptchaGenerator.generateCaptchaImage(text);

            String filename = outputDir + "/captcha_" + text + ".png";
            CaptchaGenerator.saveImage(captcha, filename);

            System.out.printf("[%d/%d] %-8s → %s%n", i + 1, count, text, filename);
        }

        System.out.println("\n완료.");
    }
}
