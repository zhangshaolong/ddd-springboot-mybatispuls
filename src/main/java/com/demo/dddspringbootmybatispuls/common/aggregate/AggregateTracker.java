package com.demo.dddspringbootmybatispuls.common.aggregate;

import com.demo.dddspringbootmybatispuls.common.mapper.StructMapper;
import java.lang.reflect.Field;
import java.util.*;
import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

@Data
@Component
public class AggregateTracker {
  private static final String ID_FIELD_NAME = "id";

  private Map<Object, BaseDomainEntity> snapshotMap = new HashMap<>();

  /**
   * 生成聚合根快照（深拷贝所有实体）
   *
   * @param aggregateRoot 聚合根实例
   * @return 快照映射（key=实体ID/临时UUID，value=深拷贝后的实体）
   */
  public Map<Object, BaseDomainEntity> buildSnapshot(AggregateRoot aggregateRoot) {
    Map<Object, BaseDomainEntity> snapshotMap = new HashMap<>();
    // 递归收集聚合根下所有实体
    List<BaseDomainEntity> allEntities = collectAllEntities(aggregateRoot);
    for (BaseDomainEntity entity : allEntities) {
      Object id = getEntityId(entity);
      Object key = id == null ? UUID.randomUUID().toString() : id;
      snapshotMap.put(key, entity.clone());
    }
    return this.snapshotMap = snapshotMap;
  }

  /**
   * 对比快照与当前聚合根，生成变更结果
   *
   * @param snapshotMap 历史快照
   * @param aggregateRoot 当前聚合根
   * @param entityDoMapping 实体→DO类型映射
   * @return 聚合根变更结果
   */
  public <T extends AggregateRoot> AggregateChanges compareChanges(
      Map<Object, BaseDomainEntity> snapshotMap,
      T aggregateRoot,
      Map<Class<?>, Class<?>> entityDoMapping) {
    AggregateChanges result = new AggregateChanges();
    Map<Class<?>, AggregateChanges.TableChanges<?>> tableMap = new HashMap<>();
    Set<Object> processedKeys = new HashSet<>();

    // 1. 收集当前所有实体
    List<BaseDomainEntity> currentEntities = collectAllEntities(aggregateRoot);

    // 2. 处理新增/修改的实体
    for (BaseDomainEntity currentEntity : currentEntities) {
      Object currentId = getEntityId(currentEntity);
      Object snapshotKey =
          currentId == null ? findTempKeyByEntity(snapshotMap, currentEntity) : currentId;

      // 新增实体（快照中无对应key）
      if (snapshotKey == null) {
        addNewEntityChange(tableMap, currentEntity, entityDoMapping);
        continue;
      }

      // 修改实体（快照中有对应key，且字段有变更）
      BaseDomainEntity snapshotEntity = snapshotMap.get(snapshotKey);
      if (snapshotEntity != null) {
        Set<String> changedFields = compareEntityFields(snapshotEntity, currentEntity);
        if (!changedFields.isEmpty()) {
          addModifiedEntityChange(tableMap, currentEntity, changedFields, entityDoMapping);
          // 聚合根本身修改时，版本号自增
          if (currentEntity instanceof AggregateRoot) {
            ((AggregateRoot) currentEntity).incrVersion();
            result.setAggregateVersion(((AggregateRoot) currentEntity).getVersion());
          }
        }
        processedKeys.add(snapshotKey);
      }
    }

    // 3. 处理删除的实体（快照中有，当前无）
    for (Map.Entry<Object, BaseDomainEntity> entry : snapshotMap.entrySet()) {
      if (!processedKeys.contains(entry.getKey())) {
        addDeletedEntityChange(tableMap, entry.getValue(), entityDoMapping);
      }
    }

    result.setTableChangesMap(tableMap);
    return result;
  }

  /**
   * 对比快照与当前聚合根，生成变更结果
   *
   * @param aggregateRoot 当前聚合根
   * @param entityDoMapping 实体→DO类型映射
   * @return 聚合根变更结果
   */
  public <T extends AggregateRoot> AggregateChanges compareChanges(
      T aggregateRoot, Map<Class<?>, Class<?>> entityDoMapping) {

    return compareChanges(this.snapshotMap, aggregateRoot, entityDoMapping);
  }

  private List<BaseDomainEntity> collectAllEntities(BaseDomainEntity root) {
    List<BaseDomainEntity> entities = new ArrayList<>();
    entities.add(root);

    Field[] fields = root.getClass().getDeclaredFields();
    for (Field field : fields) {
      field.setAccessible(true);
      try {
        Object fieldValue = field.get(root);
        switch (fieldValue) {
          case null -> {
            continue;
          }

          // 处理List<BaseDomainEntity>类型子实体
          case List<?> list -> {
            for (Object item : list) {
              if (item instanceof BaseDomainEntity) {
                entities.addAll(collectAllEntities((BaseDomainEntity) item));
              }
            }
          }
          // 处理单个BaseDomainEntity类型子实体
          case BaseDomainEntity baseDomainEntity ->
              entities.addAll(collectAllEntities(baseDomainEntity));
          default -> {}
        }

      } catch (IllegalAccessException e) {
        throw new RuntimeException("扫描聚合根子实体失败", e);
      }
    }
    return entities;
  }

