package com.cloud.programmer.runtime.client;

import com.cloud.programmer.runtime.entity.McpServerEntity;

import java.util.List;

public interface AdminMcpClient {

    List<McpServerEntity> findEnabledServers();

    McpServerEntity findByServerId(Long serverId);
}