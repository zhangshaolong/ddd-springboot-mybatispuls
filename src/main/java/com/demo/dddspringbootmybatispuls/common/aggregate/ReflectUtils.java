package com.demo.dddspringbootmybatispuls.common.aggregate;

import java.lang.reflect.Field;

public class ReflectUtils {

  /**
   * 通用获取ID字段值（兼容父类、多字段名、私有字段）
   *
   * @param obj 目标对象
   * @return ID字段值（Long类型）
   */
  public static Long getIdValue(Object obj) {
    if (obj == null) {
      return null;
    }

    String[] idFieldNames = {"id", "Id", "ID", "entityId", "entity_id"};

    for (String fieldName : idFieldNames) {
      try {
        Field idField = findField(obj.getClass(), fieldName);
        if (idField != null) {
          idField.setAccessible(true);
          Object value = idField.get(obj);
          if (value instanceof Number) {
            return ((Number) value).longValue();
          } else if (value instanceof String) {
            return Long.valueOf((String) value);
          }
          return null;
        }
      } catch (IllegalAccessException e) {
        continue;
      }
    }

    throw new RuntimeException(
        "未找到ID字段！对象类型：" + obj.getClass().getName() + "，尝试的字段名：" + String.join(",", idFieldNames));
  }

  /**
   * 通用设置ID字段值（兼容父类、私有字段）
   *
   * @param obj 目标对象
   * @param id 要设置的ID值
   */
  public static void setIdValue(Object obj, Long id) {
    if (obj == null || id == null) {
      return;
    }

    String[] idFieldNames = {"id", "Id", "ID", "entityId", "entity_id"};
    for (String fieldName : idFieldNames) {
      try {
        Field idField = findField(obj.getClass(), fieldName);
        if (idField != null) {
          idField.setAccessible(true);
          Class<?> fieldType = idField.getType();
          if (fieldType == Long.class || fieldType == long.class) {
            idField.set(obj, id);
          } else if (fieldType == Integer.class || fieldType == int.class) {
            idField.set(obj, id.intValue());
          } else if (fieldType == String.class) {
            idField.set(obj, id.toString());
          }
          return;
        }
      } catch (IllegalAccessException e) {
        continue;
      }
    }
    throw new RuntimeException("未找到可设置的ID字段！对象类型：" + obj.getClass().getName());
  }

  private static Field findField(Class<?> clazz, String fieldName) {
    while (clazz != null && clazz != Object.class) {
      try {
        return clazz.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        clazz = clazz.getSuperclass();
      }
    }
    return null;
  }
}
