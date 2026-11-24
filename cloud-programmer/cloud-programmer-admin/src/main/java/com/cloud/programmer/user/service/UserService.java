package com.cloud.programmer.user.service;

import cn.hutool.log.Log;
import com.cloud.programmer.user.entity.UserEntity;
import com.cloud.programmer.user.model.vo.LoginVO;
import com.cloud.programmer.user.model.vo.RegisterVO;
import com.cloud.programmer.user.model.vo.UserInfoVO;
import com.cloud.programmer.user.model.vo.UserVO;

import java.util.List;
import java.util.Map;

/**
 * @author 周梓强 2026/9/22
 */


public interface UserService {


    List<UserVO> list(Map<String, Object> params);

    boolean bannedUser(Integer id, Integer status);

    boolean addCredit(int creditNum, Integer id);

    boolean deletePastUser(List<Integer> ids);

    LoginVO login(LoginVO loginVO);

    LoginVO register(RegisterVO registerVO);

    UserInfoVO getUserById(Long currentUserId);
}
