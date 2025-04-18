package org.stefanie.renlike.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
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

import java.time.LocalDateTime;
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
    @Resource
    private RedissonClient redissonClient;

    @Override
    public Boolean doThumb(DoThumbRequest doThumbRequest, HttpServletRequest request) {
        //校验参数
        if (doThumbRequest == null || doThumbRequest.getBlogId() == null) {
            throw new RuntimeException("参数错误");
        }
        User loginUser = userService.getLoginUser(request);
        Long blogId = doThumbRequest.getBlogId();
        Long userId = loginUser.getId();
        //冷热分离，仅一个月的数据查询redis，否则查询数据库
        Boolean expired = blogService.isExpired(blogId);
        return doThumbWithLock(blogId, userId, expired);
    }

    private Boolean doThumbWithLock(Long blogId, Long userId, boolean isNotHot) {
        String lockKey = BlogConstant.BLOG_THUMB + blogId.toString();
        boolean isLock = false;
        boolean isSuccess = false;
        RLock lock = redissonClient.getLock(lockKey);
        if (!isNotHot) {
            //走缓存
            RMap<Object, Object> map = redissonClient.getMap(ThumbConstant.USER_THUMB_KEY_PREFIX + userId);
            Long redisThumbId =(Long) map.get(blogId.toString());
            if (redisThumbId != null) {
              throw  new BusinessException(ErrorCode.PARAMS_ERROR, "您已经点赞过了");
            } else {
              //查询数据库
                try {
                    isLock = lock.tryLock(10, 10, TimeUnit.SECONDS);
                    if (!isLock) {
                        throw new RuntimeException("加锁失败");
                    }
                    //加锁成功，处理点赞逻辑
                    // 1. 更新数据库
                    isSuccess = transactionTemplate.execute(status -> {
                        Thumb thumb = new Thumb();
                        thumb.setBlogId(blogId);
                        thumb.setUserId(userId);
                        // 2. 更新点赞数量
                        boolean update = blogService.lambdaUpdate()
                                .eq(Blog::getId, blogId)
                                .setSql("thumbCount = thumbCount + 1")
                                .update();
                        boolean save = this.save(thumb);
                        //更新缓存
                        map.put(blogId.toString(), thumb.getId());
                        return update && save;
                    });
                    return isSuccess;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作失败");
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }

            }
        }
        //否则走数据库
        // 冷数据走布隆过滤器 + 数据库
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(BlogConstant.BLOG_BLOOM_FILTER);
        String bloomKey = blogId + ":" + userId;
        boolean isContain = bloomFilter.contains(bloomKey);
        if (isContain) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "您已经点赞过了");
        }
        try {
            isLock = lock.tryLock(10, 10, TimeUnit.SECONDS);
            if (!isLock) {
                throw new RuntimeException("加锁失败");
            }
            //加锁成功，处理点赞逻辑
            // 1. 更新数据库
            isSuccess = transactionTemplate.execute(status -> {
                Thumb thumb = new Thumb();
                thumb.setBlogId(blogId);
                thumb.setUserId(userId);
                // 2. 更新点赞数量
                boolean update = blogService.lambdaUpdate()
                        .eq(Blog::getId, blogId)
                        .setSql("thumbCount = thumbCount + 1")
                        .update();
                //加入布隆过滤器
                bloomFilter.add(bloomKey);
                return update && this.save(thumb);
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作失败");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return isSuccess;
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
}




