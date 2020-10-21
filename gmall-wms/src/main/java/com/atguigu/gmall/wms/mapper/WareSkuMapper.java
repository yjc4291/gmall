package com.atguigu.gmall.wms.mapper;

import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品库存
 * 
 * @author Yan
 * @email fengge@atguigu.com
 * @date 2020-08-21 19:30:44
 */
@Mapper
public interface WareSkuMapper extends BaseMapper<WareSkuEntity> {

    List<WareSkuEntity> checkLock(@Param("skuId") Long skuId, @Param("count") Integer count);

    int lock(@Param("id")Long id, @Param("count") Integer count);

    void unLock(@Param("id")Long id, @Param("count")Integer count);
}
