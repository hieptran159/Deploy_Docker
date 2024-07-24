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
    @Autowired
    public CommentServiceImpl(UserCommentRepository userCommentRepository,
                              CommentRepository commentRepository,
                              CommentLikeRepository commentLikeRepository,
                              UserRepository userRepository,
                              FileUploadsService fileUploadsService,
                              AuthorizePathService authorizePathService,
                              PostRepository postRepository){
        this.userCommentRepository = userCommentRepository;
        this.commentRepository = commentRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.userRepository = userRepository;
        this.fileUploadsService = fileUploadsService;
        this.authorizePathService = authorizePathService;
        this.postRepository = postRepository;
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

    @Transactional
    @Override
    public String postCommentInPost(String postId, CreateCommentRequest createCommentRequest) throws Exception {
        if (!StringUtils.hasText(createCommentRequest.getContent())){
            logger.error("Miss some fields");
            throw new Exception("Miss some fields");
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
        Comments comment = new Comments();
        UserComment userComment = new UserComment();
        UUID commentId = UUID.randomUUID();
        comment.setCommentId(commentId.toString());
        comment.setContent(createCommentRequest.getContent());
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
    public boolean likeComment(String commentId) throws Exception {
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
        CommentLikes commentLike = comment.getCommentLikes().stream().filter(cmtLike -> cmtLike.getUsers().getUserId().equals(user.getUserId())).findFirst().orElse(null);
//        CommentLikes commentLike = commentLikeRepository.findFirstByUsers_UserIdAndComments_CommentId(user.getUserId(), commentId);
        if (commentLike != null) {
            logger.error("User liked comment and cannot do it twice");
            throw new Exception("User liked comment and cannot do it twice");
        }
        CommentLikes newCommentLike = new CommentLikes();
        newCommentLike.setCommentLikeId(new CommentLikeId(commentId, user.getUserId()));
        newCommentLike.setUsers(user);
        newCommentLike.setComments(comment);
        commentLikeRepository.save(newCommentLike);
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
        List<CommentLikes> commentLikes = commentLikeRepository.findAllByComments_CommentId(comment.getCommentId());
        commentDTO.setCommentLikes(commentLikes.size());
        List<String> userLikes = commentLikes.stream().map(commentLike -> commentLike.getUsers().getUserId()).collect(Collectors.toList());
        commentDTO.setUserLikes(userLikes);
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
        userCommentRepository.delete(userComment);
        if(StringUtils.hasText(comment.getCommentImg())){
            fileUploadsService.deleteFile(comment.getCommentImg());
        }
        commentRepository.delete(comment);
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
        List<CommentLikes> commentLikes = commentLikeRepository.findAllByComments_CommentId(userComment.getComments().getCommentId());
        commentDTO.setCommentLikes(commentLikes.size());
        List<String> userLikes = commentLikes.stream().map(commentLike -> commentLike.getUsers().getUserId()).collect(Collectors.toList());
        commentDTO.setUserLikes(userLikes);
        return commentDTO;
    }
}