package com.demo.dddspringbootmybatispuls.common.aggregate;

import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 聚合根基类（带版本号） */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class AggregateRoot extends BaseDomainEntity {
  @Serial private static final long serialVersionUID = 1L;

  /** 乐观锁版本号 */
  private Long version = 1L;

  /** 标记主实体被删除 */
  private Integer deleted = 0;

  /** 版本号自增 */
  public void incrVersion() {
    this.version++;
  }

  public void markDeleted() {
    this.deleted = 1;
  }
}
