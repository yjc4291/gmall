package com.atguigu.gmall.oms.mapper;

import com.atguigu.gmall.oms.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 订单
 * 
 * @author Yan
 * @email 18112918867@163.com
 * @date 2020-08-21 18:23:59
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {

    public int updateStatus(@Param("orderToken") String orderToken, @Param("newStatus") Integer newStatus, @Param("oldStatus") Integer oldStatus);
	
}
