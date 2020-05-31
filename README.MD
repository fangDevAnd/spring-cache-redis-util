## SpringCache的封装实现缓存方案

### 1.案例说明
由于缓存的复杂性，我们在CRUD的操作过程中，常常需要思考缓存
的设置，清除，更新操作，为了简化这个流程，当前demo结合springCache以及mybatis对缓存进行了基本的封装操作，
当前的demo只作为一个缓存封装的思路，如果需要其他的特性，以及其他的方法，可以按照下面我说的步骤进行封装。


### 2.功能
抽象类`BaseServiceMyBatis`提供了下面几种方法
```java
//更新操作
public abstract T updateProxy(T bean);

//删除操作
public abstract T deleteProxy(K id);

//删除指定条件的元素
 public void deleteFilter(T bean, KeyCall<K, T> keyCall) 


//查找所有
 public List<T> findAll()

 //插入一个元素
public abstract void insertProxy(T bean);

//总数量
 public int count() 

//查询指定条件的数量
public int count(T bean);


//分页查询
public Page<T> findByPage(int start, int page) 

//分页条件查询
 public Page<T> findByFilterPage(Paging paging, T bean)

 //条件查询
 public Page<T> findByFilter(T bean)


```




### 3.使用方法
当前的demo主要的类是`BaseServiceMyBatis`,对缓存进行了基本的抽象通过
我们主只需要继承`BaseSeriveMyBatis`进行实现，当前继承上面的抽象类，需要重写下面的方法，下面给出demo
```java

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
        getProxy().insert(bean);
    }

    @Override
    public Department updateProxy(Department bean) {
         Department depart = updateInner(bean, bean.getId());
               //返回旧数据,便于更新
               return depart;
    }

}


```
上面几个代理方法的实现并不是一定需要实现的，前提是你没有自定义查询，因为如果你进行了自定义查询，必定需要自己缓存，然后在代理方法上面进行缓存的清除
下面我们对上面的方法进行介绍  
 1）`getMapper()`用于定义mybatis的mapper，当前的mapper需要实现BaseMapper里面的方法，具体我们下面会说  
 2） `initCache()`内部需要调用setCacheNames设置缓存的前缀，一般设置表的名称
 3）剩下三个都是数据操作的代理接口，需要在内部分别调用对应的操作即可  
 上面的demo给出了一个最基本的实现方案，但是现实操作很复杂，我们内部封装了常用的方法可能不能满足你的操作，为了解决这个问题，你可能会自定义新的方法，例如
 ```java
   @Cacheable(key = "#lastName")
    public Employee getEmpByLastName(String lastName) {
        return employeeMapper.getEmpByLastName(lastName);
    }
```
这个取决于你的业务逻辑，当你进行上面的操作之后，你就必须在上面的三个代理对象上面设置缓存的操作方法，那么上面的三个代理对象就变成了下面的样子
```java
 /**
     * 进行代理操作
     * 返回删除的旧数据
     *
     * @param id
     */
    @Caching(
            evict = {
                    @CacheEvict(key = "#result.lastName"),//删除旧数据的lastName的匹配
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
            }
    )
    @Override
    public void insertProxy(Employee bean) {
        getProxy().insert(bean);
    }
```
尽管如此看起来还是很麻烦，但是仔细分析并没有那么复杂，一般来说，查询受到删除，更新，插入的影响，所以在三个代理对象上面都要配置对应的缓存清除

下面看下Mapper的实现方案
```java
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
```
由于`BaseServiceMybatis`底层的实现依赖mybatis，所以需要实现上面的mapper接口
当然本例子使用了MyBatis，你完全可以进行改写底层的实现，例如jpa等。

该demo 提供了一种缓存方法的基本思路，并不一定能够满足一定的生产环境，具体需要自己进行解决，本demo只是提供了一个基本的思路
如果有问题，欢迎issue 


 
 


