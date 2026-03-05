package com.demo.dddspringbootmybatispuls.test.mapper;

import com.demo.dddspringbootmybatispuls.common.mapper.StructMapper;
import io.github.linpeilie.annotations.AutoMapper;

public class PlusTest {

  public static void main(String[] args) {

    PlusEntity plusEntity = new PlusEntity();
    plusEntity.setMobile("aa");
    plusEntity.setUsername("name");
    plusEntity.setPassword("pwd");
    PlusDTO plus = StructMapper.to(plusEntity, PlusDTO.class);
    System.out.println(plus);

    AddressEntity addressEntity = new AddressEntity();
    addressEntity.setCity("city");
    addressEntity.setProvince("province");
    AutoMapper autoMapper = AddressEntity.class.getAnnotation(AutoMapper.class);
    System.out.println(autoMapper);
    AddressDTO addressDTO = StructMapper.to(addressEntity, AddressDTO.class);
    System.out.println(addressDTO);
  }
}
