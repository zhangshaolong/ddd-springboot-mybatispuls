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

  /** 对外暴露的深克隆方法（兼容原有逻辑） */
  public BaseDomainEntity deepClone() {
    try {
      return (BaseDomainEntity) this.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException("实体深拷贝失败", e);
    }
  }
}
