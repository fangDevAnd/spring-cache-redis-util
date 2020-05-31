package com.atguigu.cache.service.base.cache;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * redis的缓存处理器,
 * 用来进行动态的更新spring-cache
 */
@Configuration
public class RedisCacheProcess {

    Logger logger = LoggerFactory.getLogger(RedisCacheProcess.class);

    /**
     * 这里需要特别注意的是,不能使用 RedisTemplate ,具体的原因应该和底层有关系,
     */

    @Autowired
    private StringRedisTemplate redisTemplate;


    /**
     * 清除指定前缀的key
     *
     * @param prex
     */
    public void deleteByPrex(String prex) {
        Set<String> keys = redisTemplate.keys(prex);

        logger.debug("删除指定的key");
        logger.debug(Arrays.asList(keys.toArray()).toString());

        if (CollectionUtils.isNotEmpty(keys)) {
            for (String key : keys) {
                redisTemplate.delete(key);
            }
        }
    }


    /**
     * 批量删除
     *
     * @param currentMath
     */
    public void deleteBatch(List<String> currentMath) {

        //调用的连接为 DefaultStringRedisConnection ,这是使用StringRedisTemplate的情况
        //调用的连接为 JedisConnection ,这是使用RedisTemplate的情况,会导致数据无法被删除的情况,
        redisTemplate.delete(currentMath);
    }


}
