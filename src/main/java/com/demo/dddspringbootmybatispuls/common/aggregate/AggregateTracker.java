package com.demo.dddspringbootmybatispuls.common.aggregate;

import com.demo.dddspringbootmybatispuls.common.mapper.StructMapper;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

@Slf4j
@Data
@Component
public class AggregateTracker {
  private static final String ID_FIELD_NAME = "id";
  private Map<Object, BaseDomainEntity> snapshotMap = new HashMap<>();

  /** 生成聚合根快照（深拷贝所有实体） */
  public Map<Object, BaseDomainEntity> buildSnapshot(AggregateRoot aggregateRoot) {
    Map<Object, BaseDomainEntity> snapshotMap = new HashMap<>();
    List<BaseDomainEntity> allEntities = collectAllEntities(aggregateRoot);
    for (BaseDomainEntity entity : allEntities) {
      Object id = getEntityId(entity);
      Object key = id == null ? UUID.randomUUID().toString() : id;
      snapshotMap.put(key, deepClone(entity));
    }
    log.info("快照生成完成，包含 {} 个实体", snapshotMap.size());
    return this.snapshotMap = snapshotMap;
  }

  /** 对比快照与当前聚合根（核心修复：仅主实体deleted=1时才删主实体，子实体置null仅删子实体） */
  public <T extends AggregateRoot> AggregateChanges compareChanges(
      Map<Object, BaseDomainEntity> snapshotMap,
      T aggregateRoot,
      Map<Class<?>, Class<?>> entityDoMapping) {
    AggregateChanges result = new AggregateChanges();
    Map<Class<?>, AggregateChanges.TableChanges<?>> tableMap = new HashMap<>();
    Set<Object> processedKeys = new HashSet<>();
    boolean hasAnyChange = false;
    // 仅当主实体deleted从0→1时，才标记主实体删除（核心：严格区分主实体删除和子实体移除）
    boolean isRootNeedDelete = false;

    // 1. 收集当前所有实体（主实体+未被置null的子实体）
    List<BaseDomainEntity> currentEntities = collectAllEntities(aggregateRoot);
    // 2. 处理主实体本身（核心：主实体永远不参与“快照有、当前无”的删除逻辑）
    Object rootId = getEntityId(aggregateRoot);
    Object rootSnapshotKey =
        rootId == null ? findTempKeyByEntity(snapshotMap, aggregateRoot) : rootId;
    if (rootSnapshotKey != null) {
      BaseDomainEntity rootSnapshot = snapshotMap.get(rootSnapshotKey);
      // 仅当主实体deleted从0→1时，才标记主实体需要删除
      if (aggregateRoot.isDeletedStatusChanged(rootSnapshot)) {
        isRootNeedDelete = true;
        hasAnyChange = true;
        log.info("主实体[{}]标记删除（deleted=1），将删除主实体并级联删除子实体", aggregateRoot.getClass().getSimpleName());
        // 主实体加入删除列表
        addDeletedEntityChange(tableMap, aggregateRoot, entityDoMapping);
        processedKeys.add(rootSnapshotKey);
      } else {
        // 主实体未标记删除：对比主实体字段变更（如payment置null属于主实体字段变更）
        Set<String> rootChangedFields = compareEntityFields(rootSnapshot, aggregateRoot);
        if (!rootChangedFields.isEmpty()) {
          addModifiedEntityChange(tableMap, aggregateRoot, rootChangedFields, entityDoMapping);
          hasAnyChange = true;
        }
        processedKeys.add(rootSnapshotKey);
      }
    } else if (rootId != null) {
      // 主实体是新增（快照无）：加入新增列表
      addNewEntityChange(tableMap, aggregateRoot, entityDoMapping);
      hasAnyChange = true;
    }

    // 3. 处理子实体（核心：区分“主实体删除级联删子实体”和“子实体置null删子实体”）
    for (BaseDomainEntity currentEntity : currentEntities) {
      // 跳过主实体（已单独处理）
      if (currentEntity instanceof AggregateRoot && currentEntity == aggregateRoot) {
        continue;
      }

      Object currentId = getEntityId(currentEntity);
      Object snapshotKey =
          currentId == null ? findTempKeyByEntity(snapshotMap, currentEntity) : currentId;

      // 场景1：主实体标记删除 → 级联删除所有子实体
      if (isRootNeedDelete) {
        if (snapshotKey != null) {
          addDeletedEntityChange(tableMap, snapshotMap.get(snapshotKey), entityDoMapping);
          processedKeys.add(snapshotKey);
        }
        continue;
      }

      // 场景2：主实体未删除，子实体是新增
      if (snapshotKey == null) {
        addNewEntityChange(tableMap, currentEntity, entityDoMapping);
        hasAnyChange = true;
        continue;
      }

      // 场景3：主实体未删除，子实体有变更
      BaseDomainEntity snapshotEntity = snapshotMap.get(snapshotKey);
      if (snapshotEntity != null) {
        Set<String> changedFields = compareEntityFields(snapshotEntity, currentEntity);
        if (!changedFields.isEmpty()) {
          addModifiedEntityChange(tableMap, currentEntity, changedFields, entityDoMapping);
          hasAnyChange = true;
        }
        processedKeys.add(snapshotKey);
      }
    }

    // 4. 处理“快照有、当前无”的子实体（即被置null的子实体，仅删子实体，不碰主实体）
    for (Map.Entry<Object, BaseDomainEntity> entry : snapshotMap.entrySet()) {
      Object snapshotKey = entry.getKey();
      if (processedKeys.contains(snapshotKey)) {
        continue;
      }
      BaseDomainEntity snapshotEntity = entry.getValue();
      // 核心：跳过主实体（即使主实体被误判，也不删），仅删子实体
      if (snapshotEntity instanceof AggregateRoot) {
        log.warn("跳过主实体的删除逻辑，主实体ID：{}", getEntityId(snapshotEntity));
        continue;
      }
      // 子实体“快照有、当前无” → 删除子实体
      addDeletedEntityChange(tableMap, snapshotEntity, entityDoMapping);
      hasAnyChange = true;
    }

    // 5. 版本号自增（任意变更都触发）
    if (hasAnyChange) {
      aggregateRoot.incrVersion();
      result.setAggregateVersion(aggregateRoot.getVersion());
      result.setAggregateRootId((Long) getEntityId(aggregateRoot));
      log.info("聚合根版本号自增为：{}", aggregateRoot.getVersion());
    }

    result.setTableChangesMap(tableMap);
    return result;
  }

