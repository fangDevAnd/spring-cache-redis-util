package com.atguigu.cache.service.base;

import com.atguigu.cache.service.base.entity.Page;
import com.atguigu.cache.service.base.entity.Paging;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 基本的mapper ,
 * 所有使用mybatis的基本结构都需要实现的基本接口
 * T bean类型
 * K  主键类型
 */
@Mapper
public interface BaseMapper<T, K> {


    /**
     * 通过ID查找
     *
     * @param id
     * @return
     */
    T findById(K id);


    void update(T bean);


    void deleteById(K id);

    List<T> findAll();


    void insert(T bean);

    int count();

    List<T> findByPage(Integer start, Integer page);

    int countFilter(T bean);

    /**
     * 添加查询
     *
     * @param paging
     * @param bean
     * @return
     */
    List<T> findByFilterPage(Paging paging, T bean);

    List<T> findByFilter(T bean);
}
