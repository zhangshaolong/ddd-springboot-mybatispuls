package com.demo.dddspringbootmybatispuls.common.aggregate;

import java.io.Serial;
import java.util.Objects;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public abstract class AggregateRoot extends BaseDomainEntity {
  @Serial private static final long serialVersionUID = 1L;

  private Long version = 1L;

  private Integer deleted = 0;

  public void markAsDeleted() {
    this.deleted = 1;
  }

  public boolean isDeleted() {
    return this.deleted == 1;
  }

  public boolean isValidVersion(Long version) {
    return Objects.equals(version, this.version);
  }

  public void autoIncrementVersion() {
    this.version++;
  }
}
