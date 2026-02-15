package com.demo.dddspringbootmybatispuls.common.aggregate;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/** 基础领域实体（所有领域实体的父类） */
@Data
public abstract class BaseDomainEntity implements Cloneable, Serializable {
  @Serial private static final long serialVersionUID = 1L;

  private Long id;

  @Override
  protected Object clone() throws CloneNotSupportedException {
    return super.clone();
  }
}
