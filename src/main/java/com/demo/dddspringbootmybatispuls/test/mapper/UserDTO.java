package com.demo.dddspringbootmybatispuls.test.mapper;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
  private Long id;
  private String name;
  private AddressDTO address; // 嵌套DTO
  private List<OrderDTO> orders; // 集合DTO

  @Override
  public String toString() {
    return "UserDTO{"
        + "id="
        + id
        + ", name='"
        + name
        + '\''
        + ", address="
        + address
        + ", orders="
        + orders
        + '}';
  }
}
