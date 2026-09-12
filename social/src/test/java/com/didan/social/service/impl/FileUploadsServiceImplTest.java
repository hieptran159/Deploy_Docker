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

    /**
     * PNG ĐỤC và có nhiễu: ảnh một màu nén xuống còn vài trăm byte nên không nói lên được
     * điều gì về việc nén ảnh thật. Nhiễu cố định theo seed để kích thước ổn định giữa các lần chạy.
     */
    private static byte[] noisyPng(int w, int h) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        java.util.Random r = new java.util.Random(42);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) img.setRGB(x, y, r.nextInt(0xFFFFFF));
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    /** PNG có đúng một pixel trong suốt — đủ để coi là ảnh cần giữ nền trong. */
    private static byte[] pngWithHole(int w, int h) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setColor(java.awt.Color.RED);
        g.fillRect(0, 0, w, h);
        g.dispose();
        img.setRGB(0, 0, 0x00000000);
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

    // ---- Ảnh đại diện / ảnh bìa: giới hạn riêng, nhỏ hơn hẳn ảnh nội dung ----
    // Không set app.file.avatar-max-dimension / cover-max-dimension trong MockEnvironment,
    // để các test này khoá luôn GIÁ TRỊ MẶC ĐỊNH — đó mới là thứ chạy trên production.

    @Test
    void gioiHanKichThuocKhacNhauTheoLoaiAnh() {
        assertEquals(256, svc.maxDimensionFor("avatar"));
        assertEquals(256, svc.maxDimensionFor("conversation"), "avatar nhóm chat cũng hiển thị bé");
        assertEquals(1280, svc.maxDimensionFor("cover"));
        assertEquals(1600, svc.maxDimensionFor("post"), "ảnh bài viết giữ nguyên giới hạn cũ");
        assertEquals(1600, svc.maxDimensionFor("message"));
    }

    /**
     * Đây chính là lỗi đo được trên production: một avatar PNG 503 KB, 1600px, được vẽ ở 40px.
     * Nó lọt vì PNG nằm trong giới hạn 1600px thì code cũ trả về nguyên bytes gốc.
     */
    @Test
    void avatarBiThuNhoVaChuyenSangJpeg() throws Exception {
        byte[] original = noisyPng(1000, 1000);
        MockMultipartFile f = new MockMultipartFile("avatar", "me.png", "image/png", original);

        String name = svc.storeFile(f, "avatar", "u1");

        assertEquals("u1.jpg", name, "PNG ảnh chụp -> lưu thành JPEG, tên trả về phải đổi theo");
        byte[] stored = Files.readAllBytes(dirA.resolve("avatar").resolve("u1.jpg"));
        BufferedImage back = ImageIO.read(dirA.resolve("avatar").resolve("u1.jpg").toFile());
        assertEquals(256, Math.max(back.getWidth(), back.getHeight()));
        assertTrue(stored.length * 10L < original.length,
                "phải nhỏ hơn ít nhất 10 lần, đang là " + original.length + " -> " + stored.length);
    }

    @Test
    void avatarCoNenTrongSuotThiGiuPng() throws Exception {
        MockMultipartFile f = new MockMultipartFile("avatar", "logo.png", "image/png", pngWithHole(800, 800));

        String name = svc.storeFile(f, "avatar", "u2");

        assertEquals("u2.png", name, "xuất JPEG sẽ biến nền trong suốt thành ô trắng trong khung tròn");
        BufferedImage back = ImageIO.read(dirA.resolve("avatar").resolve("u2.png").toFile());
        assertEquals(256, Math.max(back.getWidth(), back.getHeight()), "vẫn phải thu nhỏ");
    }

    /**
     * Ảnh bài viết KHÔNG bị ép sang JPEG: ảnh chụp màn hình có chữ bị JPEG làm nhoè, và
     * người đăng không có cách nào lấy lại bản nét.
     */
    @Test
    void anhBaiVietKhongBiEpSangJpeg() throws Exception {
        byte[] original = noisyPng(1000, 1000);
        MockMultipartFile f = new MockMultipartFile("postImg", "screenshot.png", "image/png", original);

        String name = svc.storeFile(f, "post", "p2");

        assertEquals("p2.png", name);
        assertArrayEquals(original, Files.readAllBytes(dirA.resolve("post").resolve("p2.png")));
    }

    @Test
    void anhBiaThuNhoVe1280() throws Exception {
        MockMultipartFile f = new MockMultipartFile("cover", "wide.jpg", "image/jpeg", jpg(3000, 1000));

        String name = svc.storeFile(f, "cover", "c1");

        assertEquals("c1.jpg", name);
        BufferedImage back = ImageIO.read(dirA.resolve("cover").resolve("c1.jpg").toFile());
        assertEquals(1280, back.getWidth());
    }
}