  /** 获取实体的主键值 */
  private Object getEntityId(BaseDomainEntity entity) {
    Field idField = ReflectionUtils.findField(entity.getClass(), ID_FIELD_NAME);
    if (idField == null) {
      throw new RuntimeException("实体[" + entity.getClass() + "]无" + ID_FIELD_NAME + "字段");
    }
    idField.setAccessible(true);
    try {
      return idField.get(entity);
    } catch (IllegalAccessException e) {
      throw new RuntimeException("获取实体ID失败", e);
    }
  }

  /** 对比两个实体的字段差异 */
  private Set<String> compareEntityFields(BaseDomainEntity snapshot, BaseDomainEntity current) {
    Set<String> changedFields = new HashSet<>();
    Field[] fields = snapshot.getClass().getDeclaredFields();
    for (Field field : fields) {
      field.setAccessible(true);
      try {
        Object snapshotValue = field.get(snapshot);
        Object currentValue = field.get(current);
        if (!Objects.equals(snapshotValue, currentValue)) {
          changedFields.add(field.getName());
        }
      } catch (IllegalAccessException e) {
        throw new RuntimeException("对比实体字段失败", e);
      }
    }
    return changedFields;
  }

  /** 查找新增实体的临时key（快照中无ID的实体） */
  private Object findTempKeyByEntity(
      Map<Object, BaseDomainEntity> snapshotMap, BaseDomainEntity entity) {
    for (Map.Entry<Object, BaseDomainEntity> entry : snapshotMap.entrySet()) {
      if (entry.getValue().equals(entity)) {
        return entry.getKey();
      }
    }
    return null;
  }

  /** 新增实体→DO的insertList */
  private void addNewEntityChange(
      Map<Class<?>, AggregateChanges.TableChanges<?>> tableMap,
      BaseDomainEntity entity,
      Map<Class<?>, Class<?>> entityDoMapping) {
    Class<?> doClass = entityDoMapping.get(entity.getClass());
    if (doClass == null) {
      throw new RuntimeException("未配置实体[" + entity.getClass() + "]的DO映射");
    }
    Object doObj = StructMapper.to(entity, doClass);
    AggregateChanges.TableChanges<?> tableChanges =
        tableMap.computeIfAbsent(doClass, k -> new AggregateChanges.TableChanges<>());

    @SuppressWarnings("unchecked")
    AggregateChanges.TableChanges<Object> typedTable =
        (AggregateChanges.TableChanges<Object>) tableChanges;
    if (typedTable.getInsertList() == null) {
      typedTable.setInsertList(new ArrayList<>());
    }
    typedTable.getInsertList().add(doObj);
  }

  /** 修改实体→DO的updateList */
  private void addModifiedEntityChange(
      Map<Class<?>, AggregateChanges.TableChanges<?>> tableMap,
      BaseDomainEntity entity,
      Set<String> changedFields,
      Map<Class<?>, Class<?>> entityDoMapping) {
    Class<?> doClass = entityDoMapping.get(entity.getClass());
    if (doClass == null) {
      throw new RuntimeException("未配置实体[" + entity.getClass() + "]的DO映射");
    }
    Object doObj = StructMapper.to(entity, doClass);
    AggregateChanges.TableChanges<?> tableChanges =
        tableMap.computeIfAbsent(doClass, k -> new AggregateChanges.TableChanges<>());

    @SuppressWarnings("unchecked")
    AggregateChanges.TableChanges<Object> typedTable =
        (AggregateChanges.TableChanges<Object>) tableChanges;
    if (typedTable.getUpdateList() == null) {
      typedTable.setUpdateList(new ArrayList<>());
    }
    typedTable.getUpdateList().add(doObj);
  }

  /** 删除实体→DO的deleteList */
  private void addDeletedEntityChange(
      Map<Class<?>, AggregateChanges.TableChanges<?>> tableMap,
      BaseDomainEntity entity,
      Map<Class<?>, Class<?>> entityDoMapping) {
    Class<?> doClass = entityDoMapping.get(entity.getClass());
    if (doClass == null) {
      throw new RuntimeException("未配置实体[" + entity.getClass() + "]的DO映射");
    }
    Object doObj = StructMapper.to(entity, doClass);
    AggregateChanges.TableChanges<?> tableChanges =
        tableMap.computeIfAbsent(doClass, k -> new AggregateChanges.TableChanges<>());

    @SuppressWarnings("unchecked")
    AggregateChanges.TableChanges<Object> typedTable =
        (AggregateChanges.TableChanges<Object>) tableChanges;
    if (typedTable.getDeleteList() == null) {
      typedTable.setDeleteList(new ArrayList<>());
    }
    typedTable.getDeleteList().add(doObj);
  }
}
