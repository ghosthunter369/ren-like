package org.stefanie.renlike.service;

import jakarta.servlet.http.HttpServletRequest;
import org.stefanie.renlike.model.dto.DoThumbRequest;
import org.stefanie.renlike.model.entity.Thumb;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author 张子涵
* @description 针对表【thumb】的数据库操作Service
* @createDate 2025-04-17 16:01:44
*/
public interface ThumbService extends IService<Thumb> {
    /**
     * 点赞
     * @param doThumbRequest
     * @param request
     * @return {@link Boolean }
     */
    Boolean doThumb(DoThumbRequest doThumbRequest, HttpServletRequest request);
    /**
     * 取消点赞
     * @param doThumbRequest
     * @param request
     * @return {@link Boolean }
     */
    Boolean undoThumb(DoThumbRequest doThumbRequest, HttpServletRequest request);
    Boolean hasThumb(Long blogId, Long userId);

}
