package com.cloud.programmer.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cloud.programmer.agent.dto.McpDTO;
import com.cloud.programmer.agent.entity.McpServerEntity;
import com.cloud.programmer.agent.mapper.McpServerMapper;
import com.cloud.programmer.agent.service.McpServerService;
import com.cloud.programmer.enums.McpTransportTypeEnum;
import com.cloud.programmer.security.McpUrlValidator;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class McpServerServiceImpl implements McpServerService {

    private final McpServerMapper mcpServerMapper;

    private final Environment environment;

    public McpServerServiceImpl(McpServerMapper mcpServerMapper, Environment environment) {
        this.mcpServerMapper = mcpServerMapper;
        this.environment = environment;
    }

    @Override
    public List<McpServerEntity> list() {

        return mcpServerMapper.selectList(null);
    }

    @Override
    public McpServerEntity getById(Long id) {
        return mcpServerMapper.selectById(id);
    }


    @Override
    public boolean update(McpDTO mcpDTO) {
        if (mcpDTO.getId() == null) {
            throw new IllegalArgumentException("MCP Server ID不能为空");
        }

        validate(mcpDTO);
        McpServerEntity mcpServerEntity = buildEntity(mcpDTO);
        mcpServerEntity.setId(mcpDTO.getId());
        return mcpServerMapper.updateById(mcpServerEntity) == 1;
    }

    @Override
    public boolean delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("MCP Server ID不能为空");
        }
        return mcpServerMapper.deleteById(id) == 1;
    }

    @Override
    public boolean updateEnabled(Long id, Boolean enabled) {
        if (id == null || enabled == null) {
            throw new IllegalArgumentException("ID和启用状态不能为空");
        }

        McpServerEntity mcpServerEntity = mcpServerMapper.selectById(id);
        if (mcpServerEntity != null) {
            mcpServerEntity.setEnabled(enabled);
            return mcpServerMapper.updateById(mcpServerEntity) == 1;
        }
        return false;
    }

    @Override
    public List<McpDTO> getMcpServerList(Map<String, Object> params) {
        QueryWrapper<McpServerEntity> mcpServerEntityQueryWrapper = new QueryWrapper<>();
        if (params.get("name") != null) {
            mcpServerEntityQueryWrapper.like("name", params.get("name"));
        }
        if (params.get("enabled") != null) {
            mcpServerEntityQueryWrapper.eq("enabled", params.get("enabled"));
        }

        if (params.get("transportType") != null) {
            mcpServerEntityQueryWrapper.eq("transport_type", params.get("transportType"));
        }

        List<McpServerEntity> mcpServerEntities = mcpServerMapper.selectList(mcpServerEntityQueryWrapper);
        List<McpDTO> mcpDTOS = new ArrayList<>();
        for (McpServerEntity mcpServerEntity : mcpServerEntities) {
            McpDTO mcpDTO = getMcpDTO(mcpServerEntity);
            mcpDTOS.add(mcpDTO);
        }
        return mcpDTOS;
    }



    @Override
    public void insert(McpDTO mcpDTO) {
        McpServerEntity mcpServerEntity = buildEntity(mcpDTO);
        mcpServerMapper.insert(mcpServerEntity);
    }

    private void validate(McpDTO mcpDTO) {
        if (mcpDTO == null || !StringUtils.hasText(mcpDTO.getName())) {
            throw new IllegalArgumentException("MCP Server名称不能为空");
        }
        String transportType = mcpDTO.getTransportType();
        if (!"STREAMABLE_HTTP".equals(transportType) && !"STDIO".equals(transportType)) {
            throw new IllegalArgumentException("不支持的MCP传输类型");
        }
        if ("STREAMABLE_HTTP".equals(transportType)) {
            if (!StringUtils.hasText(mcpDTO.getServerUrl())) {
                throw new IllegalArgumentException("HTTP模式下服务地址不能为空");
            }
            McpUrlValidator.validate(mcpDTO.getServerUrl(), getTrustedMcpHosts());
        }

        if ("STDIO".equals(transportType) && !StringUtils.hasText(mcpDTO.getCommand())) {
            throw new IllegalArgumentException(
                    "STDIO模式下启动命令不能为空");
        }
    }


    private McpServerEntity buildEntity(McpDTO mcpDTO) {
        McpServerEntity mcpServerEntity = new McpServerEntity();
        mcpServerEntity.setName(mcpDTO.getName());
        mcpServerEntity.setDescription(mcpDTO.getDescription());
        mcpServerEntity.setTransportType(mcpDTO.getTransportType());
        mcpServerEntity.setServerUrl(mcpDTO.getServerUrl());
        mcpServerEntity.setHeadersJson(mcpDTO.getHeadersJson());
        mcpServerEntity.setCommand(mcpDTO.getCommand());
        mcpServerEntity.setEnvJson(mcpDTO.getEnvJson());
        mcpServerEntity.setArgsJson(mcpDTO.getArgsJson());
        mcpServerEntity.setEnvJson(String.valueOf(mcpDTO.getEnvJson()));
        mcpServerEntity.setRemark(mcpDTO.getRemark() != null ? mcpDTO.getRemark() : "");
        mcpServerEntity.setConnectTimeoutMs(10000);
        mcpServerEntity.setRequestTimeoutMs(30000);
        mcpServerEntity.setEnabled(true);
        mcpServerEntity.setCreateTime(new Date());
        mcpServerEntity.setUpdateTime(new Date());
        return mcpServerEntity;
    }

    private McpDTO getMcpDTO(McpServerEntity mcpServerEntity) {
        McpDTO mcpDTO = new McpDTO();
        mcpDTO.setId(mcpServerEntity.getId());
        mcpDTO.setName(mcpServerEntity.getName());
        mcpDTO.setDescription(mcpServerEntity.getDescription());
        mcpDTO.setTransportType(mcpServerEntity.getTransportType());
        mcpDTO.setEnabled(mcpServerEntity.getEnabled());
        mcpDTO.setRemark(mcpServerEntity.getRemark() != null ? mcpServerEntity.getRemark() : "");
        if (mcpServerEntity.getTransportType().equals(McpTransportTypeEnum.STREAMABLE_HTTP.getType())) {
            mcpDTO.setServerUrl(mcpServerEntity.getServerUrl());
            mcpDTO.setHeadersJson(mcpServerEntity.getHeadersJson());
        } else if (mcpServerEntity.getTransportType().equals(McpTransportTypeEnum.STDIO.getType())) {
            mcpDTO.setCommand(mcpServerEntity.getCommand());
            mcpDTO.setArgsJson(String.valueOf(mcpServerEntity.getArgsJson()));
            mcpDTO.setEnvJson(String.valueOf(mcpServerEntity.getEnvJson()));
        }
        return mcpDTO;
    }

    private List<String> getTrustedMcpHosts() {
        String hosts = environment.getProperty("security.mcp.trusted-hosts", String.class);
        if (!StringUtils.hasText(hosts)) {
            return new ArrayList<>();
        }
        return java.util.Arrays.asList(hosts.split(","));
    }

}