package com.demo.dddspringbootmybatispuls.module.user.domain.repository;

import com.demo.dddspringbootmybatispuls.module.user.infrastructure.dataobject.UserDO;

public interface UserRepository {
  UserDO findById(Long id);
}
