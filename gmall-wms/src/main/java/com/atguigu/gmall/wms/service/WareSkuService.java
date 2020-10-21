package com.atguigu.gmall.wms.service;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 商品库存
 *
 * @author Yan
 * @email fengge@atguigu.com
 * @date 2020-08-21 19:30:44
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    List<SkuLockVo> checkAndLockStock(List<SkuLockVo> lockVos, String orderToken);
}

