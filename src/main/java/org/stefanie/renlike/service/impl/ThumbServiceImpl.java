package org.stefanie.renlike.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.stefanie.renlike.constant.BlogConstant;
import org.stefanie.renlike.constant.ThumbConstant;
import org.stefanie.renlike.exception.BusinessException;
import org.stefanie.renlike.exception.ErrorCode;
import org.stefanie.renlike.model.dto.DoThumbRequest;
import org.stefanie.renlike.model.entity.Thumb;
import org.stefanie.renlike.model.entity.User;
import org.stefanie.renlike.service.BlogService;
import org.stefanie.renlike.service.ThumbService;
import org.stefanie.renlike.mapper.ThumbMapper;
import org.springframework.stereotype.Service;
import org.stefanie.renlike.service.UserService;
import org.stefanie.renlike.model.entity.Blog;
import org.stefanie.renlike.util.RedisKeyUtil;

import java.util.concurrent.TimeUnit;

/**
 * @author 张子涵
 * @description 针对表【thumb】的数据库操作Service实现
 * @createDate 2025-04-17 16:01:44
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ThumbServiceImpl extends ServiceImpl<ThumbMapper, Thumb> implements ThumbService {

    private final UserService userService;

    private final BlogService blogService;

    private final TransactionTemplate transactionTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    @Override
    public Boolean doThumb(DoThumbRequest doThumbRequest, HttpServletRequest request) {
        //校验参数
        if (doThumbRequest == null || doThumbRequest.getBlogId() == null) {
            throw new RuntimeException("参数错误");
        }
        String lockKey = BlogConstant.BLOG_THUMB + doThumbRequest.getBlogId().toString();
        //虎丘分布式锁
        Boolean tryLock = redisTemplate.opsForValue().setIfAbsent(lockKey, doThumbRequest.getBlogId(), 10, TimeUnit.SECONDS);
        try {
            if(tryLock) {
                Boolean expired = blogService.isExpired(doThumbRequest.getBlogId());
                if (expired) {

                    //走数据库
                    return transactionTemplate.execute(status -> {
                        User loginUser = userService.getLoginUser(request);
                        Long blogId = doThumbRequest.getBlogId();
                        //先更新博客点赞数
                        boolean update = blogService.lambdaUpdate()
                                .eq(Blog::getId, blogId)
                                .setSql("thumbCount = thumbCount + 1")
                                .update();
                        //再插入点赞记录
                        Thumb thumb = new Thumb();
                        thumb.setBlogId(blogId);
                        thumb.setUserId(loginUser.getId());
                        boolean save = this.save(thumb);
                        //未过期才更新缓存

                        return update && this.save(thumb);
                    });
                }

                // 加锁
                // 编程式事务
                return transactionTemplate.execute(status -> {
                    Long blogId = doThumbRequest.getBlogId();
                    User loginUser = userService.getLoginUser(request);
                    Boolean exists = this.hasThumb(blogId, loginUser.getId());
                    if (exists) {
                        throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户已点赞");
                    }
                    boolean update = blogService.lambdaUpdate()
                            .eq(Blog::getId, blogId)
                            .setSql("thumbCount = thumbCount + 1")
                            .update();

                    Thumb thumb = new Thumb();
                    thumb.setUserId(loginUser.getId());
                    thumb.setBlogId(blogId);
                    // 更新成功才执行
                    boolean success = update && this.save(thumb);
                    // 点赞记录存入 Redis
                    if (success) {
                        redisTemplate.opsForHash().put(ThumbConstant.USER_THUMB_KEY_PREFIX + loginUser.getId().toString(), blogId.toString(), thumb.getId());
                    }
                    // 更新成功才执行
                    return success;
                });

            }
        }finally {
            //有锁才会释放锁
            if(tryLock){
                unLock(lockKey, String.valueOf(doThumbRequest.getBlogId()));
            }

        }
        return false;

    }

    @Override
    public Boolean undoThumb(DoThumbRequest doThumbRequest, HttpServletRequest request) {
        if (doThumbRequest == null || doThumbRequest.getBlogId() == null) {
            throw new RuntimeException("参数错误");
        }
        User loginUser = userService.getLoginUser(request);
        // 加锁
        synchronized (loginUser.getId().toString().intern()) {

            // 编程式事务
            return transactionTemplate.execute(status -> {
                Long blogId = doThumbRequest.getBlogId();
                Long thumbId = ((Long) redisTemplate.opsForHash().get(ThumbConstant.USER_THUMB_KEY_PREFIX + loginUser.getId().toString(), blogId.toString()));
                if (thumbId == null) {
                    throw new RuntimeException("用户未点赞");
                }
                boolean update = blogService.lambdaUpdate()
                        .eq(Blog::getId, blogId)
                        .setSql("thumbCount = thumbCount - 1")
                        .update();

                boolean success = update && this.removeById(thumbId);

// 点赞记录从 Redis 删除
                if (success) {
                    redisTemplate.opsForHash().delete(ThumbConstant.USER_THUMB_KEY_PREFIX + loginUser.getId(), blogId.toString());
                }
                return success;

            });
        }
    }



    @Override
    public Boolean hasThumb(Long blogId, Long userId) {
        return redisTemplate.opsForHash().hasKey(RedisKeyUtil.getUserThumbKey(userId), blogId.toString());
    }
    private void unLock(String lockKey, String lockValue) {
        try {
            // 使用 Lua 脚本确保释放锁的原子性
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            redisTemplate.execute((RedisCallback<Long>) connection -> connection.eval(script.getBytes(),
                    ReturnType.INTEGER, 1, lockKey.getBytes(), lockValue.getBytes()));
        } catch (Exception e) {
            e.printStackTrace();
            // 错误处理
        }
    }
}




