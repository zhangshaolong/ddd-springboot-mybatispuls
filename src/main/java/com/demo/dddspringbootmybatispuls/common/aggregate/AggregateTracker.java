package com.demo.dddspringbootmybatispuls.common.aggregate;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Getter
@Setter
public class AggregateTracker {
  private AggregateRoot currentAggregateRoot;
  private Aggregate<? extends AggregateRoot> currentAggregate;

  // ========== 无参build（核心：标记为新建聚合） ==========
  @SuppressWarnings("unchecked")
  public <T extends AggregateRoot> Aggregate<T> build(Class<T> aggregateRootClass) {
    this.currentAggregateRoot = null;
    this.currentAggregate = Aggregate.of(); // of()标记isBuiltWithoutRoot=true
    this.currentAggregate.createSnapshot(); // 快照同步标记
    log.debug(
        "无参build()执行完成：初始化{}类型容器（强制新建），无参标记：{}",
        aggregateRootClass.getSimpleName(),
        this.currentAggregate.isBuiltWithoutRoot());
    return (Aggregate<T>) this.currentAggregate;
  }

  public Aggregate<? extends AggregateRoot> build() {
    return build(AggregateRoot.class);
  }

  // ========== 带参build（原有逻辑） ==========
  public <T extends AggregateRoot> Aggregate<T> build(T root) {
    this.currentAggregateRoot = root;
    this.currentAggregate = Aggregate.of(root); // of(root)标记isBuiltWithoutRoot=false
    ((Aggregate<T>) this.currentAggregate).createSnapshot();
    log.debug(
        "带参build()执行完成：初始化{}类型容器（非新建），无参标记：{}",
        root.getClass().getSimpleName(),
        this.currentAggregate.isBuiltWithoutRoot());
    return (Aggregate<T>) this.currentAggregate;
  }

  // ========== 核心重构：对比逻辑（基于初始化方式判定） ==========
  public AggregateChanges compareChanges() {
    AggregateChanges changes = new AggregateChanges();
    if (currentAggregate == null || !currentAggregate.hasSnapshot()) {
      log.warn("无聚合快照，无法对比变更");
      return changes;
    }

    currentAggregate.syncChildEntities();
    AggregateRoot root = currentAggregate.getRoot();
    if (root != null) {
      changes.setAggregateRootId(ReflectUtils.getIdValue(root));
      changes.setAggregateVersion(root.getVersion());
    }

    // 核心判定：仅根据isBuiltWithoutRoot判断是否新建（完全移除ID判断）
    if (currentAggregate.isBuiltWithoutRoot()) {
      // 无参build初始化 → 强制所有主实体为新增
      handleBuiltWithoutRootChanges(changes);
    } else {
      // 带参build初始化 → 原有修改逻辑
      handleBuiltWithRootChanges(changes);
    }

    return changes;
  }

  /** 无参build初始化 → 主实体强制新增，子实体按快照对比 */
  private void handleBuiltWithoutRootChanges(AggregateChanges changes) {
    log.debug("无参build初始化，强制主实体为新增（忽略ID）");

    // 1. 主实体强制新增（无论是否有ID）
    AggregateRoot root = currentAggregate.getRoot();
    if (root != null) {
      changes.addAggregateRootInsert(root);
      changes.addInsertEntity(root); // 加入INSERT列表
      log.debug("主实体（{}）强制标记为新增，ID：{}", root.getClass().getSimpleName(), root.getId());
    }

    // 2. 子实体按快照对比（新增/修改/删除）
    compareChildEntitiesChanges(changes);
  }

