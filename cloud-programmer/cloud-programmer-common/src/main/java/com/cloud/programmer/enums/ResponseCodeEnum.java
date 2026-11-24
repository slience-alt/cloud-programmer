package com.cloud.programmer.enums;

import com.cloud.programmer.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author: 犬小哈
 * @url: www.quanxiaoha.com
 * @date: 2023-08-15 10:33
 * @description: 响应异常码
 **/
@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {

    // ----------- 通用异常状态码 -----------
    SYSTEM_ERROR(10000, "出错啦，后台小哥正在努力修复中..."),
    PARAM_NOT_VALID(10001, "参数错误"),
    SKILL_IS_USED(10002, "SKill已被关联"),
    MCP_IS_USED(10003, "MCP已被关联"),
    TASK_IS_CONCELED(10004, "Agent 任务已取消"),
    USER_NO_LOGIN(10005, "用户未登录"),
    DATA_NOT_FOUND(10006, "数据未找到"),
    USER_IS_EXIST(10007, "用户已存在"),
    USER_LOGIN_ERROR(10008, "用户登录错误");




    // ----------- 业务异常状态码 -----------

    // 异常码
    private Integer errorCode;
    // 错误信息
    private String errorMessage;

}

