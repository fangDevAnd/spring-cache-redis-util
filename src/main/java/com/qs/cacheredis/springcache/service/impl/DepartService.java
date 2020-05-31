package com.atguigu.cache.service.impl;

import com.atguigu.cache.bean.Department;
import com.atguigu.cache.bean.Employee;
import com.atguigu.cache.mapper.DepartmentMapper;
import com.atguigu.cache.service.base.BaseServiceMyBatis;
import org.springframework.stereotype.Service;

@Service
public class DepartService extends BaseServiceMyBatis<Department, DepartmentMapper, Integer> {


    /**
     * 设置底层调用mybatis的mapper
     *
     * @return
     */
    @Override
    public DepartmentMapper getMapper() {
        return null;
    }


    /**
     * 设置缓存的前缀，为了避免缓存覆盖，一般设置表的名称
     *
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    @Override
    public void initCache() throws NoSuchFieldException, IllegalAccessException {
        setCacheNames(new String[]{"Depart"});
    }


    @Override
    public Department deleteProxy(Integer id) {
        Department department = getProxy().findById(id);
        getProxy().delete(id);
        return department;
    }

    @Override
    public void insertProxy(Department bean) {

    }

    @Override
    public Department updateProxy(Department bean) {
        return null;
    }

}
