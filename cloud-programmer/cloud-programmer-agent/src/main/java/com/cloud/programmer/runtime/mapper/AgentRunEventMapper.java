package com.cloud.programmer.runtime.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.programmer.runtime.entity.AgentRunEvent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AgentRunEventMapper extends BaseMapper<AgentRunEvent> {
}