  /** 带参build初始化 → 原有修改逻辑 + 主实体同步到changeMap的updateList */
  private void handleBuiltWithRootChanges(AggregateChanges changes) {
    AggregateRoot root = currentAggregate.getRoot();
    if (root != null) {
      // 1. 原有逻辑：对比主实体是否修改
      compareRootChangesByReflection(changes);

      // 核心新增：无论是否修改，编辑状态的主实体都同步到changeMap（保证统一）
      // 场景1：主实体有修改 → 放入updateList
      if (changes.isAggregateRootUpdate()) {
        changes.addUpdateEntity(root);
        log.debug(
            "编辑主实体（{}）同步到changeMap的updateList，ID：{}",
            root.getClass().getSimpleName(),
            root.getId());
      }
      // 场景2：主实体无修改 → 也放入changeMap（可选，根据你的业务决定是否保留）
      // else {
      //     changes.addInsertEntity(root); // 无修改时也放入，保证key存在
      // }
    }

    // 2. 子实体对比逻辑（不变）
    compareChildEntitiesChanges(changes);
  }

  private void compareRootChangesByReflection(AggregateChanges changes) {
    AggregateRoot currentRoot = currentAggregate.getRoot();
    AggregateRoot snapshotRoot = currentAggregate.getSnapshot().getRoot();

    if (currentRoot == null || snapshotRoot == null) {
      return;
    }

    boolean isModified = isEntityFieldsChanged(currentRoot, snapshotRoot);
    if (isModified) {
      changes.addAggregateRootUpdate(currentRoot);
      log.debug("识别到主实体（{}）修改，ID：{}", currentRoot.getClass().getSimpleName(), currentRoot.getId());
    }
  }

  private void compareChildEntitiesChanges(AggregateChanges changes) {
    List<BaseDomainEntity> currentChildren = currentAggregate.getChildEntities();
    List<BaseDomainEntity> snapshotChildren = currentAggregate.getSnapshot().getChildEntities();

    Map<String, BaseDomainEntity> snapshotChildMap = new HashMap<>();
    for (BaseDomainEntity snapshotChild : snapshotChildren) {
      snapshotChildMap.put(buildEntityKey(snapshotChild), snapshotChild);
    }

    for (BaseDomainEntity currentChild : currentChildren) {
      String childKey = buildEntityKey(currentChild);
      if (snapshotChildMap.containsKey(childKey)) {
        BaseDomainEntity snapshotChild = snapshotChildMap.get(childKey);
        if (isEntityFieldsChanged(currentChild, snapshotChild)) {
          changes.addUpdateEntity(currentChild);
        }
        snapshotChildMap.remove(childKey);
      } else {
        changes.addInsertEntity(currentChild);
        log.debug("子实体（{}）新增，ID：{}", currentChild.getClass().getSimpleName(), currentChild.getId());
      }
    }

    for (BaseDomainEntity deletedChild : snapshotChildMap.values()) {
      changes.addDeleteEntity(deletedChild);
      log.debug("子实体（{}）删除，ID：{}", deletedChild.getClass().getSimpleName(), deletedChild.getId());
    }
  }

  private boolean isEntityFieldsChanged(BaseDomainEntity current, BaseDomainEntity snapshot) {
    if (current == null || snapshot == null) return false;
    Class<?> currentClass = current.getClass();
    while (currentClass != null && currentClass != Object.class) {
      Field[] fields = currentClass.getDeclaredFields();
      for (Field field : fields) {
        if (field.getName().equals("id")
            || field.getName().equals("version")
            || field.getName().equals("deleted")) {
          continue;
        }
        try {
          field.setAccessible(true);
          Object currentValue = field.get(current);
          Object snapshotValue = field.get(snapshot);
          if (currentValue == null && snapshotValue != null) return true;
          if (currentValue != null && !currentValue.equals(snapshotValue)) return true;
        } catch (IllegalAccessException e) {
          continue;
        }
      }
      currentClass = currentClass.getSuperclass();
    }
    return false;
  }

  private String buildEntityKey(BaseDomainEntity entity) {
    Long entityId = ReflectUtils.getIdValue(entity);
    return entity.getClass().getName() + "_" + (entityId == null ? "NEW" : entityId);
  }

  // 同步聚合容器到tracker
  public void setCurrentAggregate(Aggregate<? extends AggregateRoot> currentAggregate) {
    this.currentAggregate = currentAggregate;
    this.currentAggregateRoot = currentAggregate.getRoot();
  }
}
