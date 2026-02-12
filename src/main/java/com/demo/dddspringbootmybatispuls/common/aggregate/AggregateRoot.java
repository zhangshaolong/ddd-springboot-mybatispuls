package com.demo.dddspringbootmybatispuls.common.aggregate;

import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 聚合根基类（带版本号+删除标记） */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class AggregateRoot extends BaseDomainEntity {
  @Serial private static final long serialVersionUID = 1L;

  /** 乐观锁版本号 */
  private Long version = 1L;

  /** 删除标记：0-未删除，1-已删除 */
  private Integer deleted = 0;

  /** 版本号自增 */
  public void incrVersion() {
    this.version = this.version + 1;
  }

  /** 标记为删除 */
  public void markAsDeleted() {
    this.deleted = 1;
  }

  /** 判断是否被标记删除 */
  public boolean isDeleted() {
    return this.deleted == 1;
  }

  /** 判断删除状态是否变更（从0→1） */
  public boolean isDeletedStatusChanged(BaseDomainEntity snapshot) {
    if (!(snapshot instanceof AggregateRoot)) {
      return false;
    }
    AggregateRoot snapshotRoot = (AggregateRoot) snapshot;
    // 快照未删除，当前已删除 → 状态变更
    return snapshotRoot.getDeleted() == 0 && this.isDeleted();
  }
}
