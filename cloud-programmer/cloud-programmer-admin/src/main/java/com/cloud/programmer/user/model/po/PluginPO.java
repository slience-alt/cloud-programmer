package com.cloud.programmer.user.model.po;

import lombok.Data;

/**
 * @author 周梓强 2026/9/23
 */

@Data
public class PluginPO {
    private String name;
    private Integer type;
    private String content;
    private Boolean isCustom;
}
