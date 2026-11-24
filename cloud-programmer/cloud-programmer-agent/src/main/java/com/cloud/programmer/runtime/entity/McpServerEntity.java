
package com.cloud.programmer.runtime.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
public class McpServerEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private String description;

    /**
     * STREAMABLE_HTTP 或 STDIO
     */
    private String transportType;
    private String serverUrl;

    /**
     * STDIO 启动命令及配置
     */
    private String command;
    private String argsJson;
    private String envJson;

    /**
     * HTTP 请求头配置
     */
    private String headersJson;
    private Boolean enabled;
    private Integer connectTimeoutMs;
    private Integer requestTimeoutMs;
    private String remark;
    private Date createTime;
    private Date updateTime;


}
