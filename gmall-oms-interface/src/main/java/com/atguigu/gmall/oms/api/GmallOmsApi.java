package com.atguigu.gmall.oms.api;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface GmallOmsApi {

    //@ApiOperation("创建订单")
    @PostMapping("oms/order/create/{userId}")
    public ResponseVo<OrderEntity> addOrder(@RequestBody OrderSubmitVo submitVo,
                                            @PathVariable("userId")Long userId);
}
