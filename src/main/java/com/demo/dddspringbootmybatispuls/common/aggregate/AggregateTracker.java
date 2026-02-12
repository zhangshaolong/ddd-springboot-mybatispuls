package com.demo.dddspringbootmybatispuls.common.aggregate;

import com.demo.dddspringbootmybatispuls.common.mapper.StructMapper;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public class AggregateTracker {

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
   * 对比快照与当前聚合根，生成变更结果（核心修改：支持主实体deleted处理+级联删除子实体）
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
    result.setAggregateVersion(aggregateRoot.getVersion());
    result.setAggregateRootId(aggregateRoot.getId());
    Map<Class<?>, AggregateChanges.TableChanges<?>> tableMap = new HashMap<>();
    Set<Object> processedKeys = new HashSet<>();

    boolean isRootDeleted = false;
    Object rootId = getEntityId(aggregateRoot);
    Object rootSnapshotKey =
        rootId == null ? findTempKeyByEntity(snapshotMap, aggregateRoot) : rootId;
    if (rootSnapshotKey != null) {
      BaseDomainEntity rootSnapshot = snapshotMap.get(rootSnapshotKey);
      if (rootSnapshot instanceof AggregateRoot) {
        boolean snapshotDeleted = isAggregateRootDeleted((AggregateRoot) rootSnapshot);
        boolean currentDeleted = isAggregateRootDeleted(aggregateRoot);
        isRootDeleted = !snapshotDeleted && currentDeleted;
      }
    }

    // 1. 收集当前所有实体
    List<BaseDomainEntity> currentEntities = collectAllEntities(aggregateRoot);

    // 2. 处理新增/修改的实体
    for (BaseDomainEntity currentEntity : currentEntities) {
      Object currentId = getEntityId(currentEntity);
      Object snapshotKey =
          currentId == null ? findTempKeyByEntity(snapshotMap, currentEntity) : currentId;

      if (isRootDeleted && !(currentEntity instanceof AggregateRoot)) {
        // 子实体：直接加入删除列表
        if (snapshotKey != null) {
          addDeletedEntityChange(tableMap, snapshotMap.get(snapshotKey), entityDoMapping);
          processedKeys.add(snapshotKey);
        }
        continue;
      }

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
        }
        processedKeys.add(snapshotKey);
      }
    }

    // 3. 处理删除的实体（快照中有，当前无）
    for (Map.Entry<Object, BaseDomainEntity> entry : snapshotMap.entrySet()) {
      if (!processedKeys.contains(entry.getKey())) {
        BaseDomainEntity snapshotEntity = entry.getValue();
        if (isRootDeleted || !(snapshotEntity instanceof AggregateRoot)) {
          addDeletedEntityChange(tableMap, snapshotEntity, entityDoMapping);
        }
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

  private boolean isAggregateRootDeleted(AggregateRoot aggregateRoot) {
    return aggregateRoot.getDeleted() == 1;
  }

  private List<BaseDomainEntity> collectAllEntities(BaseDomainEntity root) {
    List<BaseDomainEntity> entities = new ArrayList<>();
    // 新增：去重集合，避免循环引用
    Set<Object> collectedIds = new HashSet<>();
    collectAllEntitiesRecursive(root, entities, collectedIds);
    return entities;
  }

  // 拆分递归方法，增加去重
  private void collectAllEntitiesRecursive(
      BaseDomainEntity root, List<BaseDomainEntity> entities, Set<Object> collectedIds) {
    if (root == null) {
      return;
    }
    Object id = getEntityId(root);
    // 已收集过的实体跳过（无ID的临时实体用引用去重）
    Object uniqueKey = id == null ? root : id;
    if (collectedIds.contains(uniqueKey)) {
      return;
    }
    collectedIds.add(uniqueKey);
    entities.add(root);

    Field[] fields = root.getClass().getDeclaredFields();
    for (Field field : fields) {
      field.setAccessible(true);
      try {
        Object fieldValue = field.get(root);
        if (null == fieldValue) continue;
        switch (fieldValue) {
          case List<?> list -> {
            for (Object item : list) {
              if (item instanceof BaseDomainEntity) {
                collectAllEntitiesRecursive((BaseDomainEntity) item, entities, collectedIds);
              }
            }
          }
          case BaseDomainEntity baseDomainEntity ->
              collectAllEntitiesRecursive(baseDomainEntity, entities, collectedIds);
          default -> {}
        }
      } catch (IllegalAccessException e) {
        // 优化异常信息：增加字段名，便于定位问题
        throw new RuntimeException(
            "扫描聚合根子实体失败，实体类：" + root.getClass().getName() + "，字段：" + field.getName(), e);
      }
    }
  }

  /** 获取实体的主键值 */
  private Object getEntityId(BaseDomainEntity entity) {
    return entity.getId();
  }

  private Set<String> compareEntityFields(BaseDomainEntity snapshot, BaseDomainEntity current) {
    Set<String> changedFields = new HashSet<>();
    // 遍历当前类+父类的所有非静态字段（排除无效字段）
    Class<?> clazz = snapshot.getClass();
    while (clazz != Object.class) {
      Field[] fields = clazz.getDeclaredFields();
      for (Field field : fields) {
        // 排除静态字段、transient字段
        if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
          continue;
        }
        // 排除序列化字段（可选）
        if ("serialVersionUID".equals(field.getName())) {
          continue;
        }
        field.setAccessible(true);
        try {
          Object snapshotValue = field.get(snapshot);
          Object currentValue = field.get(current);
          if (!Objects.equals(snapshotValue, currentValue)) {
            changedFields.add(field.getName());
          }
        } catch (IllegalAccessException e) {
          throw new RuntimeException(
              "对比实体字段失败，实体类：" + snapshot.getClass().getName() + "，字段：" + field.getName(), e);
        }
      }
      clazz = clazz.getSuperclass();
    }
    return changedFields;
  }

  /** 查找新增实体的临时key（快照中无ID的实体） */
  private Object findTempKeyByEntity(
      Map<Object, BaseDomainEntity> snapshotMap, BaseDomainEntity entity) {
    // 无ID的实体是新增候选，通过类+字段浅对比匹配快照中的临时实体
    for (Map.Entry<Object, BaseDomainEntity> entry : snapshotMap.entrySet()) {
      BaseDomainEntity snapshotEntity = entry.getValue();
      if (!snapshotEntity.getClass().equals(entity.getClass())) {
        continue;
      }
      // 2. 都是无ID实体（临时实体）
      if (snapshotEntity.getId() == null && entity.getId() == null) {
        Set<String> diffFields = compareEntityFields(snapshotEntity, entity);
        if (diffFields.isEmpty()) {
          return entry.getKey();
        }
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

  /** 删除实体→DO的deleteList（子实体级联删除会触发此逻辑） */
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
