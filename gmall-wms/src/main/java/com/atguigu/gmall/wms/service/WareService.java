package com.atguigu.gmall.wms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.wms.entity.WareEntity;

import java.util.Map;

/**
 * 仓库信息
 *
 * @author Yan
 * @email fengge@atguigu.com
 * @date 2020-08-21 19:30:45
 */
public interface WareService extends IService<WareEntity> {

    PageResultVo queryPage(PageParamVo paramVo);
}

