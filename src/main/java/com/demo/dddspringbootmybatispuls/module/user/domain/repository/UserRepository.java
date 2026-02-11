package com.demo.dddspringbootmybatispuls.module.user.domain.repository;

import com.demo.dddspringbootmybatispuls.module.user.infrastructure.dataobject.UserDO;
import java.util.List;

public interface UserRepository {
  UserDO selectById(Long id);

  List<UserDO> selectList();
}
