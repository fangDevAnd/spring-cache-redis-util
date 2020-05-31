package com.atguigu.cache.service.base.tool;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;

public class Tool {

    /**
     * 生成key
     *
     * @param method
     * @param params
     * @return
     */
    public static final String keyGenerator(Method method, Object... params) {
        return method.getName() + ":" + Arrays.toString(params);
    }

    public static final String keyGenerator(String methodName, String cacheName, Object... params) {
        return cacheName + ":" + "\"" + methodName + ":" + Arrays.toString(params) + "\"";
    }

    public static String getParam(Object... param) {
        return Arrays.toString(param);
    }


    public static String getMethodName(Method method) {
        return method.getName();
    }


    public static String getPrefix(String methodName, String cacheName) {
        return cacheName + ":" + "\"" + methodName;
    }
}
