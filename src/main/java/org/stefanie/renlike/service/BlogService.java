package org.stefanie.renlike.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import org.stefanie.renlike.model.dto.post.BlogQueryRequest;
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
    /**
     * 校验
     *
     * @param blog
     * @param add
     */
    void validBlog(Blog blog, boolean add);

    /**
     * 获取查询条件
     *
     * @param blogQueryRequest
     * @return
     */
    QueryWrapper<Blog> getQueryWrapper(BlogQueryRequest blogQueryRequest);


    /**
     * 获取帖子封装
     *
     * @param blog
     * @param request
     * @return
     */
    BlogVO getBlogVO(Blog blog, HttpServletRequest request);

    /**
     * 分页获取帖子封装
     *
     * @param blogPage
     * @param request
     * @return
     */
    Page<BlogVO> getBlogVOPage(Page<Blog> blogPage, HttpServletRequest request);

    Page<BlogVO> listBlogVOByPage(BlogQueryRequest blogQueryRequest,HttpServletRequest request);

}
