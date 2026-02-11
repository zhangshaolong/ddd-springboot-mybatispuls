package com.demo.dddspringbootmybatispuls.module.user.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.dddspringbootmybatispuls.module.user.domain.repository.UserRepository;
import com.demo.dddspringbootmybatispuls.module.user.infrastructure.dataobject.UserDO;
import com.demo.dddspringbootmybatispuls.module.user.infrastructure.mapper.UserMapper;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepositoryImpl implements UserRepository {

  @Resource private UserMapper userMapper;

  @Override
  public UserDO selectById(Long id) {
    return userMapper.selectById(id);
  }

  @Override
  public List<UserDO> selectList() {
    return userMapper.selectList(new LambdaQueryWrapper<UserDO>());
  }
}
