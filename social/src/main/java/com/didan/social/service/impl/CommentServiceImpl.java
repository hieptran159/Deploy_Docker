package com.didan.social.service.impl;

import com.didan.social.dto.CommentDTO;
import com.didan.social.dto.PostDTO;
import com.didan.social.entity.*;
import com.didan.social.entity.keys.CommentLikeId;
import com.didan.social.entity.keys.UserCommentId;
import com.didan.social.payload.request.CreateCommentRequest;
import com.didan.social.payload.request.EditCommentRequest;
import com.didan.social.repository.*;
import com.didan.social.service.AuthorizePathService;
import com.didan.social.service.CommentService;
import com.didan.social.service.FileUploadsService;
import com.didan.social.service.convertdto.ConvertDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl extends ConvertDTO implements CommentService {
    private final Logger logger = LoggerFactory.getLogger(CommentServiceImpl.class);
    private final UserCommentRepository userCommentRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final FileUploadsService fileUploadsService;
    private final AuthorizePathService authorizePathService;
    private final UserPostRepository userPostRepository;
    private final com.didan.social.service.NotificationService notificationService;
    private final com.didan.social.socket.RealtimeGateway realtimeGateway;
    @Autowired
    public CommentServiceImpl(UserCommentRepository userCommentRepository,
                              CommentRepository commentRepository,
                              CommentLikeRepository commentLikeRepository,
                              UserRepository userRepository,
                              FileUploadsService fileUploadsService,
                              AuthorizePathService authorizePathService,
                              PostRepository postRepository,
                              UserPostRepository userPostRepository,
                              com.didan.social.service.NotificationService notificationService,
                              com.didan.social.socket.RealtimeGateway realtimeGateway){
        this.userCommentRepository = userCommentRepository;
        this.commentRepository = commentRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.userRepository = userRepository;
        this.fileUploadsService = fileUploadsService;
        this.authorizePathService = authorizePathService;
        this.postRepository = postRepository;
        this.userPostRepository = userPostRepository;
        this.notificationService = notificationService;
        this.realtimeGateway = realtimeGateway;
    }

    // báo cho mọi người đang mở bài viết biết danh sách bình luận vừa đổi
    private void broadcastCommentsChanged(String postId) {
        if (postId == null) return;
        try {
            java.util.Map<String, Object> p = new java.util.HashMap<>();
            p.put("postId", postId);
            realtimeGateway.toRoom("post:" + postId, "post_comments_changed", p);
        } catch (Exception ignore) { /* realtime là phụ */ }
    }

    @Override
    public List<CommentDTO> getCommentsInPost(String postId) throws Exception {
        List<UserComment> userComments = userCommentRepository.findByPosts_PostId(postId);
        if(userComments == null) return Collections.emptyList();
        List<CommentDTO> commentDTOs = new ArrayList<>();
        for (UserComment userComment : userComments){
            CommentDTO commentDTO = (CommentDTO) convertToDTO(userComment);
            commentDTOs.add(commentDTO);
        }
        commentDTOs.sort(Comparator.comparing(CommentDTO::getCommentAt).reversed());
        return commentDTOs;
    }

    @Override
    public java.util.Map<String, Object> getCommentsPage(String postId, int page, int size) throws Exception {
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 20;
        List<CommentDTO> all = getCommentsInPost(postId); // đã kèm reaction, sắp mới -> cũ

        // gom cây 1 cấp
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (CommentDTO c : all) ids.add(c.getCommentId());
        List<CommentDTO> roots = new ArrayList<>();
        java.util.Map<String, List<CommentDTO>> children = new java.util.HashMap<>();
        for (CommentDTO c : all) {
            String pid = c.getParentId();
            if (pid != null && ids.contains(pid)) {
                children.computeIfAbsent(pid, k -> new ArrayList<>()).add(c);
            } else {
                roots.add(c);
            }
        }
        int total = roots.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) size));
        int from = Math.min((page - 1) * size, total);
        int to = Math.min(from + size, total);

        List<CommentDTO> items = new ArrayList<>();
        for (CommentDTO root : roots.subList(from, to)) {
            items.add(root);
            List<CommentDTO> reps = children.get(root.getCommentId());
            if (reps != null) {
                reps.sort(Comparator.comparing(CommentDTO::getCommentAt)); // trả lời: cũ -> mới
                items.addAll(reps);
            }
        }

        java.util.Map<String, Object> out = new java.util.HashMap<>();
        out.put("items", items);
        out.put("total", total);          // số bình luận gốc
        out.put("totalPages", totalPages);
        out.put("page", page);
        return out;
    }

    @Transactional
    @Override
    public String postCommentInPost(String postId, CreateCommentRequest createCommentRequest) throws Exception {
        boolean hasImg = createCommentRequest.getCommentImg() != null && !createCommentRequest.getCommentImg().isEmpty();
        if (!StringUtils.hasText(createCommentRequest.getContent()) && !hasImg){
            logger.error("Miss some fields");
            throw new Exception("Bình luận phải có nội dung hoặc hình ảnh");
        }
        String userId = authorizePathService.getUserIdAuthoried();
        Users user = userRepository.findFirstByUserId(userId);
        if (user == null) {
            logger.error("User is not found");
            throw new Exception("User is not found");
        }
        Posts post = postRepository.findFirstByPostId(postId);
        if (post == null) {
            logger.error("No post is here");
            throw new Exception("No post is here");
        }
        // trả lời bình luận: chuẩn hoá về tối đa 1 cấp
        String parentId = null;
        String parentAuthorId = null;
        if (StringUtils.hasText(createCommentRequest.getParentId())) {
            Comments parent = commentRepository.findByCommentId(createCommentRequest.getParentId().trim());
            UserComment parentUc = parent == null ? null
                    : userCommentRepository.findFirstByComments_CommentId(parent.getCommentId());
            if (parent != null && parentUc != null && parentUc.getPosts() != null
                    && postId.equals(parentUc.getPosts().getPostId())) {
                // nếu cha cũng là 1 câu trả lời -> gắn vào bình luận gốc của nó
                parentId = StringUtils.hasText(parent.getParentId()) ? parent.getParentId() : parent.getCommentId();
                parentAuthorId = parentUc.getUsers() != null ? parentUc.getUsers().getUserId() : null;
            }
        }

        Comments comment = new Comments();
        UserComment userComment = new UserComment();
        UUID commentId = UUID.randomUUID();
        comment.setCommentId(commentId.toString());
        comment.setParentId(parentId);
        comment.setContent(StringUtils.hasText(createCommentRequest.getContent()) ? createCommentRequest.getContent() : "");
        if (createCommentRequest.getCommentImg() != null && !createCommentRequest.getCommentImg().isEmpty()){
            String fileName = fileUploadsService.storeFile(createCommentRequest.getCommentImg(), "comment", commentId.toString());
            comment.setCommentImg("comment/"+fileName);
        }
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        Date nowSql = Timestamp.valueOf(now);
        comment.setCommentAt(nowSql);
        commentRepository.save(comment);
        userComment.setUserCommentId(new UserCommentId(postId, comment.getCommentId(), user.getUserId()));
        userCommentRepository.save(userComment);
        String newCommentId = comment.getCommentId();
        UserPosts owner = userPostRepository.findFirstByPosts_PostId(postId);
        if (owner != null && owner.getUsers() != null) {
            notificationService.push(owner.getUsers().getUserId(), user.getUserId(), "COMMENT", postId,
                    newCommentId, user.getFullName() + " đã bình luận bài viết của bạn");
        }
        // báo cho chủ bình luận được trả lời
        if (parentAuthorId != null) {
            notificationService.pushUniquePerActor(parentAuthorId, user.getUserId(), "REPLY", postId,
                    newCommentId, user.getFullName() + " đã trả lời bình luận của bạn");
        }
        // @nhắc tên: tách các token dạng @[Tên](userId) trong nội dung
        try {
            String body = createCommentRequest.getContent();
            if (body != null) {
                java.util.regex.Matcher mt = java.util.regex.Pattern
                        .compile("@\\[[^\\]]+\\]\\(([0-9a-fA-F\\-]{8,})\\)")
                        .matcher(body);
                java.util.Set<String> mentioned = new java.util.HashSet<>();
                while (mt.find()) mentioned.add(mt.group(1));
                for (String uid : mentioned) {
                    notificationService.pushUniquePerActor(uid, user.getUserId(), "MENTION", postId,
                            newCommentId, user.getFullName() + " đã nhắc đến bạn trong một bình luận");
                }
            }
        } catch (Exception ex) {
            logger.error("mention parse failed: " + ex.getMessage());
        }
        broadcastCommentsChanged(postId);
        return commentId.toString();
    }

    @Override
    public CommentDTO getCommentById(String commentId) throws Exception {
        UserComment userComment = userCommentRepository.findFirstByComments_CommentId(commentId);
        if (userComment == null) return null;
        return (CommentDTO) convertToDTO(userComment);
    }

    @Transactional
    @Override
    public boolean likeComment(String commentId, String type) throws Exception {
        String userId = authorizePathService.getUserIdAuthoried();
        Users user = userRepository.findFirstByUserId(userId);
        if (user == null) {
            logger.error("User is not found");
            throw new Exception("User is not found");
        }
        Comments comment = commentRepository.findByCommentId(commentId);
        if (comment == null) {
            logger.error("No comment is here");
            throw new Exception("No comment is here");
        }
        String reaction = com.didan.social.service.impl.PostServiceImpl.normReaction(type);
        UserComment uc = userCommentRepository.findFirstByComments_CommentId(commentId);
        String likedPostId = (uc != null && uc.getPosts() != null) ? uc.getPosts().getPostId() : null;

        CommentLikes commentLike = commentLikeRepository.findFirstByUsers_UserIdAndComments_CommentId(user.getUserId(), commentId);
        if (commentLike != null) {
            // đã thả cảm xúc -> chỉ đổi loại
            commentLike.setType(reaction);
            commentLikeRepository.save(commentLike);
            broadcastCommentsChanged(likedPostId);
            return true;
        }
        CommentLikes newCommentLike = new CommentLikes();
        newCommentLike.setCommentLikeId(new CommentLikeId(commentId, user.getUserId()));
        newCommentLike.setUsers(user);
        newCommentLike.setComments(comment);
        newCommentLike.setType(reaction);
        commentLikeRepository.save(newCommentLike);
        if (uc != null && uc.getUsers() != null) {
            notificationService.pushUniquePerActor(uc.getUsers().getUserId(), user.getUserId(), "COMMENT_LIKE", likedPostId,
                    commentId, user.getFullName() + " đã bày tỏ cảm xúc về bình luận của bạn");
        }
        broadcastCommentsChanged(likedPostId);
        return true;
    }

    @Transactional
    @Override
    public boolean unlikeComment(String commentId) throws Exception {
        String userId = authorizePathService.getUserIdAuthoried();
        Users user = userRepository.findFirstByUserId(userId);
        if (user == null) {
            logger.error("User is not found");
            throw new Exception("User is not found");
        }
        Comments comment = commentRepository.findByCommentId(commentId);
        if (comment == null) {
            logger.error("No comment is here");
            throw new Exception("No comment is here");
        }
        CommentLikes commentLike = commentLikeRepository.findFirstByUsers_UserIdAndComments_CommentId(userId, commentId);
        if (commentLike == null) {
            logger.error("User hasn't liked post yet");
            throw new Exception("User hasn't liked post yet");
        }
        commentLikeRepository.delete(commentLike);
        UserComment uc = userCommentRepository.findFirstByComments_CommentId(commentId);
        broadcastCommentsChanged(uc != null && uc.getPosts() != null ? uc.getPosts().getPostId() : null);
        return true;
    }

    @Override
    public CommentDTO updateComment(String commentId, EditCommentRequest editCommentRequest) throws Exception {
        String userId = authorizePathService.getUserIdAuthoried();
        Users user = userRepository.findFirstByUserId(userId);
        if (user == null) {
            logger.error("User is not found");
            throw new Exception("User is not found");
        }
        UserComment userComment = userCommentRepository.findFirstByUsers_UserIdAndComments_CommentId(user.getUserId(), commentId);
        if (userComment == null) {
            logger.error("The user hasn't this comment or not authorized to edit this comment");
            throw new Exception("The user hasn't this comment or not authorized to edit this comment");
        }
        Comments comment = userComment.getComments();
        if (StringUtils.hasText(editCommentRequest.getContent())){
            comment.setContent(editCommentRequest.getContent());
        }
        if (editCommentRequest.getCommentImg()!= null && !editCommentRequest.getCommentImg().isEmpty()){
            if(StringUtils.hasText(comment.getCommentImg())){
                fileUploadsService.deleteFile(comment.getCommentImg());
            }
            String fileName = fileUploadsService.storeFile(editCommentRequest.getCommentImg(), "comment", commentId);
            comment.setCommentImg("comment/"+fileName);
        }
        commentRepository.save(comment);
        CommentDTO commentDTO = new CommentDTO();
        commentDTO.setCommentId(comment.getCommentId());
        commentDTO.setUserComments(userComment.getUsers().getUserId());
        commentDTO.setContent(comment.getContent());
        commentDTO.setCommentImg(comment.getCommentImg());
        commentDTO.setCommentAt(comment.getCommentAt().toString());
        commentDTO.setParentId(comment.getParentId());
        List<CommentLikes> commentLikes = commentLikeRepository.findAllByComments_CommentId(comment.getCommentId());
        commentDTO.setCommentLikes(commentLikes.size());
        List<String> userLikes = commentLikes.stream().map(commentLike -> commentLike.getUsers().getUserId()).collect(Collectors.toList());
        commentDTO.setUserLikes(userLikes);
        broadcastCommentsChanged(userComment.getPosts() != null ? userComment.getPosts().getPostId() : null);
        return commentDTO;
    }

    @Override
    public boolean deleteComment(String commentId) throws Exception {
        String userId = authorizePathService.getUserIdAuthoried();
        Users user = userRepository.findFirstByUserId(userId);
        if (user == null) {
            logger.error("User is not found");
            throw new Exception("User is not found");
        }
        Comments comment = commentRepository.findByCommentId(commentId);
        if(comment == null) {
            logger.error("There is not comment to delete");
            throw new Exception("There is not comment to delete");
        }
        UserComment userComment = new UserComment();
        if (user.getIsAdmin() == 0){
            userComment = userCommentRepository.findFirstByUsers_UserIdAndComments_CommentId(user.getUserId(), commentId);
        } else {
            userComment = userCommentRepository.findFirstByComments_CommentId(commentId);
        }
        if (userComment == null) {
            logger.error("The user hasn't this post or not authorized to edit this post");
            throw new Exception("The user hasn't this post or not authorized to edit this post");
        }
        String deletedPostId = userComment.getPosts() != null ? userComment.getPosts().getPostId() : null;
        userCommentRepository.delete(userComment);
        if(StringUtils.hasText(comment.getCommentImg())){
            fileUploadsService.deleteFile(comment.getCommentImg());
        }
        commentRepository.delete(comment);
        broadcastCommentsChanged(deletedPostId);
        return true;
    }


    @Override
    protected Object convertToDTO(Object object) {
        if (!(object instanceof UserComment)){
            return null;
        }
        UserComment userComment = (UserComment) object;
        CommentDTO commentDTO = new CommentDTO();
        commentDTO.setCommentId(userComment.getComments().getCommentId());
        commentDTO.setUserComments(userComment.getUsers().getUserId());
        commentDTO.setContent(userComment.getComments().getContent());
        commentDTO.setCommentImg(userComment.getComments().getCommentImg());
        commentDTO.setCommentAt(userComment.getComments().getCommentAt().toString());
        commentDTO.setParentId(userComment.getComments().getParentId());
        List<CommentLikes> commentLikes = commentLikeRepository.findAllByComments_CommentId(userComment.getComments().getCommentId());
        commentDTO.setCommentLikes(commentLikes.size());
        List<String> userLikes = commentLikes.stream().map(commentLike -> commentLike.getUsers().getUserId()).collect(Collectors.toList());
        commentDTO.setUserLikes(userLikes);
        String meId = null;
        try { meId = authorizePathService.getUserIdAuthoried(); } catch (Exception ignore) { }
        java.util.Map<String, Long> rc = new java.util.LinkedHashMap<>();
        String mine = null;
        for (CommentLikes cl : commentLikes) {
            String t = com.didan.social.service.impl.PostServiceImpl.normReaction(cl.getType());
            rc.merge(t, 1L, Long::sum);
            if (meId != null && cl.getUsers() != null && meId.equals(cl.getUsers().getUserId())) mine = t;
        }
        commentDTO.setReactionCounts(rc);
        commentDTO.setMyReaction(mine);
        return commentDTO;
    }
}