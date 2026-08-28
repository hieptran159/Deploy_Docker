package com.didan.social.service.impl;

import com.didan.social.dto.ReportDTO;
import com.didan.social.entity.Comments;
import com.didan.social.entity.Posts;
import com.didan.social.entity.Reports;
import com.didan.social.entity.Users;
import com.didan.social.repository.CommentRepository;
import com.didan.social.repository.PostRepository;
import com.didan.social.repository.ReportRepository;
import com.didan.social.repository.UserRepository;
import com.didan.social.service.AuthorizePathService;
import com.didan.social.service.CommentService;
import com.didan.social.service.PostService;
import com.didan.social.service.ReportService;
import com.didan.social.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class ReportServiceImpl implements ReportService {
    private static final List<String> TYPES = Arrays.asList("USER", "POST", "COMMENT");

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final AuthorizePathService authorizePathService;
    private final UserService userService;
    private final PostService postService;
    private final CommentService commentService;

    // Bài viết đạt số báo cáo OPEN >= ngưỡng này sẽ tự ẩn khỏi feed (0 = tắt).
    @Value("${app.moderation.post-autohide-threshold:3}")
    private int autoHideThreshold;

    @Autowired
    public ReportServiceImpl(ReportRepository reportRepository, UserRepository userRepository,
                             PostRepository postRepository, CommentRepository commentRepository,
                             AuthorizePathService authorizePathService, UserService userService,
                             PostService postService, CommentService commentService) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.authorizePathService = authorizePathService;
        this.userService = userService;
        this.postService = postService;
        this.commentService = commentService;
    }

    private Users requireAdmin() throws Exception {
        Users me = userRepository.findFirstByUserId(authorizePathService.getUserIdAuthoried());
        if (me == null || me.getIsAdmin() == 0) {
            throw new Exception("Bạn không có quyền quản trị");
        }
        return me;
    }

    @Override
    public boolean create(String targetType, String targetId, String reason) throws Exception {
        String me = authorizePathService.getUserIdAuthoried();
        String type = targetType == null ? "" : targetType.trim().toUpperCase();
        if (!TYPES.contains(type)) {
            throw new Exception("Loại báo cáo không hợp lệ");
        }
        if (targetId == null || targetId.isBlank()) {
            throw new Exception("Thiếu đối tượng báo cáo");
        }

        Posts postTarget = null;
        if ("USER".equals(type)) {
            if (me.equals(targetId)) throw new Exception("Không thể tự báo cáo mình");
            // tận dụng kiểm tra + tăng bộ đếm blacklist sẵn có
            userService.reportUser(targetId);
        } else if ("POST".equals(type)) {
            postTarget = postRepository.findFirstByPostId(targetId);
            if (postTarget == null) throw new Exception("Không tìm thấy bài viết");
        } else {
            Comments c = commentRepository.findByCommentId(targetId);
            if (c == null) throw new Exception("Không tìm thấy bình luận");
        }

        Reports existing = reportRepository
                .findFirstByReporterIdAndTargetTypeAndTargetIdAndStatus(me, type, targetId, "OPEN");
        if (existing != null) {
            return true; // đã báo cáo, không tạo trùng
        }

        Reports r = new Reports();
        r.setReportId(UUID.randomUUID().toString());
        r.setReporterId(me);
        r.setTargetType(type);
        r.setTargetId(targetId);
        r.setReason(reason == null ? "" : reason.trim());
        r.setStatus("OPEN");
        r.setCreatedAt(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))));
        reportRepository.save(r);

        // Tự ẩn bài viết khi số báo cáo OPEN đạt ngưỡng
        if (postTarget != null && autoHideThreshold > 0) {
            String st = postTarget.getStatus();
            boolean live = st == null || "published".equals(st);
            if (live) {
                long open = reportRepository.countByTargetTypeAndTargetIdAndStatus("POST", targetId, "OPEN");
                if (open >= autoHideThreshold) {
                    postTarget.setStatus("hidden");
                    postRepository.save(postTarget);
                }
            }
        }
        return true;
    }

    @Override
    public List<ReportDTO> listForAdmin(String status) throws Exception {
        requireAdmin();
        String s = status == null ? "OPEN" : status.trim().toUpperCase();
        List<Reports> rows = "ALL".equals(s)
                ? reportRepository.findTop300ByOrderByCreatedAtDesc()
                : reportRepository.findTop300ByStatusOrderByCreatedAtDesc(s);
        List<ReportDTO> out = new ArrayList<>();
        for (Reports r : rows) {
            ReportDTO d = new ReportDTO();
            d.setReportId(r.getReportId());
            d.setReporterId(r.getReporterId());
            Users reporter = userRepository.findFirstByUserId(r.getReporterId());
            d.setReporterName(reporter != null ? reporter.getFullName() : r.getReporterId());
            d.setTargetType(r.getTargetType());
            d.setTargetId(r.getTargetId());
            d.setReason(r.getReason());
            d.setStatus(r.getStatus());
            d.setCreatedAt(r.getCreatedAt() == null ? null : r.getCreatedAt().toString());
            d.setTargetPreview(previewOf(r.getTargetType(), r.getTargetId()));
            d.setSameTargetOpenCount(
                    reportRepository.countByTargetTypeAndTargetIdAndStatus(r.getTargetType(), r.getTargetId(), "OPEN"));
            if ("POST".equals(r.getTargetType())) {
                Posts p = postRepository.findFirstByPostId(r.getTargetId());
                d.setTargetStatus(p != null ? (p.getStatus() == null ? "published" : p.getStatus()) : "deleted");
            }
            out.add(d);
        }
        return out;
    }

    private String previewOf(String type, String id) {
        try {
            if ("USER".equals(type)) {
                Users u = userRepository.findFirstByUserId(id);
                return u != null ? u.getFullName() + " (" + u.getEmail() + ")" : id;
            }
            if ("POST".equals(type)) {
                Posts p = postRepository.findFirstByPostId(id);
                return p != null ? p.getTitle() : "(bài đã xoá)";
            }
            Comments c = commentRepository.findByCommentId(id);
            if (c == null) return "(bình luận đã xoá)";
            String content = c.getContent() == null ? "" : c.getContent();
            return content.length() > 120 ? content.substring(0, 120) + "…" : content;
        } catch (Exception e) {
            return id;
        }
    }

    @Override
    public boolean handle(String reportId, String status) throws Exception {
        Users admin = requireAdmin();
        String s = status == null ? "" : status.trim().toUpperCase();
        if (!s.equals("RESOLVED") && !s.equals("DISMISSED") && !s.equals("OPEN")) {
            throw new Exception("Trạng thái không hợp lệ");
        }
        Reports r = reportRepository.findById(reportId).orElse(null);
        if (r == null) {
            throw new Exception("Không tìm thấy báo cáo");
        }
        r.setStatus(s);
        r.setHandledBy(admin.getUserId());
        r.setHandledAt(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))));
        reportRepository.save(r);
        return true;
    }

    @Override
    public boolean removeReportedTarget(String reportId) throws Exception {
        Users admin = requireAdmin();
        Reports r = reportRepository.findById(reportId).orElse(null);
        if (r == null) {
            throw new Exception("Không tìm thấy báo cáo");
        }
        String type = r.getTargetType();
        if ("POST".equals(type)) {
            postService.deletePost(r.getTargetId());       // nhánh admin trong deletePost cho phép xoá của người khác
        } else if ("COMMENT".equals(type)) {
            commentService.deleteComment(r.getTargetId());
        } else {
            throw new Exception("Chỉ xoá được nội dung bài viết / bình luận. Với người dùng hãy dùng chức năng chặn.");
        }
        // đóng mọi báo cáo OPEN cùng đối tượng
        resolveOpenFor(type, r.getTargetId(), admin.getUserId());
        return true;
    }

    @Override
    public boolean restoreReportedTarget(String reportId) throws Exception {
        Users admin = requireAdmin();
        Reports r = reportRepository.findById(reportId).orElse(null);
        if (r == null) throw new Exception("Không tìm thấy báo cáo");
        if (!"POST".equals(r.getTargetType())) {
            throw new Exception("Chỉ khôi phục được bài viết bị ẩn");
        }
        Posts p = postRepository.findFirstByPostId(r.getTargetId());
        if (p == null) throw new Exception("Không tìm thấy bài viết");
        p.setStatus("published");
        postRepository.save(p);
        resolveOpenFor("POST", r.getTargetId(), admin.getUserId());
        return true;
    }

    // Đánh dấu RESOLVED mọi báo cáo OPEN của cùng đối tượng
    private void resolveOpenFor(String type, String targetId, String adminId) {
        Date now = Timestamp.valueOf(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        for (Reports open : reportRepository.findTop300ByStatusOrderByCreatedAtDesc("OPEN")) {
            if (type.equals(open.getTargetType()) && targetId.equals(open.getTargetId())) {
                open.setStatus("RESOLVED");
                open.setHandledBy(adminId);
                open.setHandledAt(now);
                reportRepository.save(open);
            }
        }
    }
}
