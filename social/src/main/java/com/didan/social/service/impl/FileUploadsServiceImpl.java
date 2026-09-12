package com.didan.social.service.impl;

import com.didan.social.service.FileUploadsService;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

@Service
public class FileUploadsServiceImpl implements FileUploadsService {
    private final Logger logger = LoggerFactory.getLogger(FileUploadsServiceImpl.class);
    private final Environment env;

    private final long maxSizeBytes;
    private final int maxDimension;
    // Ảnh đại diện hiển thị ở 40px trong feed, to nhất là 160px ở trang hồ sơ -> 256 đã dư
    // cho màn retina. Trước đây nó dùng chung giới hạn 1600px với ảnh bài viết, nên một
    // avatar thật trên production nặng 503 KB mà chỗ nào cũng vẽ nó ở 40px.
    private final int avatarMaxDimension;
    // Ảnh bìa trải hết chiều ngang trang -> cần to hơn avatar, nhưng không cần 1600.
    private final int coverMaxDimension;
    private final float jpegQuality;

    @Autowired
    public FileUploadsServiceImpl(Environment env){
        this.env = env;
        this.maxSizeBytes = parseLong(env.getProperty("app.file.max-size-bytes"), 8L * 1024 * 1024);
        this.maxDimension = (int) parseLong(env.getProperty("app.file.max-dimension"), 1600);
        this.avatarMaxDimension = (int) parseLong(env.getProperty("app.file.avatar-max-dimension"), 256);
        this.coverMaxDimension = (int) parseLong(env.getProperty("app.file.cover-max-dimension"), 1280);
        this.jpegQuality = parseFloat(env.getProperty("app.file.jpeg-quality"), 0.82f);
    }

    private static long parseLong(String v, long def){ try { return v == null ? def : Long.parseLong(v.trim()); } catch (Exception e){ return def; } }
    private static float parseFloat(String v, float def){ try { return v == null ? def : Float.parseFloat(v.trim()); } catch (Exception e){ return def; } }

    private Path rootPath1; // 1 đường dẫn tạo folder ở trong src
    private Path rootPath2; // 1 đường dẫn tạo folder ở ngoài src (trong ./target) (ko cần khởi động lại server, truy cập ngay lập tức)

    public void init(String typeFile){
        this.rootPath1 = Paths.get(env.getProperty("app.file.upload-dir","./src/main/resources/static/uploads/images")+"/"+typeFile).toAbsolutePath().normalize();
        this.rootPath2 = Paths.get(env.getProperty("app.file.no-reload", "./target/classes/static/uploads/images")+"/"+typeFile).toAbsolutePath().normalize();
        try{
            Files.createDirectories(this.rootPath1);
            Files.createDirectories(this.rootPath2);
        }catch (Exception e){
            logger.error("Could not create the directory where the uploaded files will be stored");
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored", e);
        }
    }

