package com.demo.dddspringbootmybatispuls.common.aggregate;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class Aggregate<T extends AggregateRoot> {
  private T root;
  private List<BaseDomainEntity> childEntities = new ArrayList<>();
  private Aggregate<T> snapshot;

  private boolean isBuiltWithoutRoot = false;

  private ConcurrentHashMap<String, BaseDomainEntity> deletedChildEntities =
      new ConcurrentHashMap<>();

  public static <T extends AggregateRoot> Aggregate<T> of() {
    Aggregate<T> aggregate = new Aggregate<>();
    aggregate.setBuiltWithoutRoot(true);
    return aggregate;
  }

  public static <T extends AggregateRoot> Aggregate<T> of(T root) {
    Aggregate<T> aggregate = new Aggregate<>(root);
    aggregate.setBuiltWithoutRoot(false);
    return aggregate;
  }

  private Aggregate() {
    this.isBuiltWithoutRoot = true;
  }

  private Aggregate(T root) {
    this.root = root;
    this.isBuiltWithoutRoot = false;
    collectChildEntitiesByReflection();
  }

  public void setRoot(T root) {
    this.root = root;
    this.childEntities.clear();
    collectChildEntitiesByReflection();
    log.debug(
        "设置聚合根完成，类型：{}，ID：{}，是否无参初始化：{}",
        root.getClass().getSimpleName(),
        root.getId(),
        this.isBuiltWithoutRoot);
  }

  @SuppressWarnings("unchecked")
  public void createSnapshot() {
    if (root == null) {
      Aggregate<T> snapshotAggregate = new Aggregate<>();
      snapshotAggregate.setBuiltWithoutRoot(this.isBuiltWithoutRoot); // 同步标记
      this.snapshot = snapshotAggregate;
      log.debug("为空聚合容器创建空快照，无参初始化标记：{}", this.isBuiltWithoutRoot);
      return;
    }
    try {
      T rootSnapshot = (T) this.root.clone();
      List<BaseDomainEntity> childSnapshot = new ArrayList<>();
      for (BaseDomainEntity child : this.childEntities) {
        childSnapshot.add((BaseDomainEntity) child.clone());
      }
      Aggregate<T> snapshotAggregate = new Aggregate<>();
      snapshotAggregate.setRoot(rootSnapshot);
      snapshotAggregate.setChildEntities(childSnapshot);
      snapshotAggregate.setBuiltWithoutRoot(this.isBuiltWithoutRoot); // 同步标记
      this.snapshot = snapshotAggregate;
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException("聚合快照创建失败", e);
    }
  }

  public void syncChildEntities() {
    if (root == null) {
      log.warn("聚合根为空，无法同步子实体");
      return;
    }
    this.childEntities.clear();
    collectChildEntitiesByReflection();
    log.debug("同步聚合根子实体完成，当前子实体数量：{}", this.childEntities.size());
  }

  private void collectChildEntitiesByReflection() {
    if (root == null) {
      return;
    }
    Class<?> rootClass = root.getClass();
    while (rootClass != null && rootClass != Object.class) {
      Field[] fields = rootClass.getDeclaredFields();
      for (Field field : fields) {
        try {
          field.setAccessible(true);
          Object fieldValue = field.get(root);
          if (fieldValue == null) continue;
          if (fieldValue instanceof BaseDomainEntity) {
            addChildEntity((BaseDomainEntity) fieldValue);
          } else if (fieldValue instanceof List<?>) {
            for (Object item : (List<?>) fieldValue) {
              if (item instanceof BaseDomainEntity) {
                addChildEntity((BaseDomainEntity) item);
              }
            }
          }
        } catch (IllegalAccessException e) {
          log.warn("反射读取字段失败：{}", field.getName());
        }
      }
      rootClass = rootClass.getSuperclass();
    }
  }

  public void addChildEntity(BaseDomainEntity childEntity) {
    if (childEntity == null) return;
    String entityKey = buildEntityKey(childEntity);
    if (childEntities.stream().noneMatch(e -> buildEntityKey(e).equals(entityKey))) {
      childEntities.add(childEntity);
    }
  }

  private String buildEntityKey(BaseDomainEntity entity) {
    Long entityId = ReflectUtils.getIdValue(entity);
    return entity.getClass().getName() + "_" + (entityId == null ? "NEW" : entityId);
  }

  public boolean hasSnapshot() {
    return this.snapshot != null;
  }
}
