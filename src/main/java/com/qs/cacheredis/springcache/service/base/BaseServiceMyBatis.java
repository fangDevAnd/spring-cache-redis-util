package com.atguigu.cache.service.base;

import com.atguigu.cache.bean.Employee;
import com.atguigu.cache.service.base.cache.RedisCacheMaintain;
import com.atguigu.cache.service.base.cache.RedisCacheProcess;
import com.atguigu.cache.service.base.entity.Page;
import com.atguigu.cache.service.base.entity.Paging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cache.annotation.*;
import org.springframework.cache.interceptor.SimpleCacheResolver;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.PostConstruct;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 这个类的作用是封装基本的查询的实现,
 * <p>
 * 这些查询的接口都已经实现了 基本的缓存的实现, 主要依赖于spring-cache的基本功能实现
 * 继承当前的接口,需要自己添加
 *
 * @CacheConfig(cacheNames = "xxx") 指定前缀,避免缓存覆盖
 * <p>
 * <p>
 * 如果没有指定 keyGenerator 会使用 SimpleKeyGenerator
 * 如果没有指定 CacheResolver 使用默认的 SimpleCacheResolver
 */

@CacheConfig(cacheNames = "xxx")
public abstract class BaseServiceMyBatis<T, M extends BaseMapper<T, K>, K> {


    public Logger logger = LoggerFactory.getLogger(BaseServiceMyBatis.class);


    /**
     * 定义的mapper
     *
     * @return
     */
    public abstract M getMapper();


    /**
     * redis的缓存维护
     */
    @Autowired
    RedisCacheMaintain redisCacheMaintain;


    /**
     * 查找指定的元素，通过 主键查找方案
     *
     * @param id
     * @return
     */
    @Cacheable(key = "#id")
    public T findById(K id) {
        System.out.println("查询" + id + "号数据");
        T emp = getMapper().findById(id);
        return emp;
    }


    private String[] cacheNames;


    /**
     * 配置cache的名称，这个作为缓存的命名空间，是对缓存的一个隔离作用，不同的service应该设置不同的cacheName
     *
     * @param cacheNames
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public void setCacheNames(String[] cacheNames) throws NoSuchFieldException, IllegalAccessException {
        this.cacheNames = cacheNames;
        cacheProcess();
    }


    /**
     * 获得当前类的的代理对象
     * 需要注意的是，为了保证系统内部的调用走代理，本地调用不能直接使用this，
     * 需要通过 代理对象调用
     *
     * @return
     */
    protected BaseServiceMyBatis<T, M, K> getProxy() {

        BaseServiceMyBatis service = null;

        if (AopContext.currentProxy() != null) {
            service = (BaseServiceMyBatis) AopContext.currentProxy();
        } else {
            service = this;
        }
        return service;
    }


    /**
     * 缓存处理框架
     * 动态的修改cachename，这个主要用来实现缓存的隔离方案
     */
    private void cacheProcess() throws NoSuchFieldException, IllegalAccessException {

        CacheConfig annotation = BaseServiceMyBatis.class.getAnnotation(CacheConfig.class);

        InvocationHandler ih = Proxy.getInvocationHandler(annotation);

        Field hField = ih.getClass().getDeclaredField("memberValues");
        // 因为这个字段事 private final 修饰，所以要打开权限
        hField.setAccessible(true);
        // 获取 memberValues
        Map memberValues = (Map) hField.get(ih);
        memberValues.put("cacheNames", cacheNames);

        String[] value = annotation.cacheNames();

        logger.debug(Arrays.toString(value));

    }


    /**
     * 更新当前的数据，需要注意的是
     * 当前方法请不要在外部调用，
     *
     * @param bean
     * @return
     */
    @Caching(
            put = {
                    @CachePut(key = "#result.id")
            },
            evict = {
                    @CacheEvict(key = "'findAll'"),
//                    @CacheEvict(key = "'count'"), 更新一个数据，count的数量是不变的
            }
    )

    public T update(T bean) {
        System.out.println("updateEmp:" + bean);
        getMapper().update(bean);

        clearOtherCache();

        return bean;
    }




    /**
     * 供外部调用的更新的接口
     *
     * @param bean 需要更新的bean
     * @param id   bean的主键
     * @return 返回更新之前的bean的相关数据
     */
    public T updateInner(T bean, K id) {
        //原先的数据
        T temp = getProxy().findById(id);
        //新的数据
        getProxy().update(bean);
        return temp;
    }


    /**
     * 更新操作的代理，这个接口必须在内部通过调用{@link BaseServiceMyBatis#updateInner(Object, Object)}
     * 操作，
     *
     * @param bean 需要被更新的bean
     * @return 返回被更新之前的旧数据，用于设置缓存的清除操作
     */
    public abstract T updateProxy(T bean);


    /**
     * 清空缓存的方法
     * 这个方法主要清空 saveOtherCache 对应的缓存
     * 如果继承的类实现了 delete insert add 都要 调用当前的方法进行缓存的清空
     */
    public void clearOtherCache() {
        //手动的清除
        redisCacheMaintain.clear(cacheNames[0]);
    }

