package com.demo.dddspringbootmybatispuls.module.user.convert;


import com.demo.dddspringbootmybatispuls.module.user.application.query.dto.UserDTO;
import com.demo.dddspringbootmybatispuls.module.user.infrastructure.dataobject.UserDO;
import org.mapstruct.factory.Mappers;

//@Mapper(componentModel = "spring")
public interface UserConvert {
    // 单例实例
    UserConvert INSTANCE = Mappers.getMapper(UserConvert.class);

    /**
     * DO转DTO：忽略密码，自定义状态描述
     */
//    @Mapping(target = "statusDesc", expression = "java(userDO.getStatus() == 1 ? \"正常\" : \"禁用\")")
//    @Mapping(target = "password", ignore = true)
    // 忽略密码字段
    UserDTO do2dto(UserDO userDO);
}
