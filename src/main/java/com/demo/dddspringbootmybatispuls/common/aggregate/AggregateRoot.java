package com.demo.dddspringbootmybatispuls.common.aggregate;

/** 聚合根基类（带版本号） */
public abstract class AggregateRoot extends BaseDomainEntity {
  private static final long serialVersionUID = 1L;

  /** 乐观锁版本号 */
  private Long version;

  /** 版本号自增 */
  public void incrVersion() {
    this.version = this.version == null ? 1 : this.version + 1;
  }

  // Getter & Setter
  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }
}
