package com.didan.social.service.impl;

import com.didan.social.dto.*;
import com.didan.social.utils.ClientIpUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.didan.social.entity.*;
import com.didan.social.entity.keys.PostLikeId;
import com.didan.social.entity.keys.UserPostId;
import com.didan.social.payload.request.CreatePostRequest;
import com.didan.social.payload.request.EditPostRequest;
import com.didan.social.repository.*;
import com.didan.social.service.AuthorizePathService;
import com.didan.social.service.PostService;
import com.didan.social.service.convertdto.ConvertDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PostServiceImpl extends ConvertDTO implements PostService {
    //    private Gson gson = new GsonBuilder().setPrettyPrinting().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").create();
    private final Logger logger = LoggerFactory.getLogger(com.didan.social.service.PostService.class);
    private final PostRepository postRepository;
    private final UserPostRepository userPostRepository;
    private final FileUploadsServiceImpl fileUploadsService;
    private final UserRepository userRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;
    private final AuthorizePathService authorizePathService;
    private final com.didan.social.service.NotificationService notificationService;
    private final com.didan.social.repository.BlockRepository blockRepository;
    private final com.didan.social.repository.RepostRepository repostRepository;
    private final com.didan.social.service.FollowService followService;
    private final com.didan.social.repository.PostHashtagRepository postHashtagRepository;
    private final com.didan.social.repository.BookmarkRepository bookmarkRepository;
    @Autowired
    public PostServiceImpl(PostRepository postRepository,
                       UserPostRepository userPostRepository,
                       FileUploadsServiceImpl fileUploadsService,
                       UserRepository userRepository,
                       PostLikeRepository postLikeRepository,
                       CommentRepository commentRepository,
                       AuthorizePathService authorizePathService,
                       com.didan.social.service.NotificationService notificationService,
                       com.didan.social.repository.BlockRepository blockRepository,
                       com.didan.social.repository.RepostRepository repostRepository,
                       com.didan.social.service.FollowService followService,
                       com.didan.social.repository.PostHashtagRepository postHashtagRepository,
                       com.didan.social.repository.BookmarkRepository bookmarkRepository,
                       com.didan.social.service.PostViewThrottle postViewThrottle
    ){
        this.postViewThrottle = postViewThrottle;
        this.bookmarkRepository = bookmarkRepository;
        this.postRepository = postRepository;
        this.userPostRepository = userPostRepository;
        this.fileUploadsService = fileUploadsService;
        this.userRepository = userRepository;
        this.postLikeRepository = postLikeRepository;
        this.commentRepository =commentRepository;
        this.authorizePathService = authorizePathService;
        this.notificationService = notificationService;
        this.blockRepository = blockRepository;
        this.repostRepository = repostRepository;
        this.followService = followService;
        this.postHashtagRepository = postHashtagRepository;
    }

    private final com.didan.social.service.PostViewThrottle postViewThrottle;

    /** Cùng cờ với RateLimitFilter / màn hình phiên: chỉ tin header proxy khi đứng sau proxy. */
    @org.springframework.beans.factory.annotation.Value("${app.ratelimit.trust-forwarded:false}")
    private boolean trustForwarded;

    // Đồng bộ hashtag của 1 bài: xóa hết tag cũ, chèn lại từ tiêu đề + nội dung hiện tại.
    private void syncHashtags(String postId, String title, String body) {
        try {
            postHashtagRepository.deleteByPostHashtagId_PostId(postId);
            java.util.Set<String> tags = com.didan.social.utils.HashtagUtils.extract(title, body);
            if (tags.isEmpty()) return;
            java.util.List<com.didan.social.entity.PostHashtags> rows = new ArrayList<>();
            for (String t : tags) {
                rows.add(new com.didan.social.entity.PostHashtags(
                        new com.didan.social.entity.keys.PostHashtagId(postId, t)));
            }
            postHashtagRepository.saveAll(rows);
        } catch (Exception e) {
            logger.error("syncHashtags lỗi cho bài " + postId + ": " + e.getMessage());
        }
    }

    // Điền danh sách hashtag cho nhiều DTO bằng 1 truy vấn gộp (không N+1)
    private void applyHashtags(List<PostDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) return;
        java.util.List<String> pids = dtos.stream().map(PostDTO::getPostId).distinct().collect(Collectors.toList());
        java.util.Map<String, java.util.List<String>> byPost = new java.util.HashMap<>();
        for (Object[] row : postHashtagRepository.findTagsForPosts(pids)) {
            byPost.computeIfAbsent((String) row[0], k -> new ArrayList<>()).add((String) row[1]);
        }
        for (PostDTO d : dtos) {
            d.setHashtags(byPost.getOrDefault(d.getPostId(), java.util.Collections.emptyList()));
        }
    }

    // Điền repostCount + reposted cho danh sách DTO bằng 2 truy vấn gộp (không N+1)
    private void applyRepostInfo(List<PostDTO> dtos, String meId) {
        if (dtos == null || dtos.isEmpty()) return;
        java.util.List<String> pids = dtos.stream().map(PostDTO::getPostId).distinct().collect(Collectors.toList());
        java.util.Map<String, Long> counts = new java.util.HashMap<>();
        for (Object[] row : repostRepository.countForPosts(pids)) {
            counts.put((String) row[0], ((Number) row[1]).longValue());
        }
        java.util.Set<String> mine = meId == null ? java.util.Collections.emptySet()
                : new java.util.HashSet<>(repostRepository.repostedByUserIn(meId, pids));
        for (PostDTO d : dtos) {
            d.setRepostCount(counts.getOrDefault(d.getPostId(), 0L));
            d.setReposted(mine.contains(d.getPostId()));
        }
    }

    // Tập tác giả bị ẩn khỏi feed/tìm kiếm: (1) quan hệ chặn 2 chiều với tôi,
    // (2) tài khoản đang tự vô hiệu hoá.
    private java.util.Set<String> blockRelatedIds(String meId) {
        java.util.Set<String> s = new java.util.HashSet<>(userRepository.findDeactivatedIds());
        if (meId != null) {
            s.addAll(blockRepository.blockedIdsOf(meId));
            s.addAll(blockRepository.blockerIdsOf(meId));
        }
        return s;
    }
    @Transactional
    @Override
    public String createPost(CreatePostRequest createPostRequest) throws Exception {
        String userId = authorizePathService.getUserIdAuthoried();
        boolean draft = createPostRequest.isDraft();
        if (draft) {
            // Bản nháp: chỉ cần có tiêu đề HOẶC nội dung
            if (!StringUtils.hasText(createPostRequest.getTitle())
                    && !StringUtils.hasText(createPostRequest.getBody())) {
                throw new Exception("Bản nháp cần ít nhất tiêu đề hoặc nội dung");
            }
        } else if (!StringUtils.hasText(createPostRequest.getTitle())
                || !StringUtils.hasText(createPostRequest.getBody())) {
            logger.error("Miss some fields");
            throw new Exception("Miss some fields");
        }
        Users user = userRepository.findFirstByUserId(userId);
        if (user == null) {
            logger.error("User is not found");
            throw new Exception("User is not found");
        }
        Posts post = new Posts();
        UserPosts userPost = new UserPosts();
        UUID postId = UUID.randomUUID();
        post.setPostId(postId.toString());
        post.setStatus(draft ? "draft" : "published");
        post.setVisibility(normVisibility(createPostRequest.getVisibility()));
        post.setTitle(createPostRequest.getTitle() == null ? "" : createPostRequest.getTitle());
        if (createPostRequest.getPostImg() != null && !createPostRequest.getPostImg().isEmpty()){
            String fileName = fileUploadsService.storeFile(createPostRequest.getPostImg(), "post", postId.toString());
            post.setPostImg("post/"+fileName);
        }
        post.setBody(createPostRequest.getBody() == null ? "" : createPostRequest.getBody());
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        Date nowSql = Timestamp.valueOf(now);
        post.setPostedAt(nowSql);
        userPost.setUserPostId(new UserPostId(postId.toString(), user.getUserId()));
        postRepository.save(post);
        userPostRepository.save(userPost);
        syncHashtags(postId.toString(), post.getTitle(), post.getBody());
        return postId.toString();
    }

    @Override
    public List<PostDTO> getAllPosts() throws Exception {
        String meId = currentUserOrNull();
        List<Posts> posts = postRepository.findAllPost(visibleAuthorIds(meId), meParam(meId));
        if (posts == null) {
            logger.info("No posts are here");
            return Collections.emptyList();
        }
        java.util.Set<String> ex = blockRelatedIds(meId);
        return posts.stream()
                .filter(p -> ex.isEmpty() || !ex.contains(p.getUserPost().getUsers().getUserId()))
                .map(post -> toListDTO(post, meId)).collect(Collectors.toList());
    }

    @Override
    public List<PostDTO> getAllPostsByPage(int index) throws Exception {
        if (index < 1) index = 1;
        String meId = currentUserOrNull();
        List<Object[]> rows = postRepository.feedPage(feedExcludeParam(meId), visibleAuthorIds(meId), meParam(meId), 10, (index - 1) * 10);
        return buildFeedFromRows(rows, meId);
    }

    @Override
    public List<PostDTO> getFriendsFeed(int index) throws Exception {
        if (index < 1) index = 1;
        String meId = authorizePathService.getUserIdAuthoried();
        List<Object[]> rows = postRepository.friendFeedPage(friendFeedIds(meId), 10, (index - 1) * 10);
        return buildFeedFromRows(rows, meId);
    }

    @Override
    public java.util.Map<String, Object> friendsFeedPageInfo() throws Exception {
        String meId = authorizePathService.getUserIdAuthoried();
        long total = postRepository.friendFeedCount(friendFeedIds(meId));
        int totalPages = (int) Math.max(1, Math.ceil(total / 10.0));
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("total", total);
        m.put("pageSize", 10);
        m.put("totalPages", totalPages);
        return m;
    }

    // Bạn bè (đã chấp nhận) + chính mình, bỏ tài khoản đang vô hiệu hoá. Luôn khác rỗng (có meId).
    private java.util.Collection<String> friendFeedIds(String meId) {
        java.util.Set<String> ids = new java.util.HashSet<>(followService.friendIdsOf(meId));
        ids.add(meId);
        ids.removeAll(userRepository.findDeactivatedIds());
        ids.add(meId); // giữ lại mình kể cả khi tự vô hiệu hoá (xem feed của chính mình)
        return ids;
    }

    // Dựng danh sách PostDTO từ các dòng feed [pid, sort_t, is_repost]
    private List<PostDTO> buildFeedFromRows(List<Object[]> rows, String meId) {
        if (rows == null || rows.isEmpty()) {
            logger.info("No posts are here");
            return Collections.emptyList();
        }
        java.util.List<String> pidOrder = new ArrayList<>();
        java.util.Set<String> repostPids = new java.util.HashSet<>();
        for (Object[] r : rows) {
            String pid = (String) r[0];
            pidOrder.add(pid);
            if (r[2] != null && ((Number) r[2]).intValue() == 1) repostPids.add(pid);
        }
        java.util.Map<String, Posts> byId = new java.util.HashMap<>();
        for (Posts p : postRepository.findByPostIdIn(new java.util.LinkedHashSet<>(pidOrder))) byId.put(p.getPostId(), p);

        // Với các bài xuất hiện dạng "đã chia sẻ": lấy lượt repost mới nhất (ai chia sẻ + ghi chú)
        java.util.Map<String, com.didan.social.entity.Reposts> latestRepost = new java.util.HashMap<>();
        if (!repostPids.isEmpty()) {
            for (com.didan.social.entity.Reposts rp : repostRepository.findByPostIdsOrderByCreatedAtDesc(repostPids)) {
                latestRepost.putIfAbsent(rp.getRepostId().getPostId(), rp); // dòng đầu = mới nhất
            }
        }
        java.util.Set<String> reposterIds = latestRepost.values().stream()
                .map(rp -> rp.getRepostId().getUserId()).collect(Collectors.toSet());
        java.util.Map<String, Users> reposters = new java.util.HashMap<>();
        if (!reposterIds.isEmpty()) {
            for (Users u : userRepository.findAllById(reposterIds)) reposters.put(u.getUserId(), u);
        }

        List<PostDTO> out = new ArrayList<>();
        for (String pid : pidOrder) {
            Posts p = byId.get(pid);
            if (p == null) continue;
            PostDTO d = toListDTO(p, meId);
            com.didan.social.entity.Reposts rp = latestRepost.get(pid);
            if (rp != null) {
                Users ru = reposters.get(rp.getRepostId().getUserId());
                d.setRepostedBy(ru != null ? ru.getFullName() : rp.getRepostId().getUserId());
                d.setRepostedById(rp.getRepostId().getUserId());
                d.setRepostedAt(rp.getCreatedAt() == null ? null : rp.getCreatedAt().toString());
                d.setRepostNote(rp.getNote());
            }
            out.add(d);
        }
        applyRepostInfo(out, meId);
        applyHashtags(out);
        return out;
    }

    // Tập user id loại khỏi feed cho native NOT IN (phải khác rỗng -> sentinel "-")
    private java.util.Collection<String> feedExcludeParam(String meId) {
        java.util.Set<String> ex = blockRelatedIds(meId);
        return ex.isEmpty() ? java.util.List.of("-") : ex;
    }

    // Tác giả mà người xem được phép thấy bài "chỉ bạn bè": bạn bè của người xem + chính người xem.
    // Luôn khác rỗng (native IN) -> sentinel "-" khi là khách.
    private java.util.Collection<String> visibleAuthorIds(String meId) {
        if (meId == null) return java.util.List.of("-");
        java.util.Set<String> s = new java.util.HashSet<>(followService.friendIdsOf(meId));
        s.add(meId);
        return s;
    }

    // Id người xem cho nhánh ':me' của VISIBLE (bài 'private'/của mình). Khách -> sentinel "-".
    private String meParam(String meId) {
        return meId == null ? "-" : meId;
    }

    // Chuẩn hoá quyền xem bài: 'public' (mặc định) | 'friends' | 'private'
    static String normVisibility(String v) {
        if ("friends".equalsIgnoreCase(v)) return "friends";
        if ("private".equalsIgnoreCase(v)) return "private";
        return "public";
    }

    @Override
    public java.util.Map<String, Object> feedPageInfo() throws Exception {
        int pageSize = 10;
        String meId = currentUserOrNull();
        long total = postRepository.feedCount(feedExcludeParam(meId), visibleAuthorIds(meId), meParam(meId));
        int totalPages = (int) Math.max(1, Math.ceil(total / (double) pageSize));
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("total", total);
        m.put("pageSize", pageSize);
        m.put("totalPages", totalPages);
        return m;
    }

    @Override
    public PostDTO getPostById(String postId) throws Exception {
        Posts post = postRepository.findFirstByPostId(postId);
        if (post == null) {
            logger.info("No post is here");
            return null;
        }
        Users author = post.getUserPost() != null ? post.getUserPost().getUsers() : null;
        String authorId = author != null ? author.getUserId() : null;
        String meId = currentUserOrNull();
        boolean isAuthor = meId != null && meId.equals(authorId);

        // Tác giả đang tự vô hiệu hoá -> chỉ chính chủ xem được
        if (author != null && author.getDeactivated() != null && author.getDeactivated() == 1 && !isAuthor) {
            logger.info("Post by deactivated user not visible");
            return null;
        }
        // Bài "chỉ mình tôi" -> chỉ tác giả
        if ("private".equals(post.getVisibility()) && !isAuthor) {
            logger.info("Private post not visible to this user");
            return null;
        }
        // Bài "chỉ bạn bè" -> chỉ tác giả hoặc bạn bè của tác giả
        if ("friends".equals(post.getVisibility()) && !isAuthor) {
            if (authorId == null || !followService.areFriends(meId, authorId)) {
                logger.info("Friends-only post not visible to this user");
                return null;
            }
        }
        // Bản nháp: chỉ chủ bài. Bài bị ẩn (nhiều báo cáo): chủ bài hoặc admin.
        String st = post.getStatus();
        if ("draft".equals(st) || "hidden".equals(st)) {
            boolean isAdmin = false;
            if (!isAuthor && meId != null && "hidden".equals(st)) {
                Users me = userRepository.findFirstByUserId(meId);
                isAdmin = me != null && me.getIsAdmin() == 1;
            }
            if (!isAuthor && !isAdmin) {
                logger.info("Post {} not visible to this user", st);
                return null;
            }
        }
        // Đếm lượt xem SAU mọi cửa kiểm tra: bài không được phép xem thì không tính.
        // Tác giả tự xem cũng không tính — tự thổi số của chính mình thì con số vô nghĩa.
        if (!isAuthor) countView(post, meId);
        PostDTO dto = (PostDTO) convertToDTO(post);
        applyRepostInfo(java.util.Collections.singletonList(dto), currentUserOrNull());
        applyHashtags(java.util.Collections.singletonList(dto));
        return dto;
    }

    /**
     * Khách vãng lai phân biệt bằng IP; đăng nhập rồi thì bằng userId. Cùng người,
     * cùng bài, trong một cửa sổ -> chỉ tính một lần (xem PostViewThrottle).
     */
    private void countView(Posts post, String meId) {
        String viewer = meId;
        if (viewer == null) {
            RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes sra)
                viewer = ClientIpUtils.resolve(sra.getRequest(), trustForwarded);
        }
        if (!postViewThrottle.shouldCount(post.getPostId(), viewer)) return;
        try {
            postRepository.incrementViews(post.getPostId());
            // UPDATE nguyên tử không đụng tới entity đang cầm trên tay -> cộng thêm
            // cho khớp, khỏi phải đọc lại cả bài chỉ vì một con số.
            post.setViews((post.getViews() == null ? 0 : post.getViews()) + 1);
        } catch (Exception e) {
            // Đếm hụt một lượt xem không đáng để hỏng cả trang bài viết
            logger.warn("Khong dem duoc luot xem cho {}: {}", post.getPostId(), e.getMessage());
        }
    }

    @Override
    public java.util.Map<String, Object> getMyDrafts(int page, int size) throws Exception {
        String meId = authorizePathService.getUserIdAuthoried();
        if (page < 0) page = 0;
        if (size < 1) size = 20;
        if (size > 50) size = 50;
        List<Posts> drafts = postRepository.findDraftsOfAuthor(meId, PageRequest.of(page, size));
        List<PostDTO> items = drafts.stream().map(p -> toListDTO(p, meId)).collect(Collectors.toList());
        applyHashtags(items);
        long total = postRepository.countDraftsOfAuthor(meId);
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("items", items);
        m.put("total", total);
        m.put("page", page);
        m.put("totalPages", (int) Math.max(1, Math.ceil(total / (double) size)));
        return m;
    }

    @Transactional
    @Override
    public java.util.Map<String, Object> publishAllDrafts() throws Exception {
        String meId = authorizePathService.getUserIdAuthoried();
        Date now = Timestamp.valueOf(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        int published = 0, skipped = 0;
        for (Posts p : postRepository.findDraftsOfAuthor(meId)) {
            if (!StringUtils.hasText(p.getTitle()) || !StringUtils.hasText(p.getBody())) { skipped++; continue; }
            p.setStatus("published");
            p.setPostedAt(now);
            postRepository.save(p);
            published++;
        }
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("published", published);
        m.put("skipped", skipped);
        return m;
    }

    @Transactional
    @Override
    public int deleteAllDrafts() throws Exception {
        String meId = authorizePathService.getUserIdAuthoried();
        List<Posts> drafts = postRepository.findDraftsOfAuthor(meId);
        for (Posts p : drafts) {
            postHashtagRepository.deleteByPostHashtagId_PostId(p.getPostId());
            if (StringUtils.hasText(p.getPostImg())) {
                try { fileUploadsService.deleteFile(p.getPostImg()); } catch (Exception ignore) {}
            }
            postRepository.delete(p);
        }
        return drafts.size();
    }

    @Override
    public java.util.Map<String, Object> getPostsByUser(String userId, int page, int size) throws Exception {
        if (page < 0) page = 0;
        if (size < 1) size = 10;
        if (size > 50) size = 50;
        String meId = currentUserOrNull();
        java.util.Collection<String> vids = visibleAuthorIds(meId);
        List<Posts> posts = postRepository.findPublishedByAuthor(userId, vids, meParam(meId), PageRequest.of(page, size));
        List<PostDTO> items = posts.stream().map(p -> toListDTO(p, meId)).collect(Collectors.toList());
        applyRepostInfo(items, meId);
        applyHashtags(items);
        long total = postRepository.countPublishedByAuthor(userId, vids, meParam(meId));
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("items", items);
        m.put("total", total);
        m.put("page", page);
        m.put("totalPages", (int) Math.max(1, Math.ceil(total / (double) size)));
        return m;
    }

    @Override
    public java.util.Map<String, Object> getPostsByTag(String tag, int page, int size) throws Exception {
        if (page < 0) page = 0;
        if (size < 1) size = 10;
        if (size > 50) size = 50;
        String norm = com.didan.social.utils.HashtagUtils.normalize(tag);
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("tag", norm);
        m.put("page", page);
        if (norm == null) {
            m.put("items", java.util.Collections.emptyList());
            m.put("total", 0L);
            m.put("totalPages", 1);
            return m;
        }
        String meId = currentUserOrNull();
        java.util.Collection<String> vids = visibleAuthorIds(meId);
        List<Posts> posts = postHashtagRepository.findPostsByTag(norm, vids, meParam(meId), PageRequest.of(page, size));
        List<PostDTO> items = posts.stream().map(p -> toListDTO(p, meId)).collect(Collectors.toList());
        applyRepostInfo(items, meId);
        applyHashtags(items);
        long total = postHashtagRepository.countPostsByTag(norm, vids, meParam(meId));
        m.put("items", items);
        m.put("total", total);
        m.put("totalPages", (int) Math.max(1, Math.ceil(total / (double) size)));
        return m;
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> getTrendingHashtags(int limit) throws Exception {
        if (limit < 1) limit = 10;
        if (limit > 50) limit = 50;
        java.util.List<java.util.Map<String, Object>> out = new ArrayList<>();
        for (Object[] row : postHashtagRepository.trending(PageRequest.of(0, limit))) {
            java.util.Map<String, Object> item = new java.util.HashMap<>();
            item.put("tag", row[0]);
            item.put("count", ((Number) row[1]).longValue());
            out.add(item);
        }
        return out;
    }

    @Override
    public java.util.Map<String, Object> getRepostsOf(String userId, int page, int size) throws Exception {
        if (page < 0) page = 0;
        if (size < 1) size = 10;
        if (size > 50) size = 50;
        String meId = currentUserOrNull();
        java.util.List<com.didan.social.entity.Reposts> rows = repostRepository
                .findByRepostId_UserId(userId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent();
        List<PostDTO> out = new ArrayList<>();
        if (!rows.isEmpty()) {
            Users sharer = userRepository.findFirstByUserId(userId);
            java.util.List<String> pids = rows.stream().map(r -> r.getRepostId().getPostId()).collect(Collectors.toList());
            java.util.Map<String, Posts> byId = new java.util.HashMap<>();
            for (Posts p : postRepository.findByPostIdIn(pids)) byId.put(p.getPostId(), p);
            for (com.didan.social.entity.Reposts r : rows) {
                Posts p = byId.get(r.getRepostId().getPostId());
                if (p == null) continue;
                String st = p.getStatus();
                if (st != null && !"published".equals(st)) continue; // bỏ bài nháp/ẩn
                PostDTO d = toListDTO(p, meId);
                d.setRepostedBy(sharer != null ? sharer.getFullName() : userId);
                d.setRepostedById(userId);
                d.setRepostedAt(r.getCreatedAt() == null ? null : r.getCreatedAt().toString());
                d.setRepostNote(r.getNote());
                out.add(d);
            }
            applyRepostInfo(out, meId);
            applyHashtags(out);
        }
        long total = repostRepository.countByRepostId_UserId(userId);
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("items", out);
        m.put("total", total);
        m.put("page", page);
        m.put("totalPages", (int) Math.max(1, Math.ceil(total / (double) size)));
        return m;
    }

    @Transactional
    @Override
    public boolean repost(String postId, String note) throws Exception {
        String meId = authorizePathService.getUserIdAuthoried();
        Posts post = postRepository.findFirstByPostId(postId);
        if (post == null) throw new Exception("Không tìm thấy bài viết");
        String st = post.getStatus();
        if (st != null && !"published".equals(st)) throw new Exception("Không thể chia sẻ bài viết này");
        if ("friends".equals(post.getVisibility())) throw new Exception("Không thể chia sẻ bài 'chỉ bạn bè'");
        if ("private".equals(post.getVisibility())) throw new Exception("Không thể chia sẻ bài 'chỉ mình tôi'");
        String authorId = post.getUserPost() != null && post.getUserPost().getUsers() != null
                ? post.getUserPost().getUsers().getUserId() : null;
        if (meId.equals(authorId)) throw new Exception("Không thể tự chia sẻ bài của mình");
        if (repostRepository.existsByRepostId_UserIdAndRepostId_PostId(meId, postId)) return true;
        com.didan.social.entity.Reposts r = new com.didan.social.entity.Reposts();
        r.setRepostId(new com.didan.social.entity.keys.RepostId(meId, postId));
        r.setNote(note == null ? null : note.trim());
        r.setCreatedAt(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))));
        repostRepository.save(r);
        if (authorId != null) {
            Users me = userRepository.findFirstByUserId(meId);
            notificationService.pushUniquePerActor(authorId, meId, "REPOST", postId,
                    (me != null ? me.getFullName() : "Ai đó") + " đã chia sẻ bài viết của bạn");
        }
        return true;
    }

    @Transactional
    @Override
    public boolean unrepost(String postId) throws Exception {
        String meId = authorizePathService.getUserIdAuthoried();
        if (!repostRepository.existsByRepostId_UserIdAndRepostId_PostId(meId, postId)) {
            throw new Exception("Bạn chưa chia sẻ bài viết này");
        }
        repostRepository.deleteByRepostId_UserIdAndRepostId_PostId(meId, postId);
        return true;
    }

    @Transactional
    @Override
    public boolean publishPost(String postId) throws Exception {
        String meId = authorizePathService.getUserIdAuthoried();
        Posts post = postRepository.findFirstByPostId(postId);
        if (post == null) throw new Exception("Không tìm thấy bài viết");
        String authorId = post.getUserPost() != null && post.getUserPost().getUsers() != null
                ? post.getUserPost().getUsers().getUserId() : null;
        if (!meId.equals(authorId)) throw new Exception("Bạn không có quyền với bài viết này");
        if (!"draft".equals(post.getStatus())) throw new Exception("Bài viết đã được đăng");
        if (!StringUtils.hasText(post.getTitle()) || !StringUtils.hasText(post.getBody())) {
            throw new Exception("Cần có tiêu đề và nội dung trước khi đăng");
        }
        post.setStatus("published");
        post.setPostedAt(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))));
        postRepository.save(post);
        return true;
    }

    @Override
    public List<PostDTO> getPostByTitle(String searchName, int page, int size) throws Exception {
        if (searchName == null || searchName.trim().isEmpty()) {
            return Collections.emptyList();
        }
        if (page < 0) page = 0;
        if (size < 1) size = 10;
        if (size > 50) size = 50;
        String meId = currentUserOrNull();
        java.util.Set<String> ex = blockRelatedIds(meId);
        java.util.Collection<String> vids = visibleAuthorIds(meId);
        PageRequest pr = PageRequest.of(page, size);
        List<Posts> posts = ex.isEmpty()
                ? postRepository.searchByKeyword(searchName.trim(), vids, meParam(meId), pr)
                : postRepository.searchByKeywordExcludingAuthors(searchName.trim(), ex, vids, meParam(meId), pr);
        if (posts.isEmpty()) {
            logger.info("No posts are here");
            return Collections.emptyList();
        }
        List<PostDTO> out = posts.stream().map(post -> toListDTO(post, meId)).collect(Collectors.toList());
        applyRepostInfo(out, meId);
        applyHashtags(out);
        return out;
    }

    @Transactional
    @Override
    public boolean likePost(String postId, String type) throws Exception {
        String userId = authorizePathService.getUserIdAuthoried();
        Users user = userRepository.findFirstByUserId(userId);
        if (user == null) {
            logger.error("User is not found");
            throw new Exception("User is not found");
        }
        if (!postRepository.existsById(postId)) {
            logger.info("No post is here");
            throw new Exception("No post is here");
        }
        String reaction = normReaction(type);
        PostLikes existing = postLikeRepository.findByPosts_PostIdAndUsers_UserId(postId, user.getUserId());
        if (existing != null) {
            // đã thả cảm xúc -> chỉ đổi loại
            existing.setType(reaction);
            postLikeRepository.save(existing);
            return true;
        }
        PostLikes newPostLike = new PostLikes();
        newPostLike.setPostLikeId(new PostLikeId(postId, user.getUserId()));
        newPostLike.setType(reaction);
        postLikeRepository.save(newPostLike);
        UserPosts up = userPostRepository.findFirstByPosts_PostId(postId);
        if (up != null && up.getUsers() != null) {
            notificationService.pushUniquePerActor(up.getUsers().getUserId(), user.getUserId(), "POST_LIKE", postId,
                    user.getFullName() + " đã bày tỏ cảm xúc về bài viết của bạn");
        }
        return true;
    }

    @Transactional
    @Override
    public boolean unlikePost(String postId) throws Exception {
        String userId = authorizePathService.getUserIdAuthoried();
        Users user = userRepository.findFirstByUserId(userId);
        if (user == null) {
            logger.error("User is not found");
            throw new Exception("User is not found");
        }
        if (!postRepository.existsById(postId)) {
            logger.info("No post is here");
            throw new Exception("No post is here");
        }
        if (!postLikeRepository.existsByPostLikeId_PostIdAndUsers_UserId(postId, user.getUserId())){
            logger.info("User hasn't liked post yet");
            throw new Exception("User hasn't liked post yet");
        }
        postLikeRepository.deleteByPostLikeId_UserIdAndPostLikeId_PostId(userId, postId);
        return true;
    }

    @Transactional
    @Override
    public PostDTO updatePost(String postId, EditPostRequest editPostRequest) throws Exception {
        String userId = authorizePathService.getUserIdAuthoried();
        Users user = userRepository.findFirstByUserId(userId);
        if (user == null) {
            logger.error("User is not found");
            throw new Exception("User is not found");
        }
        UserPosts userPost = userPostRepository.findFirstByPosts_PostIdAndUsers_UserId(postId, user.getUserId());
        if (userPost == null) {
            logger.error("The user hasn't this post or not authorized to edit this post");
            throw new Exception("The user hasn't this post or not authorized to edit this post");
        }
        Posts post = userPost.getPosts();
        if (StringUtils.hasText(editPostRequest.getTitle())){
            post.setTitle(editPostRequest.getTitle());
        }
        if (StringUtils.hasText(editPostRequest.getBody())){
            post.setBody(editPostRequest.getBody());
        }
        if (StringUtils.hasText(editPostRequest.getVisibility())){
            post.setVisibility(normVisibility(editPostRequest.getVisibility()));
        }
        if (editPostRequest.getPostImg()!= null && !editPostRequest.getPostImg().isEmpty()){
            if(StringUtils.hasText(post.getPostImg())){
                fileUploadsService.deleteFile(post.getPostImg());
            }
            String fileName = fileUploadsService.storeFile(editPostRequest.getPostImg(), "post", postId);
            post.setPostImg("post/"+fileName);
        }
        post.setEditedAt(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))));
        postRepository.save(post);
        syncHashtags(post.getPostId(), post.getTitle(), post.getBody());
        return (PostDTO) convertToDTO(post);
    }

    /*
     * Delete Post
     */
    @Transactional
    @Override
    public boolean deletePost(String postId) throws Exception {
        try{
            String userId = authorizePathService.getUserIdAuthoried();
            Users user = userRepository.findFirstByUserId(userId);
            if (user == null) {
                logger.error("User is not found");
                throw new Exception("User is not found");
            }
            Posts post = postRepository.findFirstByPostId(postId);
            if(post == null) {
                logger.info("There is not post to delete");
                throw new Exception("There is not post to delete");
            }
            // Chỉ tác giả bài viết hoặc admin mới được xoá
            boolean isAdmin = user.getIsAdmin() == 1;
            if (!isAdmin && userPostRepository.findFirstByPosts_PostIdAndUsers_UserId(postId, user.getUserId()) == null) {
                logger.error("User {} tried to delete post {} without permission", user.getUserId(), postId);
                throw new Exception("Bạn không có quyền xoá bài viết này");
            }
            // Dọn mọi thứ tham chiếu tới bài TRƯỚC khi xoá. Entity Posts chỉ cascade
            // sang user_posts, post_likes và user_comment; bookmarks có khoá ngoại
            // nhưng KHÔNG được cascade nên trước đây bài nào đã có người lưu là xoá
            // thất bại (503). reposts không có khoá ngoại nên không chặn xoá, nhưng
            // bỏ sót thì để lại hàng mồ côi mà FEED_UNION vẫn gộp vào.
            // KHÔNG xoá reports ở đây: removeReportedTarget gọi deletePost rồi mới
            // resolveOpenFor, xoá mất thì nhật ký kiểm duyệt cũng mất theo.
            postHashtagRepository.deleteByPostHashtagId_PostId(postId);
            bookmarkRepository.deleteByBookmarkId_PostId(postId);
            repostRepository.deleteByRepostId_PostId(postId);
            postRepository.delete(post);
            if(StringUtils.hasText(post.getPostImg())){
                fileUploadsService.deleteFile(post.getPostImg());
            }
            List<String> commentIds = commentRepository.findCommentIdNotInUserComment();
            for (String commentId : commentIds){
                commentRepository.deleteById(commentId);
            }
            return true;
        } catch (Exception e){
            logger.error(e.getMessage());
            throw new Exception(e.getMessage());
        }
    }
    private static final java.util.List<String> REACTION_TYPES =
            java.util.Arrays.asList("LIKE", "LOVE", "HAHA", "WOW", "SAD", "ANGRY");

    static String normReaction(String t) {
        if (t == null) return "LIKE";
        String u = t.trim().toUpperCase();
        return REACTION_TYPES.contains(u) ? u : "LIKE";
    }

    private String currentUserOrNull() {
        try { return authorizePathService.getUserIdAuthoried(); } catch (Exception e) { return null; }
    }

    // Dựng phần chung của PostDTO (không bao gồm chi tiết bình luận).
    // postLikes/userComments là quan hệ đã nạp sẵn -> chỉ duyệt trong bộ nhớ, không thêm truy vấn.
    private PostDTO basePostDTO(Posts post, String meId) {
        PostDTO dto = new PostDTO();
        dto.setPostId(post.getPostId());
        dto.setStatus(post.getStatus());
        dto.setVisibility(post.getVisibility());
        dto.setUserCreatedPost(post.getUserPost().getUserPostId().getUserId());
        Users author = post.getUserPost().getUsers();
        if (author != null) {
            dto.setAuthorName(author.getFullName());
            dto.setAuthorAvatar(author.getAvtUrl());
        }
        dto.setTitle(post.getTitle());
        dto.setPostImg(post.getPostImg());
        dto.setBody(post.getBody());
        dto.setPostedAt(post.getPostedAt().toString());
        dto.setEditedAt(post.getEditedAt() == null ? null : post.getEditedAt().toString());
        dto.setViews(post.getViews() == null ? 0 : post.getViews());
        Set<PostLikes> postLikes = post.getPostLikes();
        dto.setUserLikedPost(postLikes.stream().map(pl -> pl.getUsers().getUserId()).collect(Collectors.toList()));
        dto.setLikesQuantity(postLikes.size());
        dto.setCommentsQuantity(post.getUserComments().size());
        java.util.Map<String, Long> counts = new java.util.LinkedHashMap<>();
        String mine = null;
        for (PostLikes pl : postLikes) {
            String t = normReaction(pl.getType());
            counts.merge(t, 1L, Long::sum);
            if (meId != null && pl.getUsers() != null && meId.equals(pl.getUsers().getUserId())) mine = t;
        }
        dto.setReactionCounts(counts);
        dto.setMyReaction(mine);
        return dto;
    }

    // Dùng cho feed / tìm kiếm: KHÔNG dựng danh sách bình luận (tránh N+1 trên comment likes)
    private PostDTO toListDTO(Posts post, String meId) {
        return basePostDTO(post, meId);
    }

    @Override
    protected Object convertToDTO(Object object) {
        if (!(object instanceof Posts)){
            return null;
        }
        Posts post = (Posts) object;
        String meId = currentUserOrNull();
        PostDTO postDTO = basePostDTO(post, meId);
        Set<UserComment> userComments = post.getUserComments();
        List<CommentDTO> commentDTOs = new ArrayList<>();
        for (UserComment userComment : userComments){
            Comments comment = userComment.getComments();
            CommentDTO commentDTO = new CommentDTO();
            commentDTO.setCommentId(comment.getCommentId());
            commentDTO.setUserComments(userComment.getUsers().getUserId());
            Users cAuthor = userComment.getUsers();
            if (cAuthor != null) {
                commentDTO.setAuthorName(cAuthor.getFullName());
                commentDTO.setAuthorAvatar(cAuthor.getAvtUrl());
            }
            commentDTO.setContent(comment.getContent());
            commentDTO.setCommentImg(comment.getCommentImg());
            commentDTO.setCommentAt(comment.getCommentAt().toString());
            commentDTO.setEditedAt(comment.getEditedAt() == null ? null : comment.getEditedAt().toString());
            commentDTO.setParentId(comment.getParentId());
            Set<CommentLikes> commentLikes = comment.getCommentLikes();
            commentDTO.setCommentLikes(commentLikes.size());
            List<String> userLikes = commentLikes.stream().map(commentLike -> commentLike.getUsers().getUserId()).collect(Collectors.toList());
            commentDTO.setUserLikes(userLikes);
            java.util.Map<String, Long> cReact = new java.util.LinkedHashMap<>();
            String myCommentReaction = null;
            for (CommentLikes cl : commentLikes) {
                String t = normReaction(cl.getType());
                cReact.merge(t, 1L, Long::sum);
                if (meId != null && cl.getUsers() != null && meId.equals(cl.getUsers().getUserId())) myCommentReaction = t;
            }
            commentDTO.setReactionCounts(cReact);
            commentDTO.setMyReaction(myCommentReaction);
            commentDTOs.add(commentDTO);
        }
        commentDTOs.sort(Comparator.comparing(CommentDTO::getCommentAt).reversed());
        postDTO.setComments(commentDTOs);

        return postDTO;
    }
}
