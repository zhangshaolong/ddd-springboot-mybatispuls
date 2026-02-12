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

  // 常量：主实体版本数字段名、主实体识别标记（可根据项目调整）
  private static final String VERSION_FIELD_NAME = "version";
  private static final String AGGREGATE_ROOT_MARK = "AggregateRoot"; // 主实体类名特征

  /**
   * 核心改造：任意实体增删改 → 主实体版本号+1
   *
   * @param changes 聚合根变更结果
   */
  @SuppressWarnings({"unchecked"})
  public void persist(AggregateChanges changes) {
    Map<Class<?>, AggregateChanges.TableChanges<?>> tableMap = changes.getTableChangesMap();
    if (tableMap == null || tableMap.isEmpty()) {
      return;
    }

    // ========== 核心步骤1：判断是否有任何实体变更（增/删/改） ==========
    boolean hasAnyChange =
        tableMap.values().stream()
            .anyMatch(
                tc ->
                    (tc.getInsertList() != null && !tc.getInsertList().isEmpty())
                        || (tc.getUpdateList() != null && !tc.getUpdateList().isEmpty())
                        || (tc.getDeleteList() != null && !tc.getDeleteList().isEmpty()));

    // 无变更直接返回
    if (!hasAnyChange) {
      return;
    }

    // ========== 核心步骤2：找到主实体（聚合根）对应的DO和Mapper ==========
    Class<?> aggregateRootDoClass = findAggregateRootDoClass(tableMap);
    if (aggregateRootDoClass == null) {
      throw new RuntimeException("未找到聚合根（主实体）对应的DO类，无法更新版本号");
    }
    BaseMapper<?> aggregateRootMapper = getBaseMapper(aggregateRootDoClass);
    if (aggregateRootMapper == null) {
      throw new RuntimeException("未找到聚合根DO[" + aggregateRootDoClass + "]对应的Mapper");
    }

    // ========== 核心步骤3：获取主实体DO实例并强制自增版本号 ==========
    Object aggregateRootDo = getAggregateRootDoInstance(changes, tableMap, aggregateRootDoClass);
    if (aggregateRootDo == null) {
      throw new RuntimeException("未找到聚合根DO实例，无法更新版本号");
    }
    // 强制自增版本号（无论主实体自身是否变更）
    incrementVersionForce(aggregateRootDo);
    // 填充通用更新字段（updateTime/updateBy等）
    commonFieldHandler.fillCommonFields(aggregateRootDo, EntityChangeType.MODIFIED);

    // ========== 核心步骤4：先处理所有子实体变更，最后强制更新主实体版本号 ==========
    // 1. 遍历处理所有实体的增/删/改（原有逻辑）
    for (Map.Entry<Class<?>, AggregateChanges.TableChanges<?>> entry : tableMap.entrySet()) {
      Class<?> doClass = entry.getKey();
      AggregateChanges.TableChanges<?> tableChanges = entry.getValue();
      BaseMapper<?> mapper = getBaseMapper(doClass);
      if (mapper == null) {
        throw new RuntimeException("未找到DO[" + doClass + "]对应的Mapper");
      }
      AggregateChanges.TableChanges<Object> typedTable =
          (AggregateChanges.TableChanges<Object>) tableChanges;

      // 处理新增
      handleInsert(mapper, typedTable);
      // 处理更新（主实体更新已被版本自增覆盖，此处仅处理子实体）
      handleUpdate(mapper, typedTable, doClass != aggregateRootDoClass);
      // 处理删除（所有实体物理删除）
      handleDelete(mapper, typedTable);
    }

    // 2. 强制更新主实体版本号（即使主实体自身无字段变更）
    invokeMapperMethod(aggregateRootMapper, "updateById", aggregateRootDo);
  }

  // ========== 新增：处理新增逻辑（抽离复用） ==========
  private void handleInsert(
      BaseMapper<?> mapper, AggregateChanges.TableChanges<Object> typedTable) {
    if (typedTable.getInsertList() != null && !typedTable.getInsertList().isEmpty()) {
      List<Object> insertList = typedTable.getInsertList();
      insertList.forEach(doObj -> commonFieldHandler.fillCommonFields(doObj, EntityChangeType.NEW));
      if (insertList.size() == 1) {
        invokeMapperMethod(mapper, "insert", insertList.getFirst());
      } else {
        invokeMapperMethod(mapper, "saveBatch", insertList);
      }
    }
  }

  // ========== 新增：处理更新逻辑（仅子实体） ==========
  private void handleUpdate(
      BaseMapper<?> mapper,
      AggregateChanges.TableChanges<Object> typedTable,
      boolean isChildEntity) {
    if (!isChildEntity
        || typedTable.getUpdateList() == null
        || typedTable.getUpdateList().isEmpty()) {
      return;
    }
    List<Object> updateList = typedTable.getUpdateList();
    updateList.forEach(
        doObj -> commonFieldHandler.fillCommonFields(doObj, EntityChangeType.MODIFIED));
    if (updateList.size() == 1) {
      invokeMapperMethod(mapper, "updateById", updateList.getFirst());
    } else {
      invokeMapperMethod(mapper, "updateBatchById", updateList);
    }
  }

  // ========== 新增：处理删除逻辑（所有实体物理删除） ==========
  private void handleDelete(
      BaseMapper<?> mapper, AggregateChanges.TableChanges<Object> typedTable) {
    if (typedTable.getDeleteList() == null || typedTable.getDeleteList().isEmpty()) {
      return;
    }
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

  // ========== 核心工具方法：找到主实体DO类 ==========
  private Class<?> findAggregateRootDoClass(
      Map<Class<?>, AggregateChanges.TableChanges<?>> tableMap) {
    // 规则1：类名包含"AggregateRoot"或主实体特征（如OrderDO、UserDO，可根据项目调整）
    for (Class<?> doClass : tableMap.keySet()) {
      if (doClass.getSimpleName().contains(AGGREGATE_ROOT_MARK)
          || isAggregateRootDOByField(doClass)) {
        return doClass;
      }
    }
    // 规则2：退化为第一个包含version字段的DO（兜底）
    return tableMap.keySet().stream()
        .filter(this::isAggregateRootDOByField)
        .findFirst()
        .orElse(null);
  }

  // ========== 核心工具方法：判断是否为主实体DO（基于version字段） ==========
  private boolean isAggregateRootDOByField(Class<?> doClass) {
    try {
      doClass.getDeclaredField(VERSION_FIELD_NAME);
      return true;
    } catch (NoSuchFieldException e) {
      Class<?> superClass = doClass.getSuperclass();
      if (superClass != null && superClass != Object.class) {
        return isAggregateRootDOByField(superClass);
      }
      return false;
    }
  }

  // ========== 核心工具方法：获取主实体DO实例 ==========
  private Object getAggregateRootDoInstance(
      AggregateChanges changes,
      Map<Class<?>, AggregateChanges.TableChanges<?>> tableMap,
      Class<?> aggregateRootDoClass) {
    // 1. 先从更新列表找主实体DO
    AggregateChanges.TableChanges<?> rootTableChanges = tableMap.get(aggregateRootDoClass);
    if (rootTableChanges != null
        && rootTableChanges.getUpdateList() != null
        && !rootTableChanges.getUpdateList().isEmpty()) {
      return rootTableChanges.getUpdateList().getFirst();
    }
    // 2. 从删除列表找（物理删除前仍需更新版本）
    if (rootTableChanges != null
        && rootTableChanges.getDeleteList() != null
        && !rootTableChanges.getDeleteList().isEmpty()) {
      return rootTableChanges.getDeleteList().getFirst();
    }
    // 3. 从新增列表找（新增主实体时版本初始化为1）
    if (rootTableChanges != null
        && rootTableChanges.getInsertList() != null
        && !rootTableChanges.getInsertList().isEmpty()) {
      return rootTableChanges.getInsertList().getFirst();
    }
    // 4. 兜底：从AggregateChanges获取聚合根版本，反射创建DO实例（需保证DO有默认构造器）
    try {
      Object doObj = aggregateRootDoClass.getDeclaredConstructor().newInstance();
      Field idField = getFieldRecursively(doObj.getClass(), "id");
      idField.setAccessible(true);
      idField.set(doObj, changes.getAggregateRootId());
      return doObj;
    } catch (Exception e) {
      throw new RuntimeException("创建聚合根DO实例失败", e);
    }
  }

  // ========== 核心工具方法：强制自增版本号（无视原有值） ==========
  private void incrementVersionForce(Object doObj) {
    try {
      Field versionField = getFieldRecursively(doObj.getClass(), VERSION_FIELD_NAME);
      versionField.setAccessible(true);
      Long currentVersion = (Long) versionField.get(doObj);
      // 版本号初始化为1（新增）或+1（已有）
      Long newVersion = currentVersion == null ? 1 : currentVersion + 1;
      versionField.set(doObj, newVersion);
      if (debug) {
        System.out.println("主实体版本号强制更新：" + currentVersion + " → " + newVersion);
      }
    } catch (Exception e) {
      throw new RuntimeException("强制自增主实体版本号失败，DO类：" + doObj.getClass().getName(), e);
    }
  }

  // ========== 原有工具方法（保留+优化） ==========
  private static Long getId(Object doObj) throws IllegalAccessException {
    Field idField = getFieldRecursively(doObj.getClass(), "id");
    idField.setAccessible(true);
    return (Long) idField.get(doObj);
  }

  private static Field getFieldRecursively(Class<?> clazz, String fieldName) {
    try {
      return clazz.getDeclaredField(fieldName);
    } catch (NoSuchFieldException e) {
      Class<?> superClass = clazz.getSuperclass();
      if (superClass == null || superClass == Object.class) {
        throw new RuntimeException("DO[" + clazz + "]未找到字段：" + fieldName);
      }
      return getFieldRecursively(superClass, fieldName);
    }
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

  private void invokeMapperMethod(BaseMapper<?> mapper, String methodName, Object param) {
    try {
      Class<?>[] paramTypes = getBaseMapperParamType(methodName, param);
      Method method = mapper.getClass().getMethod(methodName, paramTypes);
      doMapper(mapper, param, method);
    } catch (NoSuchMethodException e) {
      try {
        Method method =
            BaseMapper.class.getMethod(methodName, getBaseMapperParamType(methodName, param));
        doMapper(mapper, param, method);
      } catch (Exception ex) {
        throw new RuntimeException("Mapper方法调用失败：" + methodName, ex);
      }
    } catch (Exception e) {
      throw new RuntimeException("调用Mapper方法失败：" + methodName, e);
    }
  }

  private Class<?>[] getBaseMapperParamType(String methodName, Object param) {
    return getClassType(methodName);
  }

  private static Class<?>[] getClassType(String methodName) {
    return switch (methodName) {
      case "insert", "updateById" -> new Class[] {Object.class};
      case "deleteById" -> new Class[] {Serializable.class};
      case "saveBatch", "updateBatchById", "deleteBatchIds" -> new Class[] {Collection.class};
      case null, default -> throw new RuntimeException("不支持的Mapper方法：" + methodName);
    };
  }

  private static Class<?>[] getClasses(String methodName) {
    return getClassType(methodName);
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
      System.out.println("method：" + method.getName());
      System.out.println("param：" + param);
      System.out.println("mapper：" + getMapperGenericType(mapper).getName());
      System.out.println();
    } else {
      method.invoke(mapper, param);
    }
  }
}
