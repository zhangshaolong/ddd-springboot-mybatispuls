package com.demo.dddspringbootmybatispuls.common.aggregate;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

@Component
public class DefaultDoCommonFieldHandler implements DoCommonFieldHandler {
  private static final String DEFAULT_OPERATOR = "system";

  @Override
  public void fillCommonFields(Object doObj, EntityChangeType changeType) {
    if (doObj == null) {
      return;
    }

    LocalDateTime now = LocalDateTime.now();
    if (EntityChangeType.NEW.equals(changeType)) {
      setFieldValue(doObj, "createTime", now);
      setFieldValue(doObj, "createBy", DEFAULT_OPERATOR);
    }
    // 填充更新信息（新增/修改）
    setFieldValue(doObj, "updateTime", now);
    setFieldValue(doObj, "updateBy", DEFAULT_OPERATOR);
  }

  private void setFieldValue(Object target, String fieldName, Object value) {
    Field field = ReflectionUtils.findField(target.getClass(), fieldName);
    if (field != null) {
      field.setAccessible(true);
      ReflectionUtils.setField(field, target, value);
    }
  }
}
