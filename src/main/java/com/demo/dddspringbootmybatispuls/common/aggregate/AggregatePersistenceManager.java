package com.demo.dddspringbootmybatispuls.common.aggregate;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.dddspringbootmybatispuls.common.mapper.StructMapper;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AggregatePersistenceManager {

  @Resource private ApplicationContext applicationContext;

  @SuppressWarnings({"rawtypes"})
  public boolean persist(
      AggregateTracker aggregateTracker, Map<Class<?>, Class<?>> entityDoMapping, boolean debug) {
    AggregateChanges changes = aggregateTracker.compareChanges();
    if (!changes.hasAnyChanges()) {
      log.info("无变更需要持久化");
      return false;
    }

    AggregateRoot aggregateRoot = aggregateTracker.getCurrentAggregateRoot();
    if (aggregateRoot == null) {
      throw new RuntimeException("聚合根实体不能为空，无法更新版本号");
    }

    aggregateRoot.autoIncrementVersion();
    log.info("聚合根版本号已自增，当前版本：{}", aggregateRoot.getVersion());

    ensureAggregateRootInUpdateList(changes, aggregateRoot);

    for (Map.Entry<Class<?>, AggregateChanges.EntityChanges<BaseDomainEntity>> entry :
        changes.getEntityChangesMap().entrySet()) {
      Class<?> entityClass = entry.getKey();
      AggregateChanges.EntityChanges<?> entityChanges = entry.getValue();

      Class<?> doClass = entityDoMapping.get(entityClass);
      if (doClass == null) {
        throw new RuntimeException("未配置实体[" + entityClass.getName() + "]的DO映射关系");
      }

      BaseMapper doMapper = GenericTypeUtils.getMapperByDoType(applicationContext, doClass);

      if (!entityChanges.getInsertList().isEmpty()) {
        handleInsert(entityClass, doClass, doMapper, entityChanges.getInsertList(), debug);
      }
      if (!entityChanges.getUpdateList().isEmpty()) {
        handleUpdate(entityClass, doClass, doMapper, entityChanges.getUpdateList(), debug);
      }
      if (!entityChanges.getDeleteList().isEmpty()) {
        handleDelete(entityClass, doClass, doMapper, entityChanges.getDeleteList(), debug);
      }
    }

    log.info("聚合变更持久化完成");
    return true;
  }

  public boolean persist(
      AggregateTracker aggregateTracker, Map<Class<?>, Class<?>> entityDoMapping) {
    return persist(aggregateTracker, entityDoMapping, false);
  }

  /**
   * 确保聚合根被加入更新列表（避免version变更未被持久化）
   *
   * @param changes 聚合变更对象
   * @param aggregateRoot 聚合根实体
   */
  private void ensureAggregateRootInUpdateList(AggregateChanges changes, Object aggregateRoot) {
    Class<?> aggregateRootClass = aggregateRoot.getClass();
    // 获取聚合根对应的EntityChanges
    AggregateChanges.EntityChanges<BaseDomainEntity> entityChanges =
        changes.getEntityChangesMap().get(aggregateRootClass);

    if (entityChanges == null) {
      // 如果聚合根本身无变更记录，创建新的EntityChanges并加入
      entityChanges = new AggregateChanges.EntityChanges<>();
      changes.getEntityChangesMap().put(aggregateRootClass, entityChanges);
    }

    List<BaseDomainEntity> updateList = entityChanges.getUpdateList();
    // 如果聚合根不在更新列表中，添加进去
    if (!updateList.contains(aggregateRoot)) {
      updateList.add((BaseDomainEntity) aggregateRoot);
      log.debug("聚合根已加入更新列表，确保version变更被持久化");
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private void handleInsert(
      Class<?> entityClass,
      Class<?> doClass,
      BaseMapper doMapper,
      List<?> entityList,
      boolean debug) {
    log.info("处理新增{}，共 {} 个", entityClass.getSimpleName(), entityList.size());
    for (Object entity : entityList) {
      Object doObj = StructMapper.to(entity, doClass);
      if (debug) {
        log.info(
            "新增{}转换为DO：{}, id:{}",
            entityClass.getSimpleName(),
            doObj,
            ReflectUtils.getIdValue(doObj));
      } else {
        doMapper.insert(doObj);
        // ID回写
        ReflectUtils.setIdValue(entity, ReflectUtils.getIdValue(doObj));
      }
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private void handleUpdate(
      Class<?> entityClass,
      Class<?> doClass,
      BaseMapper doMapper,
      List<?> entityList,
      boolean debug) {
    log.info("处理修改{}，共 {} 个", entityClass.getSimpleName(), entityList.size());
    for (Object entity : entityList) {
      Object doObj = StructMapper.to(entity, doClass);
      if (debug) {
        log.info(
            "修改{}转换为DO：{}, id:{}",
            entityClass.getSimpleName(),
            doObj,
            ReflectUtils.getIdValue(doObj));
      } else {
        doMapper.updateById(doObj);
      }
    }
  }

  private void handleDelete(
      Class<?> entityClass,
      Class<?> doClass,
      BaseMapper<?> doMapper,
      List<?> entityList,
      boolean debug) {
    log.info("处理删除{}，共 {} 个", entityClass.getSimpleName(), entityList.size());
    for (Object entity : entityList) {
      Object doObj = StructMapper.to(entity, doClass);
      if (debug) {
        log.info("删除{}转换为DO：{}", entityClass.getSimpleName(), doObj);
      } else {
        doMapper.deleteById(ReflectUtils.getIdValue(doObj));
      }
    }
  }
}
