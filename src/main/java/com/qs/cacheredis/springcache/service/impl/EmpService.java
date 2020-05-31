package com.atguigu.cache.service.impl;

import com.atguigu.cache.bean.Employee;
import com.atguigu.cache.mapper.EmployeeMapper;
import com.atguigu.cache.service.base.BaseMapper;
import com.atguigu.cache.service.base.BaseServiceMyBatis;
import com.atguigu.cache.service.base.entity.Page;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.List;


/**
 * 自己封装的缓存框架使用说明
 * <p>
 * 1. 继承BaseServiceMyBatis 实现里面的抽象方法
 * <p>
 * 如果你定义了新的查询接口,请 重写updateProxy()方法,并设置需要清空的缓存
 * <p>
 * 如果你定义了新的
 */
@Service
public class EmpService extends BaseServiceMyBatis<Employee, EmployeeMapper, Integer> {


    @Autowired
    EmployeeMapper employeeMapper;


    @Override
    public EmployeeMapper getMapper() {
        return employeeMapper;
    }





    /**
     * 初始化缓存之后才去执行
     *
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    @Override
    public void initCache() throws NoSuchFieldException, IllegalAccessException {
        logger.debug("加载初始化");
        setCacheNames(new String[]{"emp"});
    }


    /**
     * 进行代理操作
     * 返回删除的旧数据
     *
     * @param id
     */
    @Caching(
            evict = {
                    @CacheEvict(key = "#result.lastName"),//删除旧数据的lastName的匹配
                    @CacheEvict(key = "#result.email"),   //删除旧数据的邮件的匹配
            }
    )
    @Override
    public Employee deleteProxy(Integer id) {
        Employee employee = getProxy().findById(id);
        getProxy().delete(id);
        return employee;
    }


    /**
     * 进行代理操作
     *
     * @param bean
     */
    @Caching(
            evict = {
                    @CacheEvict(key = "#result.lastName"),//这里必须是原先的数据
                    @CacheEvict(key = "#result.email"),
            }
    )
    @Override
    public Employee updateProxy(Employee bean) {
        Employee employee = updateInner(bean, bean.getId());
        //返回旧数据,便于更新
        return employee;
    }

    @Caching(
            evict = {
                    @CacheEvict(key = "#bean.lastName"),
                    //如果邮箱能够重复,当前接口需要配置,如果系统的用户的邮箱不能从重复,就不需要配置
//                    @CacheEvict(key = "#bean.email"),
            }
    )
    @Override
    public void insertProxy(Employee bean) {
        getProxy().insert(bean);
    }


    @Cacheable(key = "#lastName")
    public Employee getEmpByLastName(String lastName) {
        return employeeMapper.getEmpByLastName(lastName);
    }

    @Cacheable(
            key = "#email"
    )
    public Employee getEmpByEmail(String email) {
        return employeeMapper.getEmpByEmail(email);
    }


    /**
     * 判断当前数据是否存在
     * <p>
     * 根据业务逻辑,邮箱不能重复,
     *
     * @param employee
     */
    public void add(Employee employee) {

        Employee employee1 = getEmpByEmail(employee.getEmail());
        if (employee1 != null) {
            //存在
            throw new IllegalArgumentException("邮箱已经被人使用");
        }
        getProxy().insertProxy(employee);
    }




}
