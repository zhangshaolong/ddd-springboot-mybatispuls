package com.demo.dddspringbootmybatispuls.test.mapper;

import com.demo.dddspringbootmybatispuls.common.mapper.MappingRule;
import com.demo.dddspringbootmybatispuls.common.mapper.StructMapper;
import java.util.List;

public class CollectionFieldTest {
  public static void main(String[] args) {
    // 构造源对象（包含OrderEntity集合）
    List<OrderEntity> orderEntities =
        List.of(new OrderEntity(1001L, "订单1"), new OrderEntity(1002L, "订单2"));
    UserEntity source = new UserEntity(1L, "张三", null, orderEntities);

    // 定义转换规则：处理集合字段
    List<MappingRule<UserEntity, UserDTO>> rules =
        List.of(
            MappingRule.of("userName", "name"),
            // 自定义规则：转换集合字段（List<OrderEntity> → List<OrderDTO>）
            MappingRule.custom(
                "orders",
                "orders",
                (fieldValue, instance) -> {
                  // fieldValue = 源对象的orders字段值（List<OrderEntity>）
                  // 调用toList()转换集合，可传入集合元素的自定义规则
                  return StructMapper.toList((List<OrderEntity>) fieldValue, OrderDTO.class);
                }));

    // 转换
    UserDTO target = StructMapper.to(source, UserDTO.class, rules);
    System.out.println("集合字段转换结果：");
    System.out.println(target);
  }
}
