package com.demo.dddspringbootmybatispuls.module.order.interfaces.controller.controller;

import com.demo.dddspringbootmybatispuls.common.response.Result;
import com.demo.dddspringbootmybatispuls.module.order.application.command.OrderCommandService;
import com.demo.dddspringbootmybatispuls.module.order.application.query.OrderQueryService;
import com.demo.dddspringbootmybatispuls.module.order.application.query.dto.OrderDTO;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/order")
public class OrderController {

  @Resource private OrderCommandService orderCommandService;
  @Resource private OrderQueryService orderQueryService;

  @GetMapping("/list")
  public Result<List<OrderDTO>> getAllUsers() {
    List<OrderDTO> orderDTOs = orderQueryService.getOrders();
    return Result.success(orderDTOs);
  }

  @GetMapping("/{id}")
  public Result<OrderDTO> getUserById(@PathVariable Long id) {
    OrderDTO orderDTO = orderQueryService.getOrderById(id);
    return Result.success(orderDTO);
  }

  @GetMapping("/update")
  public Result<?> updateOrder() {
    orderCommandService.update();
    return Result.success();
  }
}
