package com.demo.dddspringbootmybatispuls.common.aggregate;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.io.Serializable;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Data
@Component
public class AggregatePersistenceManager {
  @Autowired private ApplicationContext applicationContext;
  @Autowired private DoCommonFieldHandler commonFieldHandler;
  private boolean debug = false;

  @SuppressWarnings({"unchecked"})
  public void persist(AggregateChanges changes) {
    Map<Class<?>, AggregateChanges.TableChanges<?>> tableMap = changes.getTableChangesMap();
    if (tableMap == null || tableMap.isEmpty()) {
      return;
    }

    for (Map.Entry<Class<?>, AggregateChanges.TableChanges<?>> entry : tableMap.entrySet()) {
      Class<?> doClass = entry.getKey();
      AggregateChanges.TableChanges<?> tableChanges = entry.getValue();

      BaseMapper<?> mapper = getBaseMapper(doClass);
      if (mapper == null) {
        throw new RuntimeException("未找到DO[" + doClass + "]对应的Mapper");
      }

      AggregateChanges.TableChanges<Object> typedTable =
          (AggregateChanges.TableChanges<Object>) tableChanges;

      if (typedTable.getInsertList() != null && !typedTable.getInsertList().isEmpty()) {
        List<Object> insertList = typedTable.getInsertList();
        insertList.forEach(
            doObj -> commonFieldHandler.fillCommonFields(doObj, EntityChangeType.NEW));

        if (insertList.size() == 1) {
          invokeMapperMethod(mapper, "insert", insertList.getFirst());
        } else {
          invokeMapperMethod(mapper, "saveBatch", insertList);
        }
      }

      if (typedTable.getUpdateList() != null && !typedTable.getUpdateList().isEmpty()) {
        List<Object> updateList = typedTable.getUpdateList();
        updateList.forEach(
            doObj -> commonFieldHandler.fillCommonFields(doObj, EntityChangeType.MODIFIED));

        if (updateList.size() == 1) {
          invokeMapperMethod(mapper, "updateById", updateList.getFirst());
        } else {
          invokeMapperMethod(mapper, "updateBatchById", updateList);
        }
      }

      if (typedTable.getDeleteList() != null && !typedTable.getDeleteList().isEmpty()) {
        List<Object> deleteList = typedTable.getDeleteList();
        List<Long> deleteIds = new ArrayList<>();

        for (Object doObj : deleteList) {
          if (doObj == null) {
            System.err.println("删除列表中存在null的DO对象，跳过");
            continue;
          }

          try {
            Long id = getId(doObj);

            if (id == null) {
              System.err.println("DO[" + doObj.getClass() + "]的id为null，跳过删除");
              continue;
            }

            deleteIds.add(id);
          } catch (Exception e) {
            throw new RuntimeException("获取DO[" + doObj.getClass() + "]的id失败", e);
          }
        }

        if (!deleteIds.isEmpty()) {
          invokeMapperMethod(mapper, "deleteBatchIds", deleteIds);
        } else {
          System.err.println("无有效删除ID，跳过批量删除");
        }
      }
    }
  }

  private static Long getId(Object doObj) throws IllegalAccessException {
    Field idField = null;
    Class<?> clazz = doObj.getClass();
    while (clazz != null && idField == null) {
      try {
        idField = clazz.getDeclaredField("id");
      } catch (NoSuchFieldException e) {
        clazz = clazz.getSuperclass();
      }
    }

    if (idField == null) {
      throw new RuntimeException("DO[" + doObj.getClass() + "]未找到id字段");
    }

    idField.setAccessible(true);
    return (Long) idField.get(doObj);
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

  private void invokeMapperMethod(BaseMapper<?> mapper, String methodName, Object param) {
    try {
      Class<?>[] paramTypes = getClasses(methodName);
      Method method = mapper.getClass().getMethod(methodName, paramTypes);
      doMapper(mapper, param, method);
    } catch (NoSuchMethodException e) {
      try {
        Method method = BaseMapper.class.getMethod(methodName, param.getClass());
        doMapper(mapper, param, method);
      } catch (Exception ex) {
        throw new RuntimeException("Mapper方法调用失败：" + methodName, ex);
      }
    } catch (Exception e) {
      throw new RuntimeException("调用Mapper方法失败：" + methodName, e);
    }
  }

  private static Class<?>[] getClasses(String methodName) {
    return switch (methodName) {
      case "insert", "updateById" -> new Class[] {Object.class};
      case "deleteById" -> new Class[] {Serializable.class};
      case "saveBatch", "updateBatchById", "deleteBatchIds" -> new Class[] {Collection.class};
      case null, default -> throw new RuntimeException("不支持的Mapper方法：" + methodName);
    };
  }

  /**
   * 获取BaseMapper<T>中T的具体DO类
   *
   * @param mapper BaseMapper代理对象
   * @return 泛型对应的具体DO类，如OrderItemDO.class
   */
  private Class<?> getMapperGenericType(BaseMapper<?> mapper) {
    // 1. 获取Mapper代理对象的真实接口（如OrderItemMapper）
    Class<?>[] interfaces = mapper.getClass().getInterfaces();
    for (Class<?> mapperInterface : interfaces) {
      // 2. 遍历接口的泛型父接口，找到BaseMapper
      Type[] genericInterfaces = mapperInterface.getGenericInterfaces();
      for (Type genericInterface : genericInterfaces) {
        // 3. 仅处理参数化类型（BaseMapper<T>）
        if (genericInterface instanceof ParameterizedType parameterizedType) {
          Type rawType = parameterizedType.getRawType();
          // 4. 确认是BaseMapper
          if (rawType == BaseMapper.class) {
            // 5. 获取泛型实际类型（T）
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            if (actualTypeArguments.length > 0) {
              return (Class<?>) actualTypeArguments[0];
            }
          }
        }
      }
    }
    throw new RuntimeException("无法解析Mapper的泛型类型：" + mapper.getClass().getName());
  }

  private void doMapper(BaseMapper<?> mapper, Object param, Method method)
      throws IllegalAccessException, InvocationTargetException {
    if (debug) {
      System.out.println("method：" + method.getName());
      System.out.println("param：" + param);
      System.out.println("mapper：" + getMapperGenericType(mapper).getName());
      System.out.println();
    } else {
      method.invoke(mapper, param);
    }
  }
}
