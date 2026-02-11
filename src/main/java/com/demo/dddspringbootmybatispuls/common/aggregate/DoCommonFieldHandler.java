package com.demo.dddspringbootmybatispuls.common.aggregate;

public interface DoCommonFieldHandler {
  void fillCommonFields(Object doObj, EntityChangeType changeType);
}
