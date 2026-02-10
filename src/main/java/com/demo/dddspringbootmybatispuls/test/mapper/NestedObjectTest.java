package com.demo.dddspringbootmybatispuls.test.mapper;

import com.demo.dddspringbootmybatispuls.common.mapper.MappingRule;
import com.demo.dddspringbootmybatispuls.common.mapper.StructMapper;

import java.util.List;

public class NestedObjectTest {


    public static void main(String[] args) {
        // 构造源对象（包含嵌套AddressEntity）
        AddressEntity addressEntity = new AddressEntity("广东省", "深圳市");
        UserEntity source = new UserEntity(1L, "张三", addressEntity, null);

        // 定义转换规则：处理嵌套对象
        List<MappingRule<UserEntity, UserDTO>> rules = List.of(
                MappingRule.of("userName", "name"), // 基础映射
                // 自定义规则：转换嵌套对象（AddressEntity → AddressDTO）
                MappingRule.custom(
                        "address", "address",
                        (fieldValue, instance) -> {
                            // fieldValue = 源对象的address字段值（AddressEntity）
                            // 1. 定义嵌套对象的转换规则（比如拼接fullAddress）
                            List<MappingRule<AddressEntity, AddressDTO>> addressRules = List.of(
                                    MappingRule.custom(
                                            "province", "fullAddress",
                                            (addrFieldValue, addrInstance) -> addrInstance.getProvince() + "-" + addrInstance.getCity()
                                    )
                            );
                            // 2. 递归调用StructMapper.to()转换嵌套对象
                            return StructMapper.to((AddressEntity) fieldValue, AddressDTO.class, addressRules);
                        }
                )
        );

        // 转换
        UserDTO target = StructMapper.to(source, UserDTO.class, rules);
        System.out.println("嵌套对象转换结果：");
        System.out.println(target);
    }
}
