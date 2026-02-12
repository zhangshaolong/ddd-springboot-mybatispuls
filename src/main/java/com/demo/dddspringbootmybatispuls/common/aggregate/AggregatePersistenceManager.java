package com.demo.dddspringbootmybatispuls.common.aggregate;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.io.Serializable;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j
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
      log.info("无变更需要持久化");
      return;
    }

    if (changes.getAggregateRootId() != null) {
      log.info("开始持久化聚合根[ID:{}]的变更，涉及 {} 张表", changes.getAggregateRootId(), tableMap.size());
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

      // 新增逻辑
      if (typedTable.getInsertList() != null && !typedTable.getInsertList().isEmpty()) {
        List<Object> insertList = typedTable.getInsertList();
        insertList.forEach(
            doObj -> commonFieldHandler.fillCommonFields(doObj, EntityChangeType.NEW));

        log.info("新增{}条[{}]数据", insertList.size(), doClass.getSimpleName());
        if (insertList.size() == 1) {
          invokeMapperMethod(mapper, "insert", insertList.getFirst());
        } else {
          invokeMapperMethod(mapper, "saveBatch", insertList);
        }
      }

      // 修改逻辑
      if (typedTable.getUpdateList() != null && !typedTable.getUpdateList().isEmpty()) {
        List<Object> updateList = typedTable.getUpdateList();
        updateList.forEach(
            doObj -> commonFieldHandler.fillCommonFields(doObj, EntityChangeType.MODIFIED));

        log.info("修改{}条[{}]数据", updateList.size(), doClass.getSimpleName());
        if (updateList.size() == 1) {
          invokeMapperMethod(mapper, "updateById", updateList.getFirst());
        } else {
          invokeMapperMethod(mapper, "updateBatchById", updateList);
        }
      }

      // 删除逻辑（物理删除）
      if (typedTable.getDeleteList() != null && !typedTable.getDeleteList().isEmpty()) {
        List<Object> deleteList = typedTable.getDeleteList();
        List<Long> deleteIds = new ArrayList<>();

        for (Object doObj : deleteList) {
          if (doObj == null) {
            log.warn("删除列表中存在null的DO对象，跳过");
            continue;
          }

          try {
            Long id = getId(doObj);
            if (id == null) {
              log.warn("DO[{}]的id为null，跳过删除", doObj.getClass().getSimpleName());
              continue;
            }
            deleteIds.add(id);
          } catch (Exception e) {
            throw new RuntimeException("获取DO[" + doObj.getClass() + "]的id失败", e);
          }
        }

        if (!deleteIds.isEmpty()) {
          log.info("物理删除{}条[{}]数据，ID列表：{}", deleteIds.size(), doClass.getSimpleName(), deleteIds);
          invokeMapperMethod(mapper, "deleteBatchIds", deleteIds);
        } else {
          log.warn("无有效删除ID，跳过[{}]的批量删除", doClass.getSimpleName());
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

  @SuppressWarnings("rawtypes")
  public BaseMapper getBaseMapper(Class<?> targetDoClass) {
    Map<String, BaseMapper> allMappers = applicationContext.getBeansOfType(BaseMapper.class);
    for (BaseMapper mapper : allMappers.values()) {
      Class<?> mapperDoClass = resolveMapperGenericType(mapper.getClass().getInterfaces()[0]);
      if (mapperDoClass != null && mapperDoClass.equals(targetDoClass)) {
        return mapper;
      }
    }
    return null;
  }

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

  /** 核心修复：从Mapper接口获取方法，而非代理类 */
  private void invokeMapperMethod(BaseMapper<?> mapper, String methodName, Object param) {
    try {
      // 步骤1：获取Mapper的原始接口（如OrderMapper），而非代理类
      Class<?>[] interfaces = mapper.getClass().getInterfaces();
      Class<?> mapperInterface = null;
      for (Class<?> iface : interfaces) {
        // 找到继承BaseMapper的接口
        if (BaseMapper.class.isAssignableFrom(iface) && iface != BaseMapper.class) {
          mapperInterface = iface;
          break;
        }
      }
      // 兜底：直接用BaseMapper接口
      if (mapperInterface == null) {
        mapperInterface = BaseMapper.class;
      }

      // 步骤2：获取精准的参数类型
      Class<?>[] paramTypes = getParamTypes(methodName, param);

      // 步骤3：从接口获取方法（关键：避免从代理类获取）
      Method method = mapperInterface.getMethod(methodName, paramTypes);

      // 步骤4：执行方法
      doMapper(mapper, param, method);
    } catch (NoSuchMethodException e) {
      // 兜底：尝试从BaseMapper接口获取（兼容自定义Mapper未继承全部方法的场景）
      try {
        Class<?>[] paramTypes = getParamTypes(methodName, param);
        Method method = BaseMapper.class.getMethod(methodName, paramTypes);
        doMapper(mapper, param, method);
      } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
        throw new RuntimeException(
            "未找到Mapper方法：" + methodName + "，参数类型：" + param.getClass().getName(), ex);
      }
    } catch (Exception e) {
      throw new RuntimeException("调用Mapper方法失败：" + methodName, e);
    }
  }

  /** 精准匹配BaseMapper方法的参数类型 */
  private Class<?>[] getParamTypes(String methodName, Object param) {
    return switch (methodName) {
      // insert/updateById：参数是具体的DO对象（统一为Object.class，兼容所有DO）
      case "insert", "updateById" -> new Class[] {Object.class};
      // deleteById：参数是Serializable（ID的父类）
      case "deleteById" -> new Class[] {Serializable.class};
      // saveBatch/updateBatchById/deleteBatchIds：参数是Collection
      case "saveBatch", "updateBatchById", "deleteBatchIds" -> new Class[] {Collection.class};
      default -> throw new RuntimeException("不支持的Mapper方法：" + methodName);
    };
  }

  /** 兼容旧的getClasses方法（兜底） */
  @Deprecated
  private static Class<?>[] getClasses(String methodName) {
    return switch (methodName) {
      case "insert", "updateById" -> new Class[] {Object.class};
      case "deleteById" -> new Class[] {Serializable.class};
      case "saveBatch", "updateBatchById", "deleteBatchIds" -> new Class[] {Collection.class};
      default -> throw new RuntimeException("不支持的Mapper方法：" + methodName);
    };
  }

  private Class<?> getMapperGenericType(BaseMapper<?> mapper) {
    Class<?>[] interfaces = mapper.getClass().getInterfaces();
    for (Class<?> mapperInterface : interfaces) {
      Type[] genericInterfaces = mapperInterface.getGenericInterfaces();
      for (Type genericInterface : genericInterfaces) {
        if (genericInterface instanceof ParameterizedType parameterizedType) {
          Type rawType = parameterizedType.getRawType();
          if (rawType == BaseMapper.class) {
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
      log.debug("调试模式：调用Mapper方法[{}]，参数[{}]", method.getName(), param);
    } else {
      // 执行方法（动态代理对象支持调用接口方法）
      method.invoke(mapper, param);
    }
  }
}
