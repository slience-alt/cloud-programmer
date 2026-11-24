package com.cloud.programmer.runtime.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.programmer.runtime.entity.ConversationMemoryEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @author 周梓强 2026/10/21
 */

@Mapper
public interface ConversationMemoryMapper extends BaseMapper<ConversationMemoryEntity> {

    @Select("""
        SELECT COALESCE(MAX(sequence_no), 0)
        FROM t_agent_message
        WHERE conversation_id = #{conversationId}
        """)
    Integer selectMaxSequenceNo(@Param("conversationId") String conversationId);
}
