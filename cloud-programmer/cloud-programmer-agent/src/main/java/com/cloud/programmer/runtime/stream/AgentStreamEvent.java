package com.cloud.programmer.runtime.stream;

import java.time.Instant;

public class AgentStreamEvent {

    /**
     * 数据库事件 ID
     * 持久化后由数据库记录 ID 填充
     */
    private Long eventId;

    /**
     * 事件类型
     * token、tool_start、tool_result、done、error
     */
    private final String type;

    /**
     * 事件内容
     */
    private final String content;

    /**
     * 创建时间
     */
    private final Instant timestamp;

    public AgentStreamEvent(String type, String content) {
        this(null, type, content, Instant.now());
    }

    public AgentStreamEvent(Long eventId, String type, String content, Instant timestamp) {
        this.eventId = eventId;
        this.type = type;
        this.content = content;
        this.timestamp = timestamp;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}