package com.didan.social.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class ResourceWebConfig implements WebMvcConfigurer {

    // Spring 6 bỏ charset khỏi Content-Type của application/json (mặc định vẫn UTF-8).
    // Khai báo lại tường minh -> "application/json;charset=UTF-8".
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        for (HttpMessageConverter<?> c : converters) {
            if (c instanceof MappingJackson2HttpMessageConverter jackson) {
                jackson.setDefaultCharset(StandardCharsets.UTF_8);
                jackson.setSupportedMediaTypes(List.of(
                        new MediaType("application", "json", StandardCharsets.UTF_8),
                        new MediaType("application", "*+json", StandardCharsets.UTF_8)));
            }
        }
    }
    private static final String[] CLASSPATH_RESOURCE_LOCATIONS = {"classpath:/static/uploads/","classpath:/templates/"}; // Khai báo đường dẫn tĩnh của các file resource
    @Override
    public void addResourceHandlers(final ResourceHandlerRegistry registry){ // Hàm này sẽ cấu hình resource cho Spring Boot
        // Ảnh do người dùng upload được lưu ở ./uploads/images (app.file.upload-dir) -> phục vụ trực tiếp từ ổ đĩa
        //
        // immutable + 1 năm, KHÔNG phải 1 giờ: mọi tên file ở đây đã mang dấu thời gian
        // (`<id>-<ts>`), nên một URL không bao giờ đổi nội dung — đổi ảnh là sinh URL mới và
        // DB trỏ sang tên mới. Với 3600s, người quay lại sau đó phải gửi một loạt request
        // kiểm tra lại từng ảnh; qua Cloudflare từ Việt Nam mỗi lượt là ~0,4s.
        //
        // ĐIỀU KIỆN để khai immutable là mọi nơi ghi ảnh phải sinh tên mới. Chỗ duy nhất
        // từng phạm luật là sửa ảnh bình luận (ghi lại đúng `comment/<commentId>`), đã sửa
        // cùng lúc với thay đổi này. Thêm chỗ ghi ảnh mới thì phải giữ đúng luật đó, nếu
        // không thì ảnh cũ đóng băng trong trình duyệt người dùng cả năm.
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:./uploads/images/", "classpath:/static/uploads/images/")
                .setCacheControl(org.springframework.http.CacheControl
                        .maxAge(365, java.util.concurrent.TimeUnit.DAYS)
                        .cachePublic()
                        .immutable());
        registry.addResourceHandler("/**") // đường dẫn tới các file resource từ client
                .addResourceLocations(CLASSPATH_RESOURCE_LOCATIONS) // Đường dẫn tĩnh của các file resource
                .setCachePeriod(3600); // Thời gian cache resource là 3600s
    }
}
