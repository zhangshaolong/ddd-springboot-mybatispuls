package com.demo.dddspringbootmybatispuls.common.aggregate;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class AggregatePersistenceManager {
  @Autowired private ApplicationContext applicationContext;
  @Autowired private DoCommonFieldHandler commonFieldHandler;

  @SuppressWarnings({"unchecked"})
  public void persist(AggregateChanges changes) {
    Map<Class<?>, AggregateChanges.TableChanges<?>> tableMap = changes.getTableChangesMap();
    if (tableMap == null || tableMap.isEmpty()) {
      return;
    }

    // 遍历所有DO的变更
    for (Map.Entry<Class<?>, AggregateChanges.TableChanges<?>> entry : tableMap.entrySet()) {
      Class<?> doClass = entry.getKey();
      AggregateChanges.TableChanges<?> tableChanges = entry.getValue();

      // 1. 获取匹配的Mapper
      BaseMapper<?> mapper = getBaseMapper(doClass);
      if (mapper == null) {
        throw new RuntimeException("未找到DO[" + doClass + "]对应的Mapper");
      }

      // 2. 类型转换
      AggregateChanges.TableChanges<Object> typedTable =
          (AggregateChanges.TableChanges<Object>) tableChanges;

      // 3. 处理新增
      if (typedTable.getInsertList() != null && !typedTable.getInsertList().isEmpty()) {
        List<Object> insertList = typedTable.getInsertList();
        insertList.forEach(
            doObj -> commonFieldHandler.fillCommonFields(doObj, EntityChangeType.NEW));

        if (insertList.size() == 1) {
          invokeMapperMethod(mapper, "insert", insertList.get(0));
        } else {
          invokeMapperMethod(mapper, "saveBatch", insertList);
        }
      }

      // 4. 处理修改
      if (typedTable.getUpdateList() != null && !typedTable.getUpdateList().isEmpty()) {
        List<Object> updateList = typedTable.getUpdateList();
        updateList.forEach(
            doObj -> commonFieldHandler.fillCommonFields(doObj, EntityChangeType.MODIFIED));

        if (updateList.size() == 1) {
          invokeMapperMethod(mapper, "updateById", updateList.get(0));
        } else {
          invokeMapperMethod(mapper, "updateBatchById", updateList);
        }
      }

      // 5. 处理删除
      if (typedTable.getDeleteList() != null && !typedTable.getDeleteList().isEmpty()) {
        List<Object> deleteList = typedTable.getDeleteList();
        List<Long> deleteIds =
            deleteList.stream()
                .map(
                    doObj -> {
                      try {
                        Field idField = doObj.getClass().getDeclaredField("id");
                        idField.setAccessible(true);
                        return (Long) idField.get(doObj);
                      } catch (Exception e) {
                        throw new RuntimeException("获取DO的id失败", e);
                      }
                    })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (!deleteIds.isEmpty()) {
          invokeMapperMethod(mapper, "deleteBatchIds", deleteIds);
        }
      }
    }
  }

  /** 通过DO类型匹配对应的Mapper */
  @SuppressWarnings("rawtypes")
  public BaseMapper getBaseMapper(Class<?> targetDoClass) {
    Map<String, BaseMapper> allMappers = applicationContext.getBeansOfType(BaseMapper.class);
    for (BaseMapper mapper : allMappers.values()) {
      // 解析Mapper接口的泛型（BaseMapper<DO>）
      Class<?> mapperDoClass = resolveMapperGenericType(mapper.getClass().getInterfaces()[0]);
      if (mapperDoClass != null && mapperDoClass.equals(targetDoClass)) {
        return mapper;
      }
    }
    return null;
  }

  /** 解析Mapper接口的泛型参数（BaseMapper<T>中的T） */
  private Class<?> resolveMapperGenericType(Class<?> mapperInterface) {
    Type[] genericInterfaces = mapperInterface.getGenericInterfaces();
    for (Type genericInterface : genericInterfaces) {
      if (genericInterface instanceof ParameterizedType parameterizedType) {
        if (parameterizedType.getRawType() == BaseMapper.class) {
          return (Class<?>) parameterizedType.getActualTypeArguments()[0];
        }
      }
    }
    return null;
  }

  /** 反射调用Mapper方法 */
  // 聚合根持久化管理器中 invokeMapperMethod 方法修正
  private void invokeMapperMethod(BaseMapper<?> mapper, String methodName, Object param) {
    try {
      Class<?>[] paramTypes;
      // 根据方法名匹配正确的参数类型
      if ("insert".equals(methodName)) {
        paramTypes = new Class[] {Object.class};
      } else if ("updateById".equals(methodName)) {
        paramTypes = new Class[] {Object.class};
      } else if ("deleteById".equals(methodName)) {
        paramTypes = new Class[] {Serializable.class};
      } else if ("saveBatch".equals(methodName) || "updateBatchById".equals(methodName)) {
        paramTypes = new Class[] {Collection.class};
      } else if ("deleteBatchIds".equals(methodName)) {
        paramTypes = new Class[] {Collection.class};
      } else {
        throw new RuntimeException("不支持的Mapper方法：" + methodName);
      }
      // 反射调用方法
      Method method = mapper.getClass().getMethod(methodName, paramTypes);
      method.invoke(mapper, param);
    } catch (NoSuchMethodException e) {
      // 兜底：尝试从 BaseMapper 接口获取方法（解决代理类方法查找失败）
      try {
        Method method = BaseMapper.class.getMethod(methodName, param.getClass());
        method.invoke(mapper, param);
      } catch (Exception ex) {
        throw new RuntimeException("Mapper方法调用失败：" + methodName, ex);
      }
    } catch (Exception e) {
      throw new RuntimeException("调用Mapper方法失败：" + methodName, e);
    }
  }

  // Getter & Setter（Spring注入用）
  public ApplicationContext getApplicationContext() {
    return applicationContext;
  }

  public void setApplicationContext(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  public DoCommonFieldHandler getCommonFieldHandler() {
    return commonFieldHandler;
  }

  public void setCommonFieldHandler(DoCommonFieldHandler commonFieldHandler) {
    this.commonFieldHandler = commonFieldHandler;
  }
}
