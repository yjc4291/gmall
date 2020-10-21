package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.entity.SpuEntity;
import com.atguigu.gmall.pms.vo.SpuVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.pms.service.SkuService;

import javax.xml.crypto.Data;


@Service("skuService")
public class SkuServiceImpl extends ServiceImpl<SkuMapper, SkuEntity> implements SkuService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SkuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SkuEntity>()
        );

        return new PageResultVo(page);
    }

}