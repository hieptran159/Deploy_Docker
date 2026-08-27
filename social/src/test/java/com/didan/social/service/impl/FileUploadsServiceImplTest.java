package com.didan.social.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Kiểm tra chặn kích thước + thu nhỏ/nén ảnh trong FileUploadsServiceImpl (không cần Spring context). */
class FileUploadsServiceImplTest {

    @TempDir Path dirA;
    @TempDir Path dirB;

    private FileUploadsServiceImpl svc;

    @BeforeEach
    void setUp() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("app.file.upload-dir", dirA.toString());
        env.setProperty("app.file.no-reload", dirB.toString());
        env.setProperty("app.file.max-size-bytes", "5000000");
        env.setProperty("app.file.max-dimension", "1600");
        env.setProperty("app.file.jpeg-quality", "0.82");
        svc = new FileUploadsServiceImpl(env);
    }

    private static byte[] png(int w, int h) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    private static byte[] jpg(int w, int h) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", out);
        return out.toByteArray();
    }

    @Test
    void extensionAndContentTypePredicates() {
        assertTrue(svc.isSupportedExtension("png"));
        assertTrue(svc.isSupportedExtension("jpg"));
        assertTrue(svc.isSupportedExtension("jpeg"));
        assertFalse(svc.isSupportedExtension("gif"));
        assertFalse(svc.isSupportedExtension(null));
        assertTrue(svc.isSupportedContentType("image/png"));
        assertFalse(svc.isSupportedContentType("text/plain"));
        assertFalse(svc.isSupportedContentType(null));
    }

    @Test
    void downscalesLargeImage() throws Exception {
        MockMultipartFile f = new MockMultipartFile("cover", "big.png", "image/png", png(2400, 1800));
        String name = svc.storeFile(f, "post", "p1");
        assertEquals("p1.png", name);

        Path stored = dirA.resolve("post").resolve("p1.png");
        assertTrue(Files.exists(stored), "phải ghi vào thư mục upload-dir");
        assertTrue(Files.exists(dirB.resolve("post").resolve("p1.png")), "phải ghi vào thư mục no-reload");

        BufferedImage back = ImageIO.read(stored.toFile());
        assertEquals(1600, Math.max(back.getWidth(), back.getHeight()), "cạnh dài phải bằng max-dimension");
        assertEquals(1200, back.getHeight());
    }

    @Test
    void keepsSmallImageUntouched() throws Exception {
        byte[] original = png(300, 200);
        MockMultipartFile f = new MockMultipartFile("cover", "small.png", "image/png", original);
        svc.storeFile(f, "avt", "s1");
        byte[] stored = Files.readAllBytes(dirA.resolve("avt").resolve("s1.png"));
        assertArrayEquals(original, stored, "PNG trong giới hạn -> giữ nguyên bytes");
    }

    @Test
    void recompressesLargeJpeg() throws Exception {
        MockMultipartFile f = new MockMultipartFile("cover", "big.JPG", "image/jpeg", jpg(3000, 2000));
        String name = svc.storeFile(f, "post", "j1");
        assertEquals("j1.jpg", name, "đuôi file được chuẩn hoá về chữ thường");
        BufferedImage back = ImageIO.read(dirA.resolve("post").resolve("j1.jpg").toFile());
        assertEquals(1600, back.getWidth());
    }

    @Test
    void rejectsNonImage() {
        MockMultipartFile f = new MockMultipartFile("cover", "evil.png", "image/png",
                "This is definitely not an image".getBytes());
        assertThrows(Exception.class, () -> svc.storeFile(f, "post", "x1"));
    }

    @Test
    void rejectsOversizeInValidateFile() throws Exception {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("app.file.upload-dir", dirA.toString());
        env.setProperty("app.file.no-reload", dirB.toString());
        env.setProperty("app.file.max-size-bytes", "100");
        FileUploadsServiceImpl tiny = new FileUploadsServiceImpl(env);
        MockMultipartFile f = new MockMultipartFile("cover", "big.png", "image/png", png(400, 400));
        assertFalse(tiny.validateFile(f));
    }
}
