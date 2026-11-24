package com.cloud.programmer.exception;


import com.cloud.programmer.enums.ResponseCodeEnum;
import com.cloud.programmer.result.Response;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.Optional;

/**
 * @author: 犬小哈
 * @url: www.quanxiaoha.com
 * @date: 2023-08-15 10:14
 * @description: 全局异常处理
 **/
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获自定义业务异常
     * @return
     */
    @ExceptionHandler({ BizException.class })
    @ResponseBody
    public Response<Object> handleBizException(HttpServletRequest request, BizException e) {
        log.warn("{} request fail, errorCode: {}, errorMessage: {}", request.getRequestURI(), e.getErrorCode(), e.getErrorMessage());
        return Response.fail(e);
    }

    /**
     * 捕获参数校验异常
     * @return
     */
    @ExceptionHandler({ MethodArgumentNotValidException.class })
    @ResponseBody
    public Response<Object> handleMethodArgumentNotValidException(HttpServletRequest request, MethodArgumentNotValidException e) {
        // 参数错误异常码
        Integer errorCode = ResponseCodeEnum.PARAM_NOT_VALID.getErrorCode();

        // 获取 BindingResult
        BindingResult bindingResult = e.getBindingResult();

        StringBuilder sb = new StringBuilder();

        // 获取校验不通过的字段，并组合错误信息，格式为： email 邮箱格式不正确, 当前值: '123124qq.com';
        Optional.ofNullable(bindingResult.getFieldErrors()).ifPresent(errors -> {
            errors.forEach(error ->
                    sb.append(error.getField())
                            .append(" ")
                            .append(error.getDefaultMessage())
                            .append(", 当前值: '")
                            .append(error.getRejectedValue())
                            .append("'; ")

            );
        });

        // 错误信息
        String errorMessage = sb.toString();

        log.warn("{} request error, errorCode: {}, errorMessage: {}", request.getRequestURI(), errorCode, errorMessage);

        return Response.fail(errorCode, errorMessage);
    }


    /**
     * 其他类型异常
     * @param request
     * @param e
     * @return
     */
    /**
     * 其他类型异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Object handleOtherException(
            HttpServletRequest request,
            Exception e) {

        // SSE 请求的客户端可能已经断开，不能再返回普通 JSON
        if (isSseRequest(request) && isClientDisconnect(e)) {
            log.debug("SSE 客户端已断开，URI={}", request.getRequestURI());
            return null;
        }

        log.error("{} request error", request.getRequestURI(), e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Response.fail(ResponseCodeEnum.SYSTEM_ERROR));
    }

    /**
     * 判断是否为 SSE 请求
     */
    private boolean isSseRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String accept = request.getHeader("Accept");
        return (uri != null && uri.contains("/stream")) || (accept != null && accept.contains("text/event-stream"));
    }

    /**
     * 判断是否为客户端断开连接
     */
    private boolean isClientDisconnect(Throwable e) {
        Throwable current = e;

        while (current != null) {
            if (current instanceof IOException) {
                String message = current.getMessage();

                if (message != null &&
                        (message.contains("你的主机中的软件中止了一个已建立的连接")
                                || message.contains("Connection reset by peer")
                                || message.contains("Broken pipe"))) {
                    return true;
                }
            }

            current = current.getCause();
        }

        return false;
    }
}

