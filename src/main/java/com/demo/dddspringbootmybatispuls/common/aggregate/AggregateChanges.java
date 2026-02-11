package com.demo.dddspringbootmybatispuls.common.aggregate;

import java.io.Serializable;
import java.util.*;

public class AggregateChanges implements Serializable {
  private static final long serialVersionUID = 1L;

  /** 各DO表的变更列表（key=DO类型，value=该DO的增删改列表） */
  private Map<Class<?>, TableChanges<?>> tableChangesMap;

  /** 聚合根最新版本号 */
  private Long aggregateVersion;

  /** 单表的增删改列表 */
  public static class TableChanges<DO> implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<DO> insertList;
    private List<DO> updateList;
    private List<DO> deleteList;

    // Getter & Setter
    public List<DO> getInsertList() {
      return insertList;
    }

    public void setInsertList(List<DO> insertList) {
      this.insertList = insertList;
    }

    public List<DO> getUpdateList() {
      return updateList;
    }

    public void setUpdateList(List<DO> updateList) {
      this.updateList = updateList;
    }

    public List<DO> getDeleteList() {
      return deleteList;
    }

    public void setDeleteList(List<DO> deleteList) {
      this.deleteList = deleteList;
    }
  }

  // Getter & Setter
  public Map<Class<?>, TableChanges<?>> getTableChangesMap() {
    return tableChangesMap;
  }

  public void setTableChangesMap(Map<Class<?>, TableChanges<?>> tableChangesMap) {
    this.tableChangesMap = tableChangesMap;
  }

  public Long getAggregateVersion() {
    return aggregateVersion;
  }

  public void setAggregateVersion(Long aggregateVersion) {
    this.aggregateVersion = aggregateVersion;
  }
}
