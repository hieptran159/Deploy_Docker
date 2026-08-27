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
    @Autowired
    public PostServiceImpl(PostRepository postRepository,
                       UserPostRepository userPostRepository,
                       FileUploadsServiceImpl fileUploadsService,
                       UserRepository userRepository,
                       PostLikeRepository postLikeRepository,
                       CommentRepository commentRepository,
                       AuthorizePathService authorizePathService,
                       com.didan.social.service.NotificationService notificationService
    ){
        this.postRepository = postRepository;
        this.userPostRepository = userPostRepository;
        this.fileUploadsService = fileUploadsService;
        this.userRepository = userRepository;
        this.postLikeRepository = postLikeRepository;
        this.commentRepository =commentRepository;
        this.authorizePathService = authorizePathService;
        this.notificationService = notificationService;
    }
    @Transactional
    @Override
    public String createPost(CreatePostRequest createPostRequest) throws Exception {
        String userId = authorizePathService.getUserIdAuthoried();
        if (!StringUtils.hasText(createPostRequest.getTitle())
                || !StringUtils.hasText(createPostRequest.getBody())
        ){
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
        post.setTitle(createPostRequest.getTitle());
        if (createPostRequest.getPostImg() != null && !createPostRequest.getPostImg().isEmpty()){
            String fileName = fileUploadsService.storeFile(createPostRequest.getPostImg(), "post", postId.toString());
            post.setPostImg("post/"+fileName);
        }
        post.setBody(createPostRequest.getBody());
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
        return posts.stream().map(post -> toListDTO(post, meId)).collect(Collectors.toList());
    }

    @Override
    public List<PostDTO> getAllPostsByPage(int index) throws Exception {
        if (index < 1) index = 1;
        PageRequest pageRequest = PageRequest.of(index - 1, 10);
        List<Posts> posts = postRepository.findAllPostByCommentAtOrPostAt(pageRequest);
        if (posts == null || posts.isEmpty()) {
            logger.info("No posts are here");
            return Collections.emptyList();
        }
        String meId = currentUserOrNull();
        return posts.stream().map(post -> toListDTO(post, meId)).collect(Collectors.toList());
    }

    @Override
    public java.util.Map<String, Object> feedPageInfo() throws Exception {
        int pageSize = 10;
        long total = postRepository.count();
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
        return (PostDTO) convertToDTO(post);
    }

    @Override
    public List<PostDTO> getPostByTitle(String searchName) throws Exception {
        List<Posts> posts = postRepository.findByTitleOrBodyContainingOrderByPostedAtDesc(searchName, searchName);
        if (posts.isEmpty()) {
            logger.info("No posts are here");
            return Collections.emptyList();
        }
        String meId = currentUserOrNull();
        return posts.stream().map(post -> toListDTO(post, meId)).collect(Collectors.toList());
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
        dto.setUserCreatedPost(post.getUserPost().getUserPostId().getUserId());
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
