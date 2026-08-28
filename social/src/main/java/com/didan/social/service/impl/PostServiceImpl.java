package com.didan.social.service.impl;

import com.didan.social.dto.*;
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
                       com.didan.social.repository.RepostRepository repostRepository
    ){
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
        return postId.toString();
    }

    @Override
    public List<PostDTO> getAllPosts() throws Exception {
        List<Posts> posts = postRepository.findAllPost();
        if (posts == null) {
            logger.info("No posts are here");
            return Collections.emptyList();
        }
        String meId = currentUserOrNull();
        java.util.Set<String> ex = blockRelatedIds(meId);
        return posts.stream()
                .filter(p -> ex.isEmpty() || !ex.contains(p.getUserPost().getUsers().getUserId()))
                .map(post -> toListDTO(post, meId)).collect(Collectors.toList());
    }

    @Override
    public List<PostDTO> getAllPostsByPage(int index) throws Exception {
        if (index < 1) index = 1;
        String meId = currentUserOrNull();
        java.util.Collection<String> ex = feedExcludeParam(meId);
        List<Object[]> rows = postRepository.feedPage(ex, 10, (index - 1) * 10);
        if (rows.isEmpty()) {
            logger.info("No posts are here");
            return Collections.emptyList();
        }
        java.util.List<String> pidOrder = new ArrayList<>();
        java.util.List<String> reposterOrder = new ArrayList<>();
        java.util.List<String> timeOrder = new ArrayList<>();
        for (Object[] r : rows) {
            pidOrder.add((String) r[0]);
            timeOrder.add(r[1] == null ? null : r[1].toString());
            reposterOrder.add(r[2] == null ? null : (String) r[2]);
        }
        java.util.Map<String, Posts> byId = new java.util.HashMap<>();
        for (Posts p : postRepository.findAllById(new java.util.LinkedHashSet<>(pidOrder))) byId.put(p.getPostId(), p);
        java.util.Set<String> reposterIds = reposterOrder.stream().filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        java.util.Map<String, Users> reposters = new java.util.HashMap<>();
        if (!reposterIds.isEmpty()) {
            for (Users u : userRepository.findAllById(reposterIds)) reposters.put(u.getUserId(), u);
        }
        List<PostDTO> out = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Posts p = byId.get(pidOrder.get(i));
            if (p == null) continue;
            PostDTO d = toListDTO(p, meId);
            String rid = reposterOrder.get(i);
            if (rid != null) {
                Users ru = reposters.get(rid);
                d.setRepostedBy(ru != null ? ru.getFullName() : rid);
                d.setRepostedById(rid);
                d.setRepostedAt(timeOrder.get(i));
            }
            out.add(d);
        }
        applyRepostInfo(out, meId);
        return out;
    }

    // Tập user id loại khỏi feed cho native NOT IN (phải khác rỗng -> sentinel "-")
    private java.util.Collection<String> feedExcludeParam(String meId) {
        java.util.Set<String> ex = blockRelatedIds(meId);
        return ex.isEmpty() ? java.util.List.of("-") : ex;
    }

    @Override
    public java.util.Map<String, Object> feedPageInfo() throws Exception {
        int pageSize = 10;
        long total = postRepository.feedCount(feedExcludeParam(currentUserOrNull()));
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
        PostDTO dto = (PostDTO) convertToDTO(post);
        applyRepostInfo(java.util.Collections.singletonList(dto), currentUserOrNull());
        return dto;
    }

    @Override
    public List<PostDTO> getMyDrafts() throws Exception {
        String meId = authorizePathService.getUserIdAuthoried();
        List<Posts> drafts = postRepository.findDraftsOfAuthor(meId);
        return drafts.stream().map(p -> toListDTO(p, meId)).collect(Collectors.toList());
    }

    @Override
    public List<PostDTO> getRepostsOf(String userId) throws Exception {
        java.util.List<com.didan.social.entity.Reposts> rows = repostRepository.findByRepostId_UserIdOrderByCreatedAtDesc(userId);
        if (rows.isEmpty()) return Collections.emptyList();
        String meId = currentUserOrNull();
        Users sharer = userRepository.findFirstByUserId(userId);
        java.util.List<String> pids = rows.stream().map(r -> r.getRepostId().getPostId()).collect(Collectors.toList());
        java.util.Map<String, Posts> byId = new java.util.HashMap<>();
        for (Posts p : postRepository.findAllById(pids)) byId.put(p.getPostId(), p);
        List<PostDTO> out = new ArrayList<>();
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
        return out;
    }

    @Transactional
    @Override
    public boolean repost(String postId, String note) throws Exception {
        String meId = authorizePathService.getUserIdAuthoried();
        Posts post = postRepository.findFirstByPostId(postId);
        if (post == null) throw new Exception("Không tìm thấy bài viết");
        String st = post.getStatus();
        if (st != null && !"published".equals(st)) throw new Exception("Không thể chia sẻ bài viết này");
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
        PageRequest pr = PageRequest.of(page, size);
        List<Posts> posts = ex.isEmpty()
                ? postRepository.searchByKeyword(searchName.trim(), pr)
                : postRepository.searchByKeywordExcludingAuthors(searchName.trim(), ex, pr);
        if (posts.isEmpty()) {
            logger.info("No posts are here");
            return Collections.emptyList();
        }
        List<PostDTO> out = posts.stream().map(post -> toListDTO(post, meId)).collect(Collectors.toList());
        applyRepostInfo(out, meId);
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
        if (editPostRequest.getPostImg()!= null && !editPostRequest.getPostImg().isEmpty()){
            if(StringUtils.hasText(post.getPostImg())){
                fileUploadsService.deleteFile(post.getPostImg());
            }
            String fileName = fileUploadsService.storeFile(editPostRequest.getPostImg(), "post", postId);
            post.setPostImg("post/"+fileName);
        }
        postRepository.save(post);
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
