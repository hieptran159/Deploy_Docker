package com.didan.social.service.impl;

import com.didan.social.service.AuthorizePathService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthorizePathServiceImpl implements AuthorizePathService {
    private final Logger logger = LoggerFactory.getLogger(AuthorizePathServiceImpl.class);

    /**
     * Id người đang gọi, hoặc ném lỗi nếu KHÔNG có ai đăng nhập.
     *
     * Ở các route cho khách xem (GET /post/*, /comment/post/*, /auth/**), Spring Security
     * vẫn đặt sẵn một Authentication kiểu ẩn danh, và principal của nó là chuỗi
     * "anonymousUser". Trước đây hàm này trả thẳng chuỗi đó ra như thể là một userId —
     * không nơi nào trong repo xử lý chuỗi này, nên nó lặng lẽ đóng vai một "người dùng"
     * không tồn tại.
     *
     * Hậu quả đã đo được: trong `PostServiceImpl.countView`, người xem = meId và chỉ khi
     * meId == null mới lấy IP. meId không bao giờ null -> nhánh IP là code chết, và MỌI
     * khách dùng chung một khoá chống trùng, nên mỗi bài chỉ ghi nhận được 1 lượt xem
     * khách trong mỗi 30 phút dù có bao nhiêu người đọc.
     *
     * Sửa ở đây thay vì ở từng nơi gọi: mọi caller đều muốn "id người đăng nhập", không ai
     * muốn chuỗi giữ chỗ. Các route bắt buộc đăng nhập không đổi hành vi (khách không vào
     * được tới đó); các route cho khách đều đã bọc try/catch hoặc đi qua
     * `currentUserOrNull()`, tức là đã sẵn sàng nhận null.
     */
    @Override
    public String getUserIdAuthoried() throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            logger.error("Not Authorized");
            throw new Exception("Not Authorized");
        }
        // JwtAuthenticationFilter đặt principal là chuỗi userId. Kiểu khác = không phải
        // phiên đăng nhập của ứng dụng này -> đừng ép kiểu mù rồi nổ ClassCastException.
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof String userId) || userId.isBlank()) {
            logger.error("Not Authorized");
            throw new Exception("Not Authorized");
        }
        return userId;
    }

    @Override
    public String getAccessTokenAuthoried() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Phiên ẩn danh mang credentials là chuỗi rỗng. Trả "" ra ngoài thì nơi nhận phải
        // tự nhớ mà kiểm tra rỗng; trả null thì "không có token" chỉ có một cách viết.
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) return null;
        Object credentials = authentication.getCredentials();
        return credentials instanceof String token && !token.isBlank() ? token : null;
    }
}
