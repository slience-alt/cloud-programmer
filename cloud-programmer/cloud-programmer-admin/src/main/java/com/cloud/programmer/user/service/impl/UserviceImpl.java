package com.cloud.programmer.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cloud.programmer.contant.EnableContant;
import com.cloud.programmer.enums.ResponseCodeEnum;
import com.cloud.programmer.exception.BizException;
import com.cloud.programmer.user.entity.UserEntity;
import com.cloud.programmer.enums.RoleEnum;
import com.cloud.programmer.user.mapper.UserMapper;
import com.cloud.programmer.user.model.vo.LoginVO;
import com.cloud.programmer.user.model.vo.RegisterVO;
import com.cloud.programmer.user.model.vo.UserInfoVO;
import com.cloud.programmer.user.model.vo.UserVO;
import com.cloud.programmer.user.service.JwtTokenService;
import com.cloud.programmer.user.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author 周梓强 2026/9/22
 */

@Service
public class UserviceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private JwtTokenService jwtTokenService;

    @Resource
    private PasswordEncoder passwordEncoder;



    @Override
    public List<UserVO> list(Map<String, Object> params) {
        QueryWrapper<UserEntity> userEntityQueryWrapper = new QueryWrapper<>();
        if (params.get("nickname") != null && !"".equals(params.get("nickname"))) {
            userEntityQueryWrapper.like("nickname", params.get("nickname"));
        }
        if (params.get("role") != null && params.get("role") != "") {
            userEntityQueryWrapper.eq("role", params.get("role"));
        }
        List<UserEntity> userEntities = userMapper.selectList(userEntityQueryWrapper);
        List<UserVO> userVOList = new ArrayList<>();
        for (UserEntity userEntity : userEntities) {
            UserVO userVO = new UserVO();
            userVO.setId(userEntity.getId());
            userVO.setNickname(userEntity.getNickname());
            userVO.setRole(userEntity.getRole());
            userVO.setCredit(userEntity.getCredit());
            userVO.setIsEnable(userEntity.getIsEnable());
            userVOList.add(userVO);
        }
        return userVOList;
    }

    @Override
    public boolean bannedUser(Integer id, Integer status) {
        UserEntity userEntity = userMapper.selectById(id);
        if (userEntity != null) {
            userEntity.setIsEnable(status == EnableContant.ENABLE);
        }
        int count = userMapper.updateById(userEntity);
        return count > 0;
    }

    @Override
    public boolean addCredit(int creditNum, Integer id) {
        UserEntity userEntity = userMapper.selectById(id);
        if (userEntity != null) {
            userEntity.setCredit(userEntity.getCredit() + creditNum);
        }
        int count = userMapper.updateById(userEntity);
        return count > 0;
    }

    @Override
    public boolean deletePastUser(List<Integer> ids) {
        int i = userMapper.deleteByIds(ids);
        return i > 0;
    }

    @Override
    public LoginVO login(LoginVO loginVO) {

        if (loginVO == null || loginVO.getUsername() == null || loginVO.getUsername().trim().isEmpty() || loginVO.getPassword() == null || loginVO.getPassword().isEmpty()) {
            throw new BizException(ResponseCodeEnum.USER_LOGIN_ERROR);
        }

        UserEntity user = userMapper.selectOne(
                Wrappers.<UserEntity>lambdaQuery()
                        .eq(UserEntity::getUsername, loginVO.getUsername().trim())
                        .eq(UserEntity::getIsDel, false)
        );

        // 用户不存在或密码不正确，统一返回登录失败
        if (user == null || !passwordEncoder.matches(loginVO.getPassword(), user.getPassword())) {
            throw new BizException(ResponseCodeEnum.USER_LOGIN_ERROR);
        }

        // 禁用用户不允许登录
        if (!Boolean.TRUE.equals(user.getIsEnable())) {
            throw new BizException(ResponseCodeEnum.USER_LOGIN_ERROR);
        }

        user.setLastLoginTime(new Date());
        userMapper.updateById(user);

        String token = jwtTokenService.generateToken(user.getId(), user.getUsername(), user.getRole());
        LoginVO result = new LoginVO();
        result.setAccessToken(token);
        result.setTokenType("Bearer");
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO register(RegisterVO registerVO) {

        // 1. 检查用户名是否存在
        Long count = userMapper.selectCount(
                Wrappers.<UserEntity>lambdaQuery()
                        .eq(UserEntity::getUsername, registerVO.getUsername())
        );

        if (count > 0) {
            throw new BizException(ResponseCodeEnum.USER_IS_EXIST);
        }

        // 2. 创建用户
        UserEntity user = new UserEntity();
        user.setUsername(registerVO.getUsername());
        user.setPassword(passwordEncoder.encode(registerVO.getPassword()));
        user.setNickname(registerVO.getNickname());
        user.setEmail(registerVO.getEmail());

        // 3. 强制设置普通用户角色
        user.setRole(RoleEnum.ROLE_USER.getCode());
        user.setIsEnable(true);

        user.setCreateTime(new Date());


        // 4. 保存用户
        userMapper.insert(user);

        jwtTokenService.generateToken(user.getId(), user.getUsername(), user.getRole());
        LoginVO loginVO = new LoginVO();
        loginVO.setAccessToken(jwtTokenService.generateToken(user.getId(), user.getUsername(), user.getRole()));
        loginVO.setTokenType("Bearer");
        return loginVO;
    }

    @Override
    public UserInfoVO getUserById(Long currentUserId) {
        UserEntity userEntity = userMapper.selectById(currentUserId);
        UserInfoVO userInfoVO = new UserInfoVO();
        userInfoVO.setUsername(userEntity.getUsername());
        userInfoVO.setNickname(userEntity.getNickname());
        userInfoVO.setCredit(userEntity.getCredit());
        userInfoVO.setEmail(userEntity.getEmail());
        userInfoVO.setRole(RoleEnum.getName(userEntity.getRole()));
        userInfoVO.setLastLoginTime(userEntity.getLastLoginTime());
        return userInfoVO;
    }
}