  // ========== 以下方法与之前一致，无需修改 ==========
  public <T extends AggregateRoot> AggregateChanges compareChanges(
      T aggregateRoot, Map<Class<?>, Class<?>> entityDoMapping) {
    return compareChanges(this.snapshotMap, aggregateRoot, entityDoMapping);
  }

  private List<BaseDomainEntity> collectAllEntities(BaseDomainEntity root) {
    List<BaseDomainEntity> entities = new ArrayList<>();
    Set<Object> collectedIds = new HashSet<>();
    collectAllEntitiesRecursive(root, entities, collectedIds);
    return entities;
  }

  private void collectAllEntitiesRecursive(
      BaseDomainEntity root, List<BaseDomainEntity> entities, Set<Object> collectedIds) {
    if (root == null) {
      return;
    }

    Object id = getEntityId(root);
    Object uniqueKey = id == null ? System.identityHashCode(root) : id;
    if (collectedIds.contains(uniqueKey)) {
      return;
    }
    collectedIds.add(uniqueKey);
    entities.add(root);

    Class<?> clazz = root.getClass();
    while (clazz != Object.class) {
      Field[] fields = clazz.getDeclaredFields();
      for (Field field : fields) {
        if (Modifier.isStatic(field.getModifiers())
            || Modifier.isTransient(field.getModifiers())
            || "serialVersionUID".equals(field.getName())) {
          continue;
        }

        field.setAccessible(true);
        try {
          Object fieldValue = field.get(root);
          if (fieldValue == null) {
            continue;
          }

          if (fieldValue instanceof List<?>) {
            ((List<?>) fieldValue)
                .forEach(
                    item -> {
                      if (item instanceof BaseDomainEntity) {
                        collectAllEntitiesRecursive(
                            (BaseDomainEntity) item, entities, collectedIds);
                      }
                    });
          } else if (fieldValue instanceof BaseDomainEntity) {
            collectAllEntitiesRecursive((BaseDomainEntity) fieldValue, entities, collectedIds);
          }

        } catch (IllegalAccessException e) {
          log.error("扫描实体字段失败：{}.{}", clazz.getSimpleName(), field.getName(), e);
          throw new RuntimeException("扫描聚合根子实体失败：" + root.getClass().getName(), e);
        }
      }
      clazz = clazz.getSuperclass();
    }
  }

