package com.cloud.programmer.enums;

/**
 * @author 周梓强 2026/10/27
 */



public enum McpTransportTypeEnum {
    STREAMABLE_HTTP(1001, "网络连接", "STREAMABLE_HTTP"),
    STDIO(1002, "本地进程", "STDIO"),;


    private final String name;
    private final Integer id;
    private final String type;

    McpTransportTypeEnum(int id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }


    public String getName() {
        return name;
    }

    public Integer getId() {
        return id;
    }

    public String getType() {
        return type;
    }
}
