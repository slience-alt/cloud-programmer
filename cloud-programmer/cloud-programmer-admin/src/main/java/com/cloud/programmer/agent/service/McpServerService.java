package com.cloud.programmer.agent.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cloud.programmer.agent.dto.McpDTO;
import com.cloud.programmer.agent.entity.McpServerEntity;

import java.util.List;
import java.util.Map;

public interface McpServerService {

    List<McpServerEntity> list();

    McpServerEntity getById(Long id);

    boolean update(McpDTO mcpDTO);

    boolean delete(Long id);

    boolean updateEnabled(Long id, Boolean enabled);

    List<McpDTO> getMcpServerList(Map<String, Object> params);

    void insert(McpDTO mcpDTO);
}