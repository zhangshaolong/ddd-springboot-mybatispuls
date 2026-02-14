package com.demo.dddspringbootmybatispuls.common.aggregate;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.dddspringbootmybatispuls.common.mapper.StructMapper;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/** 聚合持久化管理器（最终版：无业务硬编码、纯类型分析找Mapper） */
@Slf4j
@Component
public class AggregatePersistenceManager {

  @Resource private ApplicationContext applicationContext;

  @SuppressWarnings({"rawtypes"})
  public void persist(
      AggregateChanges changes, Map<Class<?>, Class<?>> entityDoMapping, boolean debug) {
    if (!changes.hasAnyChanges()) {
      log.info("无变更需要持久化");
      return;
    }

    log.info(
        "开始持久化聚合变更，聚合根ID：{}，版本：{}", changes.getAggregateRootId(), changes.getAggregateVersion());

    for (Map.Entry<Class<?>, AggregateChanges.EntityChanges<BaseDomainEntity>> entry :
        changes.getEntityChangesMap().entrySet()) {
      Class<?> entityClass = entry.getKey();
      AggregateChanges.EntityChanges<?> entityChanges = entry.getValue();

      // 1. 获取DO类型
      Class<?> doClass = entityDoMapping.get(entityClass);
      if (doClass == null) {
        throw new RuntimeException("未配置实体[" + entityClass.getName() + "]的DO映射关系");
      }

      // 2. 动态获取BaseMapper（纯类型分析）
      BaseMapper doMapper = GenericTypeUtils.getMapperByDoType(applicationContext, doClass);

      // 3. 处理增删改
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
  }

  public void persist(AggregateChanges changes, Map<Class<?>, Class<?>> entityDoMapping) {
    persist(changes, entityDoMapping, false);
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
        log.debug("新增{}转换为DO：{}", entityClass.getSimpleName(), doObj);
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
        log.debug("修改{}转换为DO：{}", entityClass.getSimpleName(), doObj);
      } else {
        doMapper.updateById(doObj);
      }
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
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
        log.debug("删除{}转换为DO：{}", entityClass.getSimpleName(), doObj);
      } else {
        doMapper.deleteById(ReflectUtils.getIdValue(doObj));
      }
    }
  }
}
