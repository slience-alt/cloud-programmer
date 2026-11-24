package com.cloud.programmer.user.service;

import com.cloud.programmer.user.model.po.PluginPO;
import com.cloud.programmer.user.model.vo.PluginVO;

import java.util.List;
import java.util.Map;

/**
 * @author 周梓强 2026/9/23
 */


public interface PluginService {

    boolean add(PluginPO plugin);

    List<PluginVO> selectPlugin(Map<String, Object> params);


    boolean update(Integer id, String content);

    boolean delete(Integer id);

    boolean online(Integer id, Boolean online);
}
