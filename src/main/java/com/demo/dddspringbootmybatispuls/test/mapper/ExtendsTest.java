package com.demo.dddspringbootmybatispuls.test.mapper;

import com.demo.dddspringbootmybatispuls.common.mapper.MappingRule;
import com.demo.dddspringbootmybatispuls.common.mapper.StructMapper;

import java.util.List;

public class ExtendsTest {

    public static void main(String[] args) {
        AddressEntity addressEntity = new AddressEntity("广东省", "深圳市");
        List<OrderEntity> orderEntities = List.of(
                new OrderEntity(1001L, "订单1"),
                new OrderEntity(1002L, "订单2")
        );
        StudentEntity student = new StudentEntity();
        student.setId(2L);
        student.setUserName("李四");
        student.setStudentNo("2024001");
        student.setAddress(addressEntity);
        student.setOrders(orderEntities);

        List<MappingRule<StudentEntity, StudentDTO>> rules = List.of(
                MappingRule.of("userName", "name"),
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
        StudentDTO studentDTO = StructMapper.to(student, StudentDTO.class, rules);
        System.out.println("Class继承转换结果：");
        System.out.println(studentDTO);
    }
}
