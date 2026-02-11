package com.demo.dddspringbootmybatispuls.test.mapper;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {
  private Long id;
  private String userName;
  private AddressEntity address; // 嵌套对象
  private List<OrderEntity> orders; // 集合字段
}
