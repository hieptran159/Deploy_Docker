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
import org.springframework.data.domain.Page;
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
        return posts.stream().map(post -> (PostDTO) convertToDTO(post)).collect(Collectors.toList());
    }

    @Override
    public List<PostDTO> getAllPostsByPage(int index) throws Exception {
        PageRequest pageRequest = PageRequest.of(index-1, 10);
        Page<Posts> posts = postRepository.findAllPostByCommentAtOrPostAt(pageRequest);
        if (posts == null) {
            logger.info("No posts are here");
            return Collections.emptyList();
        }
        return posts.getContent().stream().map(post -> (PostDTO) convertToDTO(post)).collect(Collectors.toList());
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
        return posts.stream().map(post -> (PostDTO) convertToDTO(post)).collect(Collectors.toList());
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

    @Override
    protected Object convertToDTO(Object object) {
        if (!(object instanceof Posts)){
            return null;
        }
        Posts post = (Posts) object;
        PostDTO postDTO = new PostDTO();
        UserPosts userPost = post.getUserPost();
        postDTO.setPostId(post.getPostId());
        postDTO.setUserCreatedPost(userPost.getUserPostId().getUserId());
        postDTO.setTitle(post.getTitle());
        postDTO.setPostImg(post.getPostImg());
        postDTO.setBody(post.getBody());
        postDTO.setPostedAt(post.getPostedAt().toString());
        Set<PostLikes> postLikes = post.getPostLikes();
        List<String> userLikedPosts = postLikes.stream().map(postLike -> postLike.getUsers().getUserId()).collect(Collectors.toList());
        postDTO.setUserLikedPost(userLikedPosts);
        postDTO.setLikesQuantity(postLikes.size());

        String meId = null;
        try { meId = authorizePathService.getUserIdAuthoried(); } catch (Exception ignore) { }
        java.util.Map<String, Long> pReact = new java.util.LinkedHashMap<>();
        String myPostReaction = null;
        for (PostLikes pl : postLikes) {
            String t = normReaction(pl.getType());
            pReact.merge(t, 1L, Long::sum);
            if (meId != null && pl.getUsers() != null && meId.equals(pl.getUsers().getUserId())) myPostReaction = t;
        }
        postDTO.setReactionCounts(pReact);
        postDTO.setMyReaction(myPostReaction);
        Set<UserComment> userComments = post.getUserComments();
        postDTO.setCommentsQuantity(userComments.size());
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
