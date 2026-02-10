package com.demo.dddspringbootmybatispuls.test.mapper;

import com.demo.dddspringbootmybatispuls.common.mapper.MappingRule;
import com.demo.dddspringbootmybatispuls.common.mapper.StructMapper;

import java.util.List;

public class MultiLevelNestedTest {
    public static void main(String[] args) {

        // 构造多层嵌套的源对象
        AddressEntity addressEntity = new AddressEntity("广东省", "深圳市");
        List<OrderEntity> orderEntities = List.of(
                new OrderEntity(1001L, "订单1"),
                new OrderEntity(1002L, "订单2")
        );
        UserEntity source = new UserEntity(1L, "张三", addressEntity, orderEntities);

        // 定义综合规则：嵌套对象+集合字段
        List<MappingRule<UserEntity, UserDTO>> rules = List.of(
                MappingRule.of("userName", "name"),
                // 1. 转换嵌套地址
                MappingRule.custom(
                        "address", "address",
                        (fieldValue, instance) -> {
                            // 空值兜底：避免嵌套对象为null时转换报错
                            if (fieldValue == null) {
                                return null;
                            }
                            List<MappingRule<AddressEntity, AddressDTO>> addressRules = List.of(
                                    MappingRule.custom(
                                            "province", "fullAddress",
                                            (addrField, addrInstance) -> addrInstance.getProvince() + "-" + addrInstance.getCity()
                                    )
                            );
                            return StructMapper.to((AddressEntity) fieldValue, AddressDTO.class, addressRules);
                        }
                ),
                // 2. 转换订单集合
                MappingRule.custom(
                        "orders", "orders",
                        (fieldValue, instance) -> StructMapper.toList((List<OrderEntity>) fieldValue, OrderDTO.class)
                )
        );

        // 转换
        UserDTO target = StructMapper.to(source, UserDTO.class, rules);
        System.out.println("多层嵌套+集合转换结果：");
        System.out.println(target);
    }
}
