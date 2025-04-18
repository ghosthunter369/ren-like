package org.stefanie.renlike.service;

import jakarta.servlet.http.HttpServletRequest;
import org.stefanie.renlike.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import org.stefanie.renlike.model.entity.vo.LoginUserVO;
/**
* @author 张子涵
* @description 针对表【user】的数据库操作Service
* @createDate 2025-04-17 16:01:45
*/
public interface UserService extends IService<User> {
    /**
     * 获取脱敏的已登录用户信息
     *
     * @return
     */
   LoginUserVO getLoginUserVO(User user);

    User getLoginUser(HttpServletRequest request);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

}
