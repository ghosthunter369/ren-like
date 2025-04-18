package org.stefanie.renlike.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.stefanie.renlike.constant.CommonConstant;
import org.stefanie.renlike.constant.ThumbConstant;
import org.stefanie.renlike.exception.BusinessException;
import org.stefanie.renlike.exception.ErrorCode;
import org.stefanie.renlike.exception.ThrowUtils;
import org.stefanie.renlike.model.dto.post.BlogQueryRequest;
import org.stefanie.renlike.model.entity.Blog;
import org.stefanie.renlike.model.entity.Thumb;
import org.stefanie.renlike.model.entity.User;
import org.stefanie.renlike.model.vo.BlogVO;
import org.stefanie.renlike.service.BlogService;
import org.stefanie.renlike.mapper.BlogMapper;
import org.springframework.stereotype.Service;
import org.stefanie.renlike.service.ThumbService;
import org.stefanie.renlike.service.UserService;
import org.stefanie.renlike.util.SqlUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 张子涵
 * @description 针对表【blog】的数据库操作Service实现
 * @createDate 2025-04-17 16:01:44
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog>
        implements BlogService {

    @Resource
    private UserService userService;

    @Resource
    @Lazy
    private ThumbService thumbService;
    @Resource
    @Lazy
    private BlogService blogService;
    @Resource
    private RedisTemplate redisTemplate;

    @Override
    public BlogVO getBlogVOById(long blogId, HttpServletRequest request) {
        Blog blog = this.getById(blogId);
        User loginUser = userService.getLoginUser(request);
        return this.getBlogVO(blog, loginUser);
    }

    private BlogVO getBlogVO(Blog blog, User loginUser) {
        BlogVO blogVO = new BlogVO();
        BeanUtil.copyProperties(blog, blogVO);

        if (loginUser == null) {
            return blogVO;
        }

        Boolean exist = thumbService.hasThumb(blog.getId(), loginUser.getId());
        blogVO.setHasThumb(exist);

        return blogVO;
    }

    @Override
    public List<BlogVO> getBlogVOList(List<Blog> blogList, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Map<Long, Boolean> blogIdHasThumbMap = new HashMap<>();
        if (ObjUtil.isNotEmpty(loginUser)) {
            List<Object> blogIdList = blogList.stream().map(blog -> blog.getId().toString()).collect(Collectors.toList());
            // 获取点赞
            List<Object> thumbList = redisTemplate.opsForHash().multiGet(ThumbConstant.USER_THUMB_KEY_PREFIX + loginUser.getId(), blogIdList);
            for (int i = 0; i < thumbList.size(); i++) {
                if (thumbList.get(i) == null) {
                    continue;
                }
                blogIdHasThumbMap.put(Long.valueOf(blogIdList.get(i).toString()), true);
            }
        }

        return blogList.stream()
                .map(blog -> {
                    BlogVO blogVO = BeanUtil.copyProperties(blog, BlogVO.class);
                    blogVO.setHasThumb(blogIdHasThumbMap.get(blog.getId()));
                    return blogVO;
                })
                .toList();
    }

    @Override
    public Boolean isExpired(Long blogId) {
        //先查询数据库，看博客发布时间是否超过一个月，超过一个月走数据库，不超过走redis
        Blog queryBlog = blogService.getById(blogId);
        Date blogCreateTime = queryBlog.getCreateTime();
        LocalDateTime blogCreateTimeLocalDateTime = blogCreateTime.toInstant()  // 转为 Instant
                .atZone(ZoneId.of("Asia/Shanghai"))  // 使用标准时区ID
                .toLocalDateTime();  // 转为 LocalDateTime
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime blogPlusMonthDate = blogCreateTimeLocalDateTime.plusMonths(1);
        return now.isAfter(blogPlusMonthDate);

    }

    @Override
    public void validBlog(Blog blog, boolean add) {
        if (blog == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long userId = blog.getUserId();
        String title = blog.getTitle();
        String content = blog.getContent();
        // 创建时，参数不能为空

        if (!add) {
            ThrowUtils.throwIf(StringUtils.isAnyBlank(title, content), ErrorCode.PARAMS_ERROR);
        }
        if(ObjectUtil.isNull(userId)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 有参数则校验
        if (StringUtils.isNotBlank(title) && title.length() > 80) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题过长");
        }
        if (StringUtils.isNotBlank(content) && content.length() > 8192) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容过长");
        }
        if(add){
           blogService.save(blog);
        }
    }

    @Override
    public QueryWrapper<Blog> getQueryWrapper(BlogQueryRequest blogQueryRequest) {
        QueryWrapper<Blog> queryWrapper = new QueryWrapper<>();
        if (blogQueryRequest == null) {
            return queryWrapper;
        }
        Long id = blogQueryRequest.getId();
        String searchText = blogQueryRequest.getSearchText();
        String title = blogQueryRequest.getTitle();
        String content = blogQueryRequest.getContent();
        Long userId = blogQueryRequest.getUserId();
        String sortField = blogQueryRequest.getSortField();
        String sortOrder = blogQueryRequest.getSortOrder();
        // 拼接查询条件
        if (StringUtils.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw.like("title", searchText).or().like("content", searchText));
        }
        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
        queryWrapper.like(StringUtils.isNotBlank(content), "content", content);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }


    @Override
    public BlogVO getBlogVO(Blog blog, HttpServletRequest request) {
        BlogVO blogVO = new BlogVO();
        BeanUtil.copyProperties(blog, blogVO);
        return blogVO;
    }

    @Override
    public Page<BlogVO> getBlogVOPage(Page<Blog> blogPage, HttpServletRequest request) {

        return null;
    }

    @Override
    public Page<BlogVO> listBlogVOByPage(BlogQueryRequest blogQueryRequest, HttpServletRequest request) {
        return null;
    }

}




