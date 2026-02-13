package com.demo.dddspringbootmybatispuls.common.aggregate;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.dddspringbootmybatispuls.common.mapper.StructMapper;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/** 聚合持久化管理器（最终版：无业务硬编码、纯类型分析找Mapper） */
@Slf4j
@Component
public class AggregatePersistenceManager {
  private boolean debug = false;

  @Autowired private ApplicationContext applicationContext;

  /** 通用持久化方法（核心：纯类型分析找Mapper，无任何约定/硬编码） */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public void persist(AggregateChanges changes, Map<Class<?>, Class<?>> entityDoMapping) {
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
      BaseMapper doMapper =
          (BaseMapper) GenericTypeUtils.getMapperByDoType(applicationContext, doClass);
      if (doMapper == null) {
        throw new RuntimeException("Spring容器中未找到DO[" + doClass.getName() + "]对应的BaseMapper");
      }

      // 3. 处理增删改
      if (!entityChanges.getInsertList().isEmpty()) {
        handleInsert(entityClass, doClass, doMapper, entityChanges.getInsertList());
      }
      if (!entityChanges.getUpdateList().isEmpty()) {
        handleUpdate(entityClass, doClass, doMapper, entityChanges.getUpdateList());
      }
      if (!entityChanges.getDeleteList().isEmpty()) {
        handleDelete(entityClass, doClass, doMapper, entityChanges.getDeleteList());
      }
    }

    log.info("聚合变更持久化完成");
  }

  // ========== 通用操作方法 ==========
  private void handleInsert(
      Class<?> entityClass, Class<?> doClass, BaseMapper doMapper, List<?> entityList) {
    log.info("处理新增{}，共 {} 个", entityClass.getSimpleName(), entityList.size());
    for (Object entity : entityList) {
      Object doObj = StructMapper.to(entity, doClass);
      if (debug) {
        log.debug("新增{}转换为DO：{}", entityClass.getSimpleName(), doObj);
      }
      doMapper.insert(doObj);
      // ID回写
      ReflectUtils.setIdValue(entity, ReflectUtils.getIdValue(doObj));
    }
  }

  private void handleUpdate(
      Class<?> entityClass, Class<?> doClass, BaseMapper doMapper, List<?> entityList) {
    log.info("处理修改{}，共 {} 个", entityClass.getSimpleName(), entityList.size());
    for (Object entity : entityList) {
      Object doObj = StructMapper.to(entity, doClass);
      if (debug) {
        log.debug("修改{}转换为DO：{}", entityClass.getSimpleName(), doObj);
      }
      doMapper.updateById(doObj);
    }
  }

  private void handleDelete(
      Class<?> entityClass, Class<?> doClass, BaseMapper doMapper, List<?> entityList) {
    log.info("处理删除{}，共 {} 个", entityClass.getSimpleName(), entityList.size());
    for (Object entity : entityList) {
      Object doObj = StructMapper.to(entity, doClass);
      if (debug) {
        log.debug("删除{}转换为DO：{}", entityClass.getSimpleName(), doObj);
      }
      doMapper.deleteById(ReflectUtils.getIdValue(doObj));
    }
  }

  // ========== 工具方法 ==========
  public void setDebug(boolean debug) {
    this.debug = debug;
  }
}
