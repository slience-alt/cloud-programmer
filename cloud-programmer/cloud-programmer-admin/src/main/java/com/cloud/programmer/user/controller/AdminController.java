package com.cloud.programmer.user.controller;

import com.cloud.programmer.aspect.ApiOperationLog;
import com.cloud.programmer.user.model.po.PluginPO;
import com.cloud.programmer.user.model.vo.LoginVO;
import com.cloud.programmer.user.model.vo.PluginVO;
import com.cloud.programmer.user.model.vo.RegisterVO;
import com.cloud.programmer.user.model.vo.UserVO;
import com.cloud.programmer.result.Response;
import com.cloud.programmer.user.service.PluginService;
import com.cloud.programmer.user.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author 周梓强 2026/9/22
 */

@RestController
@RequestMapping("/user")
public class AdminController {

    @Resource
    private UserService userService;

    @Resource
    private PluginService pluginService;

    @RequestMapping("/list")
    @ResponseBody
    @ApiOperationLog(description = "检索用户")
    public Response<List<UserVO>> list(@RequestParam Map<String, Object> params) {
        List<UserVO> list = userService.list(params);
        return Response.success(list);
    }

    @RequestMapping("/bannedUser")
    @ResponseBody
    @ApiOperationLog(description = "启用/禁用用户")
    public Response<Objects> bannedUser(@RequestParam("id") Integer id, @RequestParam("status") Integer status) {
        boolean isSuccess = userService.bannedUser(id, status);
        return Response.successIfTrue(isSuccess);
    }

    @RequestMapping("/addCredit")
    @ResponseBody
    @ApiOperationLog(description = "增加信用值")
    public Response<Object> addCredit(@RequestParam("creditNum") int creditNum, @RequestParam("id") Integer id) {
        boolean isSuccess = userService.addCredit(creditNum, id);
        return Response.successIfTrue(isSuccess);
    }

    @RequestMapping("/addPlugin")
    @ResponseBody
    @ApiOperationLog(description = "添加插件")
    public Response<Objects> addPlugin(@RequestBody PluginPO plugin) {
        boolean isSuccess = pluginService.add(plugin);
        return Response.successIfTrue(isSuccess);
    }

    @RequestMapping("/selectPlugin")
    @ResponseBody
    @ApiOperationLog(description = "检索插件")
    public Response<List<PluginVO>> selectPlugin(@RequestParam Map<String, Object> params) {
        List<PluginVO> pluginVOPage = pluginService.selectPlugin(params);
        return Response.success(pluginVOPage);
    }

    @RequestMapping("/updatePlugin")
    @ResponseBody
    @ApiOperationLog(description = "更新插件")
    public Response<Object> updatePlugin(@RequestParam("id") Integer id, @RequestParam("content") String content) {
        boolean isSuccess = pluginService.update(id, content);
        return Response.successIfTrue(isSuccess);
    }

    @RequestMapping("/onlinePlugin")
    @ResponseBody
    @ApiOperationLog(description = "插件上下架")
    public Response<Object> onlinePlugin(@RequestParam("id") Integer id, @RequestParam("online") Boolean online) {
        boolean isSuccess = pluginService.online(id, online);
        return Response.successIfTrue(isSuccess);
    }

    @RequestMapping("/deletePlugin")
    @ResponseBody
    @ApiOperationLog(description = "删除插件")
    public Response<Object> deletePlugin(@RequestParam("id") Integer id) {
        boolean isSuccess= pluginService.delete(id);
        return Response.successIfTrue(isSuccess);
    }



}
