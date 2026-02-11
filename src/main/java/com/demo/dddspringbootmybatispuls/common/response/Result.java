package com.demo.dddspringbootmybatispuls.common.response;

import lombok.Data;

@Data
public class Result<T> {
  private int code;
  private String msg;
  private T data;

  public static <T> Result<T> success(T data) {
    Result<T> result = new Result<>();
    result.setCode(200);
    result.setMsg("成功");
    result.setData(data);
    return result;
  }

  public static <T> Result<T> success() {
    Result<T> result = new Result<>();
    result.setCode(200);
    result.setMsg("成功");
    return result;
  }

  public static <T> Result<T> fail(String msg) {
    Result<T> result = new Result<>();
    result.setCode(500);
    result.setMsg(msg);
    return result;
  }
}
