package org.stefanie.renlike.service;

import jakarta.servlet.http.HttpServletRequest;
import org.stefanie.renlike.model.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;
import org.stefanie.renlike.model.vo.BlogVO;

import java.util.List;

/**
* @author 张子涵
* @description 针对表【blog】的数据库操作Service
* @createDate 2025-04-17 16:01:44
*/
public interface BlogService extends IService<Blog> {
    BlogVO getBlogVOById(long blogId, HttpServletRequest request);
    List<BlogVO> getBlogVOList(List<Blog> blogList, HttpServletRequest request);
    Boolean isExpired(Long blogId);

}
