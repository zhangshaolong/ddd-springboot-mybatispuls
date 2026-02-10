package com.demo.dddspringbootmybatispuls.test.mapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentDTO extends UserDTO {
    private String studentNo;

    @Override
    public String toString() {
        return "StudentDTO{" +
                "id=" + getId() +
                ", name='" + getName() + '\'' +
                ", address=" + getAddress() +
                ", orders=" + getOrders() +
                ", studentNo=" + getStudentNo() +
                '}';
    }
}