    /**
     * 保存缓存的实现
     * 用于生成key的缓存的清除
     * 该方法主要用在自定义的query 操作中，
     * 由于借助 springCache不能实现部分的缓存清除操作，
     * 所以自己内部维持了一个缓存key，用于特殊处理
     * ，与之相对的就是
     * {@link BaseServiceMyBatis#clearOtherCache()}
     *
     * @param methodName
     * @param param
     */
    public void saveOtherCache(String methodName, Object... param) {
        //保存方法
        redisCacheMaintain.saveParam(methodName, cacheNames[0], param);
    }


    /**
     * 部分删除的操作，请通过调用{@link BaseServiceMyBatis#deleteProxy(Object)}
     * 操作进行
     * 删除一个元素
     * 该方法需要一个嫁接,所以为了实现这个功能,需要定义一个 代理的 删除接口
     * 该方法不能在外部直接调用，子类需要重实现deleteProxy方法，在内部调用当前的方法
     *
     * @param id 元素的主键
     */
    @Caching(
            evict = {
                    @CacheEvict(key = "#id"),
                    @CacheEvict(key = "'findAll'"),
                    @CacheEvict(key = "'count'"),
            }
    )
    public void delete(K id) {
        System.out.println("deleteEmp:" + id);
        getMapper().deleteById(id);

        clearOtherCache();

    }


    /**
     * key的回调接口
     *
     * @param <K>
     * @param <T>
     */
    public interface KeyCall<K, T> {
        public K call(T t);
    }


    /**
     * 根据条件删除,需要注意的是,当前的删除不要设置主键删除,主键删除请使用上面
     * {@link BaseServiceMyBatis#delete(Object)} 方法，当前方法对主键删除无效
     *当前方法可以在外部直接调用
     *
     *
     * @param bean
     */
    public void deleteFilter(T bean, KeyCall<K, T> keyCall) {
        List<T> tList = getMapper().findByFilter(bean);
        for (T t : tList) {
            K key = keyCall.call(t);
            getProxy().delete(key);
        }
    }


    public abstract T deleteProxy(K id);


    /**
     * 构造完成之后调用的方法
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    @PostConstruct
    public abstract void initCache() throws NoSuchFieldException, IllegalAccessException;


    @Cacheable(key = "'findAll'")
    public List<T> findAll() {
        return getMapper().findAll();
    }


    /**
     * 插入的代理操作
     * @param bean
     */
    public abstract void insertProxy(T bean);


    /**
     * 插入一个数据 ,需要删除对应的缓存数据
     * 该方法不能直接调用，插入操作需要调用对应的代理实现
     * {@link BaseServiceMyBatis#insertProxy(Object)} 进行操作
     * 对于上面的方法，需要调用内部的实现
     *
     * @param bean
     */
    @Caching(
            evict = {
                    @CacheEvict(key = "'findAll'"),
                    @CacheEvict(key = "'count'"),
            }
    )
    public void insert(T bean) {

        logger.debug("插入数据", bean.toString());

        getMapper().insert(bean);

        clearOtherCache();

    }


    //下面封装 聚合查询,count

    /**
     * 本身是一个get操作
     * 影响的操作有 delete* ,  insert* ,
     * 返回总数量
     *
     * @return
     */
    @Cacheable(key = "'count'")
    public int count() {
        return getMapper().count();
    }

    /**
     * 获得指定条件的count查询
     * @param bean
     * @return
     */
    @Cacheable(keyGenerator = "methodAndParamKeyGenerator")
    public int count(T bean) {

        saveOtherCache("count",bean);

        return getMapper().countFilter(bean);
    }

    //下面封装单表的分页查询

    /**
     * 进行分页查询
     * 涉及的缓存的操作是 删除 更新  插入
     *
     * @param page
     * @param start
     * @return
     */
    @Cacheable(keyGenerator = "methodAndParamKeyGenerator")
    public Page<T> findByPage(int start, int page) {

        saveOtherCache("findByPage", start, page);

        List<T> pageList = getMapper().findByPage(start * page, page);

        int count = getProxy().count();

        return new Page<T>(count, pageList);
    }


    /**
     * 分页条件查询
     * <p>
     * 如果传递的参数是JavaBean,使用下面的key生成器会导致序列化数据haskcode变化,从而导致缓存失效
     * 为了解决这个我问题, 对Page进行从写toString,也就是对于 使用我们定义的methodAndParamKeyGenerator
     * 参数都必须重写toString()方法
     *
     * @param paging
     * @param bean
     * @return
     */
    @Cacheable(keyGenerator = "methodAndParamKeyGenerator")
    public Page<T> findByFilterPage(Paging paging, T bean) {

        //保存缓存
        saveOtherCache("findByFilterPage", paging, bean);

        List<T> pageList = getMapper().findByFilterPage(paging, bean);

        //数量
        int count = getProxy().count(bean);

        return new Page<>(count, pageList);
    }

    /**
     * 过滤查询的实现
     *
     * @param bean
     * @return
     */
    @Cacheable(keyGenerator = "methodAndParamKeyGenerator")
    public Page<T> findByFilter(T bean) {
        //保存缓存
        saveOtherCache("findByFilter", bean);
        List<T> pageList = getMapper().findByFilter(bean);

        //数量
        int count = getProxy().count(bean);

        return new Page<>(count, pageList);
    }


}
