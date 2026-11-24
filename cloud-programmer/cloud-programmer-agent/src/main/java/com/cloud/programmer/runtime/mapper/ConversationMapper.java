package com.cloud.programmer.runtime.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.programmer.runtime.entity.ConversationEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConversationMapper extends BaseMapper<ConversationEntity> {
}