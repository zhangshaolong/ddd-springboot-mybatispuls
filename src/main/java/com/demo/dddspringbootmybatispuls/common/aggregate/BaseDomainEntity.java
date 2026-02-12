package com.demo.dddspringbootmybatispuls.common.aggregate;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/** 基础领域实体（所有领域实体的父类） */
@Data
public abstract class BaseDomainEntity implements Cloneable, Serializable {
  @Serial private static final long serialVersionUID = 1L;

  private Long id;

  /** 深拷贝（用于生成快照） */
  @Override
  public BaseDomainEntity clone() {
    try {
      return (BaseDomainEntity) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException("实体深拷贝失败", e);
    }
  }
}
