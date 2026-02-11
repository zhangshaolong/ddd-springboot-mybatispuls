package com.demo.dddspringbootmybatispuls.module.user.infrastructure.repository;

import com.demo.dddspringbootmybatispuls.module.user.domain.repository.UserRepository;
import com.demo.dddspringbootmybatispuls.module.user.infrastructure.dataobject.UserDO;
import com.demo.dddspringbootmybatispuls.module.user.infrastructure.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepositoryImpl implements UserRepository {

  @Resource private UserMapper userMapper;

  @Override
  public UserDO findById(Long id) {
    return userMapper.selectById(id);
  }
}
