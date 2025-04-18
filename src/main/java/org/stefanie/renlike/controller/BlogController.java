package org.stefanie.renlike.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.stefanie.renlike.common.BaseResponse;
import org.stefanie.renlike.common.ResultUtils;
import org.stefanie.renlike.exception.BusinessException;
import org.stefanie.renlike.exception.ErrorCode;
import org.stefanie.renlike.model.entity.Blog;
import org.stefanie.renlike.model.vo.BlogVO;
import org.stefanie.renlike.service.BlogService;

import java.util.List;

@RestController
@RequestMapping("blog")
public class BlogController {  
    @Resource
    private BlogService blogService;
  
    @GetMapping("/get")
    public BaseResponse<BlogVO> get(long blogId, HttpServletRequest request) {
        BlogVO blogVO = blogService.getBlogVOById(blogId, request);  
        return ResultUtils.success(blogVO);
    }
    @GetMapping("/list")
    public BaseResponse<List<BlogVO>> list(HttpServletRequest request) {
        List<Blog> blogList = blogService.list();
        List<BlogVO> blogVOList = blogService.getBlogVOList(blogList, request);
        return ResultUtils.success(blogVOList);
    }
    @PostMapping("/list")
    public BaseResponse<Boolean> postBlog(@RequestBody Blog blog, HttpServletRequest request) {
        if(blog == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        blogService.validBlog(blog, true);
        return ResultUtils.success(true);
    }

}
