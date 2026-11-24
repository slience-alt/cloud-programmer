package com.cloud.programmer.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.programmer.agent.entity.McpServerEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface McpServerMapper extends BaseMapper<McpServerEntity> {
}