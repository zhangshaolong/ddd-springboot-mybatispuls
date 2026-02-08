package com.demo.dddspringbootmybatispuls.module.user.application.query;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.demo.dddspringbootmybatispuls.module.user.application.query.dto.UserDTO;
import com.demo.dddspringbootmybatispuls.module.user.infrastructure.dataobject.UserDO;
import com.demo.dddspringbootmybatispuls.module.user.infrastructure.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserQueryService {

    @Resource
    private UserMapper userMapper;

    public UserDTO getUserById(Long id) {
        UserDO userDO = userMapper.selectById(id);
        if (userDO == null) {
            return null;
        }
        return null;
//        return UserConvert.INSTANCE.do2dto(userDO);
    }

    public List<UserDTO> getUsers() {
        List<UserDO> userDOList = userMapper.selectList(new QueryWrapper<UserDO>().orderByDesc("id")).stream().toList();
        if (userDOList.isEmpty()) {
            return new ArrayList<>();
        }
        return null;
//        return userDOList.stream().map(UserConvert.INSTANCE::do2dto).toList();
    }
}
