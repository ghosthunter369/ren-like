package org.stefanie.renlike.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.stefanie.renlike.constant.ThumbConstant;
import org.stefanie.renlike.model.entity.Blog;
import org.stefanie.renlike.model.entity.Thumb;
import org.stefanie.renlike.model.entity.User;
import org.stefanie.renlike.model.vo.BlogVO;
import org.stefanie.renlike.service.BlogService;
import org.stefanie.renlike.mapper.BlogMapper;
import org.springframework.stereotype.Service;
import org.stefanie.renlike.service.ThumbService;
import org.stefanie.renlike.service.UserService;

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

}




