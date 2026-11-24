package com.cloud.programmer.user.model.vo;

import lombok.Data;

/**
 * @author 周梓强 2026/9/23
 */

@Data
public class PluginVO {
    private Integer id;
    private String name;
    private Integer type;
    private String content;
    private Boolean isSale;
    private Boolean isCustom;
}
