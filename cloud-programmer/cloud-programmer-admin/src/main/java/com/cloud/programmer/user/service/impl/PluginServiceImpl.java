package com.cloud.programmer.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cloud.programmer.user.entity.PluginEntity;
import com.cloud.programmer.user.mapper.PluginMapper;
import com.cloud.programmer.user.model.po.PluginPO;
import com.cloud.programmer.user.model.vo.PluginVO;
import com.cloud.programmer.user.service.PluginService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author 周梓强 2026/9/23
 */

@Service
public class PluginServiceImpl implements PluginService {


    @Resource
    private PluginMapper pluginMapper;


    @Override
    public boolean add(PluginPO plugin) {
        PluginEntity pluginEntity = new PluginEntity();
        pluginEntity.setName(plugin.getName());
        pluginEntity.setType(plugin.getType());
        pluginEntity.setContent(plugin.getContent());
        pluginEntity.setIsCustom(plugin.getIsCustom());
        pluginEntity.setIsSale(true);
        pluginEntity.setIsDel(false);
        pluginEntity.setCreateTime(new Date());
        return pluginMapper.insert(pluginEntity) > 0;
    }

    @Override
    public List<PluginVO> selectPlugin(Map<String, Object> params) {
        QueryWrapper<PluginEntity> pluginEntityQueryWrapper = new QueryWrapper<>();
        if (params.get("name") != null && StringUtils.isNotBlank(params.get("name").toString())) {
            pluginEntityQueryWrapper.eq("name", params.get("name"));
        }
        if (params.get("type") != null && StringUtils.isNotBlank(params.get("type").toString())) {
            pluginEntityQueryWrapper.eq("type", params.get("type"));
        }
        List<PluginEntity> pluginEntities = pluginMapper.selectList(pluginEntityQueryWrapper);
        List<PluginVO> pluginVOS = new ArrayList<>();
        for (PluginEntity pluginEntity : pluginEntities) {
            PluginVO pluginVO = new PluginVO();
            pluginVO.setId(pluginEntity.getId());
            pluginVO.setName(pluginEntity.getName());
            pluginVO.setType(pluginEntity.getType());
            pluginVO.setContent(pluginEntity.getContent());
            pluginVO.setIsCustom(pluginEntity.getIsCustom());
            pluginVO.setIsSale(pluginEntity.getIsSale());
            pluginVOS.add(pluginVO);
        }
        return pluginVOS;
    }

    @Override
    public boolean update(Integer id, String content) {
        PluginEntity pluginEntity = pluginMapper.selectById(id);
        if (pluginEntity != null) {
            pluginEntity.setContent(content);
        }
        return pluginMapper.updateById(pluginEntity) > 0;
    }

    @Override
    public boolean delete(Integer id) {
       return pluginMapper.deleteById(id) > 0;
    }

    @Override
    public boolean online(Integer id, Boolean online) {
        PluginEntity pluginEntity = pluginMapper.selectById(id);
        if (pluginEntity != null) {
            pluginEntity.setIsSale(online);
        }
        return pluginMapper.updateById(pluginEntity) > 0;
    }

}
