package com.demo.dddspringbootmybatispuls.common.aggregate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public class AggregateChanges {
  private Long aggregateRootId;
  private Long aggregateVersion;

  /** 聚合根变更实体 */
  private AggregateRoot aggregateRootChange;

  /** 聚合根变更类型：INSERT（新增）/ UPDATE（修改） */
  private String aggregateRootChangeType;

  private Map<Class<?>, EntityChanges<BaseDomainEntity>> entityChangesMap = new HashMap<>();

  // ========== 核心新增：聚合根新增方法 ==========
  public void addAggregateRootInsert(AggregateRoot root) {
    this.aggregateRootChange = root;
    this.aggregateRootChangeType = "INSERT";
    log.debug("新增聚合根新增记录：{}，ID：{}", root.getClass().getSimpleName(), root.getId());
  }

  public void addAggregateRootUpdate(AggregateRoot root) {
    this.aggregateRootChange = root;
    this.aggregateRootChangeType = "UPDATE";
    log.debug("新增聚合根修改记录：{}，ID：{}", root.getClass().getSimpleName(), root.getId());
  }

  /** 判断聚合根是否为新增 */
  public boolean isAggregateRootInsert() {
    return this.aggregateRootChange != null && "INSERT".equals(this.aggregateRootChangeType);
  }

  /** 判断聚合根是否为修改 */
  public boolean isAggregateRootUpdate() {
    return this.aggregateRootChange != null && "UPDATE".equals(this.aggregateRootChangeType);
  }

  /** 判断是否有任何变更（包含新增/修改/删除） */
  public boolean hasAnyChanges() {
    // 聚合根有新增/修改
    if (isAggregateRootInsert() || isAggregateRootUpdate()) {
      return true;
    }
    // 子实体有变更
    for (EntityChanges<BaseDomainEntity> entityChanges : entityChangesMap.values()) {
      if (!entityChanges.isEmpty()) {
        return true;
      }
    }
    return false;
  }

  // ========== 子实体变更方法（不变） ==========
  public void addInsertEntity(BaseDomainEntity entity) {
    if (entity == null) {
      log.warn("尝试添加空的子实体，忽略");
      return;
    }
    // 核心修复：用entity的实际类型作为Key，而非泛型推断
    Class<?> actualClass = entity.getClass();
    EntityChanges entityChanges = getOrCreateEntityChanges(actualClass);
    entityChanges.getInsertList().add(entity);
    log.debug(
        "新增子实体变更记录：{}，ID：{}，Key类型：{}",
        actualClass.getSimpleName(),
        entity.getId(),
        actualClass.getName());
  }

  public void addUpdateEntity(BaseDomainEntity entity) {
    if (entity == null) {
      log.warn("尝试添加空的子实体修改记录，忽略");
      return;
    }
    EntityChanges<BaseDomainEntity> entityChanges = getOrCreateEntityChanges(entity.getClass());
    entityChanges.getUpdateList().add(entity);
    log.debug("新增子实体修改记录：{}，ID：{}", entity.getClass().getSimpleName(), entity.getId());
  }

  public void addDeleteEntity(BaseDomainEntity entity) {
    if (entity == null) {
      log.warn("尝试添加空的子实体删除记录，忽略");
      return;
    }
    EntityChanges<BaseDomainEntity> entityChanges = getOrCreateEntityChanges(entity.getClass());
    entityChanges.getDeleteList().add(entity);
    log.debug("新增子实体删除记录：{}，ID：{}", entity.getClass().getSimpleName(), entity.getId());
  }

  @SuppressWarnings("unchecked")
  public <T extends BaseDomainEntity> EntityChanges<T> getEntityChanges(Class<T> entityClass) {
    EntityChanges<BaseDomainEntity> entityChanges = entityChangesMap.get(entityClass);
    if (entityChanges == null) {
      return new EntityChanges<>();
    }
    return (EntityChanges<T>) entityChanges;
  }

  private EntityChanges<BaseDomainEntity> getOrCreateEntityChanges(Class<?> entityClass) {
    EntityChanges<BaseDomainEntity> entityChanges = entityChangesMap.get(entityClass);
    if (entityChanges == null) {
      entityChanges = new EntityChanges<BaseDomainEntity>();
      entityChangesMap.put(entityClass, entityChanges);
    }
    return entityChanges;
  }

  // ========== 内部类（不变） ==========
  @Getter
  @Setter
  public static class EntityChanges<T extends BaseDomainEntity> {
    private List<BaseDomainEntity> insertList = new CopyOnWriteArrayList<>();
    private List<BaseDomainEntity> updateList = new CopyOnWriteArrayList<>();
    private List<BaseDomainEntity> deleteList = new CopyOnWriteArrayList<>();

    public boolean isEmpty() {
      return insertList.isEmpty() && updateList.isEmpty() && deleteList.isEmpty();
    }

    @SuppressWarnings("unchecked")
    public <T extends BaseDomainEntity> List<T> getInsertList(Class<T> clazz) {
      return (List<T>) insertList.stream().filter(clazz::isInstance).toList();
    }

    @SuppressWarnings("unchecked")
    public <T extends BaseDomainEntity> List<T> getUpdateList(Class<T> clazz) {
      return (List<T>) updateList.stream().filter(clazz::isInstance).toList();
    }

    @SuppressWarnings("unchecked")
    public <T extends BaseDomainEntity> List<T> getDeleteList(Class<T> clazz) {
      return (List<T>) deleteList.stream().filter(clazz::isInstance).toList();
    }
  }
}
