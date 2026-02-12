package com.demo.dddspringbootmybatispuls.common.aggregate;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class AggregateChanges implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** 各DO表的变更列表（key=DO类型，value=该DO的增删改列表） */
  private Map<Class<?>, TableChanges<?>> tableChangesMap;

  /** 聚合根最新版本号 */
  private Long aggregateVersion;

  /** 单表的增删改列表 */
  @Data
  public static class TableChanges<DO> implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private List<DO> insertList;
    private List<DO> updateList;
    private List<DO> deleteList;
  }
}
