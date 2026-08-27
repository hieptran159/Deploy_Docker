package com.didan.social.controller;

import com.didan.social.payload.ResponseData;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

/**
 * Lỗi vượt kích thước upload xảy ra khi Spring phân giải multipart, TRƯỚC khi vào
 * controller, nên try/catch trong controller không bắt được. Bắt tại đây và trả về
 * đúng dạng ResponseData để FE hiển thị qua e?.description.
 */
@RestControllerAdvice
public class UploadExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ResponseData> handleTooLarge(MaxUploadSizeExceededException ex) {
        ResponseData payload = new ResponseData();
        payload.setSuccess(false);
        payload.setStatusCode(500);
        payload.setDescription("Ảnh vượt quá dung lượng cho phép. Vui lòng chọn ảnh nhỏ hơn.");
        return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ResponseData> handleMultipart(MultipartException ex) {
        ResponseData payload = new ResponseData();
        payload.setSuccess(false);
        payload.setStatusCode(500);
        payload.setDescription("Tải tệp lên thất bại. Vui lòng thử lại với ảnh png/jpg/jpeg.");
        return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
