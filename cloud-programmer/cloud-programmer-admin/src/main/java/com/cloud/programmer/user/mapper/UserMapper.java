package com.cloud.programmer.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloud.programmer.user.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author 周梓强 2026/9/22
 */

@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {

}
