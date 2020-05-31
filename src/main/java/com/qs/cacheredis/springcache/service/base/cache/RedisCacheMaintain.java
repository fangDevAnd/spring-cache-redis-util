package com.qs.cacheredis.springcache.service.base.cache;


import com.atguigu.cache.service.base.BaseMapper;
import com.atguigu.cache.service.base.cache.RedisCacheProcess;
import com.atguigu.cache.service.base.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 提供的是对 redis 的Cache的手动维护
 *
 * @author fang
 * @datetime 2020-5-14
 */
@Component
public class RedisCacheMaintain {


    Logger logger = LoggerFactory.getLogger(RedisCacheMaintain.class);

    @Autowired
    RedisCacheProcess redisCacheProcess;


    /**
     * 使用线程安全的map进行存储
     */
//    private Map<String, String> methodDescription = new ConcurrentHashMap<>();

    private List<String> keys = new CopyOnWriteArrayList();


    /**
     * 保存redis的参数
     *
     * @param methodName
     * @param param
     */
    public void saveParam(String methodName, String cacheNames, Object... param) {

        String key = Tool.keyGenerator(methodName, cacheNames, param);

        if (keys.contains(key)) {
            return;
        }

        logger.debug("保存的key" + key);

        keys.add(key);
    }

    //清除指定的前缀的key
    public void clear(String cacheName) {

        List<String> currentMath = new ArrayList<>();

//        String prefix = Tool.getPrefix(methodName, cacheName);

        logger.debug("清空key 前缀为= " + cacheName);

        keys.forEach(single -> {

            logger.debug(single);

            if (single.startsWith(cacheName)) {
                currentMath.add(single);
            }
        });
        redisCacheProcess.deleteBatch(currentMath);

    }


}
