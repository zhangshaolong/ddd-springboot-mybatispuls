package com.demo.dddspringbootmybatispuls.test.mapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddressDTO {
    private String province;
    private String city;
    private String fullAddress; // 新增：省+市拼接

    @Override
    public String toString() {
        return "AddressDTO{" +
                "province='" + province + '\'' +
                ", city='" + city + '\'' +
                ", fullAddress='" + fullAddress + '\'' +
                '}';
    }
}