  private Object getEntityId(BaseDomainEntity entity) {
    Field idField = ReflectionUtils.findField(entity.getClass(), ID_FIELD_NAME);
    if (idField == null) {
      throw new RuntimeException("实体[" + entity.getClass() + "]无" + ID_FIELD_NAME + "字段");
    }
    idField.setAccessible(true);
    try {
      return idField.get(entity);
    } catch (IllegalAccessException e) {
      throw new RuntimeException("获取实体ID失败：" + entity.getClass().getName(), e);
    }
  }

  private Set<String> compareEntityFields(BaseDomainEntity snapshot, BaseDomainEntity current) {
    Set<String> changedFields = new HashSet<>();
    Class<?> clazz = snapshot.getClass();

    while (clazz != Object.class) {
      Field[] fields = clazz.getDeclaredFields();
      for (Field field : fields) {
        if (Modifier.isStatic(field.getModifiers())
            || Modifier.isTransient(field.getModifiers())
            || "serialVersionUID".equals(field.getName())) {
          continue;
        }

        field.setAccessible(true);
        try {
          Object snapshotValue = field.get(snapshot);
          Object currentValue = field.get(current);

          if (snapshotValue instanceof BaseDomainEntity
              && currentValue instanceof BaseDomainEntity) {
            Set<String> nestedChanges =
                compareEntityFields(
                    (BaseDomainEntity) snapshotValue, (BaseDomainEntity) currentValue);
            if (!nestedChanges.isEmpty()) {
              changedFields.add(field.getName() + "_nested");
            }
          } else if (!Objects.equals(snapshotValue, currentValue)) {
            changedFields.add(field.getName());
            log.debug(
                "字段变更：{}.{}，旧值：{}，新值：{}",
                clazz.getSimpleName(),
                field.getName(),
                snapshotValue,
                currentValue);
          }
        } catch (IllegalAccessException e) {
          throw new RuntimeException("对比实体字段失败：" + clazz.getName() + "." + field.getName(), e);
        }
      }
      clazz = clazz.getSuperclass();
    }
    return changedFields;
  }

  private Object findTempKeyByEntity(
      Map<Object, BaseDomainEntity> snapshotMap, BaseDomainEntity entity) {
    for (Map.Entry<Object, BaseDomainEntity> entry : snapshotMap.entrySet()) {
      BaseDomainEntity snapshotEntity = entry.getValue();
      if (snapshotEntity.getClass().equals(entity.getClass())
          && getEntityId(snapshotEntity) == null
          && getEntityId(entity) == null) {
        Set<String> diffs = compareEntityFields(snapshotEntity, entity);
        if (diffs.isEmpty()) {
          return entry.getKey();
        }
      }
    }
    return null;
  }

  private BaseDomainEntity deepClone(BaseDomainEntity source) {
    try {
      BaseDomainEntity clone = (BaseDomainEntity) source.clone();

      Class<?> clazz = source.getClass();
      while (clazz != Object.class) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
          if (Modifier.isStatic(field.getModifiers())
              || Modifier.isTransient(field.getModifiers())
              || "serialVersionUID".equals(field.getName())) {
            continue;
          }

          field.setAccessible(true);
          Object sourceValue = field.get(source);
          if (sourceValue == null) {
            continue;
          }

          if (sourceValue instanceof BaseDomainEntity) {
            field.set(clone, deepClone((BaseDomainEntity) sourceValue));
          } else if (sourceValue instanceof List<?>) {
            List<Object> clonedList = new ArrayList<>();
            ((List<?>) sourceValue)
                .forEach(
                    item -> {
                      if (item instanceof BaseDomainEntity) {
                        clonedList.add(deepClone((BaseDomainEntity) item));
                      } else {
                        clonedList.add(item);
                      }
                    });
            field.set(clone, clonedList);
          }
        }
        clazz = clazz.getSuperclass();
      }
      return clone;
    } catch (CloneNotSupportedException | IllegalAccessException e) {
      throw new RuntimeException("深克隆实体失败：" + source.getClass().getName(), e);
    }
  }

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
    log.debug("新增实体：{}，ID：{}", entity.getClass().getSimpleName(), getEntityId(entity));
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
    log.info(
        "修改实体：{}，ID：{}，变更字段：{}",
        entity.getClass().getSimpleName(),
        getEntityId(entity),
        changedFields);
  }

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
    log.debug("标记删除实体：{}，ID：{}", entity.getClass().getSimpleName(), getEntityId(entity));
  }
}
