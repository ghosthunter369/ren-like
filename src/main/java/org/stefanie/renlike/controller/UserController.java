package org.stefanie.renlike.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.stefanie.renlike.common.BaseResponse;
import org.stefanie.renlike.common.ResultUtils;
import org.stefanie.renlike.constant.UserConstant;
import org.stefanie.renlike.exception.ErrorCode;
import org.stefanie.renlike.exception.ThrowUtils;
import org.stefanie.renlike.model.entity.User;
import org.stefanie.renlike.service.UserService;
import org.stefanie.renlike.model.entity.vo.LoginUserVO;
@RestController
@RequestMapping("user")
public class UserController {  
    @Resource
    private UserService userService;
  
    @GetMapping("/login")
    public BaseResponse<User> login(long userId, HttpServletRequest request) {
        User user = userService.getById(userId);
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        return ResultUtils.success(user);
    }
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(user));
    }
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

}