    @Override
    public boolean validateFile(MultipartFile file) {
        try{
            if (file == null || file.isEmpty()){
                logger.error("Empty upload");
                return false;
            }
            if(file.getSize() > maxSizeBytes){
                logger.error("File quá lớn: {} bytes (giới hạn {})", file.getSize(), maxSizeBytes);
                return false;
            }
            String extension = FilenameUtils.getExtension(file.getOriginalFilename());
            if(!isSupportedExtension(extension == null ? null : extension.toLowerCase())){
                logger.error("File extension is not support: {}", extension);
                return false;
            }
            Tika tika = new Tika();
            String mimeType = tika.detect(file.getInputStream());
            if(!isSupportedContentType(mimeType)){
                logger.error("Content type of file is not support: {}", mimeType);
                return false;
            }
            return true;
        }catch (Exception e){
            logger.error(e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isSupportedExtension(String extension) {
        return extension != null &&
                (extension.equals("png") || extension.equals("jpg") || extension.equals("jpeg"));
    }

    @Override
    public boolean isSupportedContentType(String contentType) {
        return contentType != null &&
                (contentType.equals("image/png") || contentType.equals("image/jpg") || contentType.equals("image/jpeg"));
    }

    @Override
    public String storeFile(MultipartFile file, String typeFile, String id) throws Exception {
        init(typeFile);
        String ext = FilenameUtils.getExtension(file.getOriginalFilename());
        ext = ext == null ? "jpg" : ext.toLowerCase();
        String fileName = id + "." + ext;
        try{
            if(!validateFile(file)){
                logger.error("Could not upload file");
                throw new Exception("Ảnh không hợp lệ (định dạng png/jpg/jpeg, tối đa " + (maxSizeBytes / (1024 * 1024)) + "MB)");
            }
            if (fileName.contains("..")) {
                logger.error("Sorry! Filename contains invalid path sequence " + fileName);
                throw new RuntimeException("Sorry! Filename contains invalid path sequence " + fileName);
            }

            Encoded enc;
            try (InputStream in = file.getInputStream()) {
                enc = compress(in.readAllBytes(), ext, maxDimensionFor(typeFile), reEncodeAsJpeg(typeFile));
            }
            // Nén có thể ĐỔI định dạng (png -> jpg) nên tên file phải lấy theo kết quả thật.
            // Giá trị trả về chính là thứ được lưu vào DB, đổi đuôi ở đây là an toàn.
            fileName = id + "." + enc.ext;

            Path targetLocation = this.rootPath1.resolve(fileName);
            Path targetLocation2 = this.rootPath2.resolve(fileName);
            Files.write(targetLocation, enc.data);
            Files.write(targetLocation2, enc.data);
            return fileName;
        }catch (Exception e){
            logger.error("Could not store file " + fileName + ". Please try again!");
            throw new RuntimeException(e.getMessage() != null ? e.getMessage()
                    : ("Could not store file " + fileName + ". Please try again!"), e);
        }
    }

    /** Kết quả nén: bytes CÙNG với định dạng thật sự ghi ra (có thể khác định dạng gốc). */
    static final class Encoded {
        final byte[] data;
        final String ext;
        Encoded(byte[] data, String ext) { this.data = data; this.ext = ext; }
    }

    /** Cạnh dài tối đa theo loại ảnh. Ảnh nội dung (bài, bình luận, tin nhắn) giữ nguyên. */
    int maxDimensionFor(String typeFile) {
        if ("avatar".equals(typeFile) || "conversation".equals(typeFile)) return avatarMaxDimension;
        if ("cover".equals(typeFile)) return coverMaxDimension;
        return maxDimension;
    }

    /**
     * Chỉ ảnh đại diện / ảnh bìa mới bị ép sang JPEG.
     *
     * Ảnh bài viết thì KHÔNG: người ta hay đăng ảnh chụp màn hình có chữ, mà JPEG làm nhoè
     * chữ theo kiểu không sửa lại được. Avatar và ảnh bìa gần như luôn là ảnh chụp, ở đó
     * JPEG nhỏ hơn PNG vài lần với cùng kích thước.
     */
    boolean reEncodeAsJpeg(String typeFile) {
        return "avatar".equals(typeFile) || "cover".equals(typeFile) || "conversation".equals(typeFile);
    }

    /**
     * Thu nhỏ ảnh về tối đa {@code maxDim}px cạnh dài và nén lại (JPEG quality
     * {@link #jpegQuality}). Không giải mã được thì trả lại bytes gốc, để lỗi nén không
     * bao giờ chặn được việc upload.
     *
     * @param forceJpeg cho phép đổi png -> jpg khi ảnh nguồn KHÔNG có phần trong suốt
     */
    Encoded compress(byte[] original, String ext, int maxDim, boolean forceJpeg) {
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(original));
            if (src == null) {
                logger.warn("Không giải mã được ảnh để nén, lưu bản gốc");
                return new Encoded(original, ext);
            }
            int w = src.getWidth(), h = src.getHeight();
            double scale = Math.min(1.0, (double) maxDim / Math.max(w, h));
            boolean resize = scale < 1.0;
            // Giữ PNG khi ảnh THẬT SỰ có phần trong suốt: avatar nền trong suốt mà xuất ra
            // JPEG thì thành một ô trắng nằm trong khung tròn.
            boolean png = ext.equals("png") && (!forceJpeg || hasTransparency(src));

            // PNG trong giới hạn kích thước: giữ nguyên (tránh phá alpha, re-encode ít lợi)
            if (png && !resize) return new Encoded(original, ext);

            int nw = resize ? Math.max(1, (int) Math.round(w * scale)) : w;
            int nh = resize ? Math.max(1, (int) Math.round(h * scale)) : h;

            BufferedImage dst = new BufferedImage(nw, nh,
                    png ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
            Graphics2D g = dst.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (!png) {
                g.setColor(java.awt.Color.WHITE); // nền cho ảnh nguồn có alpha khi xuất JPEG
                g.fillRect(0, 0, nw, nh);
            }
            g.drawImage(src, 0, 0, nw, nh, null);
            g.dispose();

            String outExt = png ? "png" : "jpg";
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (png) {
                ImageIO.write(dst, "png", out);
            } else {
                writeJpeg(dst, out);
            }
            byte[] result = out.toByteArray();
            // Nếu nén xong lại to hơn bản gốc (ảnh gốc đã tối ưu) thì dùng bản gốc
            if (!resize && result.length >= original.length) return new Encoded(original, ext);
            logger.info("Nén ảnh {}x{} {} ({}KB) -> {}x{} {} ({}KB)",
                    w, h, ext, original.length / 1024, nw, nh, outExt, result.length / 1024);
            return new Encoded(result, outExt);
        } catch (Exception e) {
            logger.warn("Nén ảnh lỗi ({}), lưu bản gốc", e.getMessage());
            return new Encoded(original, ext);
        }
    }

    /**
     * Có pixel nào KHÔNG đục hoàn toàn hay không.
     *
     * Không thể chỉ hỏi getColorModel().hasAlpha(): rất nhiều PNG chụp màn hình hoặc xuất
     * từ điện thoại là RGBA nhưng alpha toàn 255 — hỏi kiểu đó thì chẳng ảnh nào đổi sang
     * JPEG được. Quét thật, dừng ngay ở pixel trong suốt đầu tiên.
     */
    private static boolean hasTransparency(BufferedImage img) {
        if (!img.getColorModel().hasAlpha()) return false;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if ((img.getRGB(x, y) >>> 24) < 255) return true;
            }
        }
        return false;
    }

    private void writeJpeg(BufferedImage img, ByteArrayOutputStream out) throws Exception {
        Iterator<ImageWriter> it = ImageIO.getImageWritersByFormatName("jpeg");
        if (!it.hasNext()) { ImageIO.write(img, "jpg", out); return; }
        ImageWriter writer = it.next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(jpegQuality);
            }
            writer.write(null, new IIOImage(img, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    @Override
    public boolean deleteFile(String fileName) throws Exception {
        String[] pathFile = fileName.split("/");
        init(pathFile[0]);
        Path filePath1 = rootPath1.resolve(pathFile[1]);
        Path filePath2 = rootPath2.resolve(pathFile[1]);
        try{
            if(Files.exists(filePath1)) {
                Files.delete(filePath1);
            }
            if(Files.exists(filePath2)) {
                Files.delete(filePath2);
            }
            return true;
        }catch (Exception e){
            logger.error("Could not delete file " + fileName + ". Please try again!");
            throw new RuntimeException("Could not delete file " + fileName + ". Please try again!", e);
        }
    }
}
