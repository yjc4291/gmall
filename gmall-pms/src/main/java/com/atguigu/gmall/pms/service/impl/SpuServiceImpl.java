package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.feign.GmallSmsClient;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.mapper.SpuDescMapper;
import com.atguigu.gmall.pms.mapper.SpuMapper;
import com.atguigu.gmall.pms.service.SkuAttrValueService;
import com.atguigu.gmall.pms.service.SkuImagesService;
import com.atguigu.gmall.pms.service.SpuAttrValueService;
import com.atguigu.gmall.pms.service.SpuService;
import com.atguigu.gmall.pms.vo.SkuVo;
import com.atguigu.gmall.pms.vo.SpuAttrValueVo;
import com.atguigu.gmall.pms.vo.SpuVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.seata.spring.annotation.GlobalTransactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


@Service("spuService")
public class SpuServiceImpl extends ServiceImpl<SpuMapper, SpuEntity> implements SpuService {

    @Autowired
    private SpuDescMapper spuDescMapper;

    @Autowired
    private SpuAttrValueService spuAttrValueService;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private SkuImagesService skuImagesService;

    @Autowired
    private SkuAttrValueService attrValueService;

    @Autowired
    private GmallSmsClient gmallSmsClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SpuEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public PageResultVo querySpu(PageParamVo pageParamVo, Long categoryId) {

        // 封装查询条件
        QueryWrapper<SpuEntity> wrapper = new QueryWrapper<>();
        // 如果分类id不为0，要根据分类id查，否则查全部
        if (categoryId != 0){
            wrapper.eq("category_id", categoryId);
        }
        // 如果用户输入了检索条件，根据检索条件查
        String key = pageParamVo.getKey();
        if (StringUtils.isNotBlank(key)){
            wrapper.and(t -> t.like("name", key).or().like("id", key));
        }

        return new PageResultVo(this.page(pageParamVo.getPage(), wrapper));
    }

    @Override
    @GlobalTransactional
    public void bigSave(SpuVo spu) {
        // 1、保存spu相关信息：pms_spu pms_spu_desc pms_spu_attr_value
        // 1.1、保存spu基本信息 pms_spu
        spu.setCreateTime(new Date());
        spu.setUpdateTime(spu.getCreateTime()); // 新增时，更新时间和创建时间一致
        spu.setId(null); // 防止id注入，显式的设置id为null
        this.save(spu);
        Long spuId = spu.getId();

        // 1.2、保存pms_spu_desc
        // 注意：pms_spu_desc表的主键是spu_id,需要在实体类中配置该主键不是自增主键
        List<String> spuImages = spu.getSpuImages();
        if(!CollectionUtils.isEmpty(spuImages)) {
            SpuDescEntity spuDescEntity = new SpuDescEntity();
            spuDescEntity.setSpuId(spuId);
            spuDescEntity.setDecript(StringUtils.join(spuImages, ","));
            this.spuDescMapper.insert(spuDescEntity);
        }

        // 1.3、保存pms_spu_attr_value
        List<SpuAttrValueVo> baseAttrs = spu.getBaseAttrs();
        if (!CollectionUtils.isEmpty(baseAttrs)) {
            List<SpuAttrValueEntity> spuAttrValueEntities = baseAttrs.stream().map(spuAttrValueVo -> {
                SpuAttrValueEntity baseEntity = new SpuAttrValueEntity();
                BeanUtils.copyProperties(spuAttrValueVo, baseEntity);
                baseEntity.setSpuId(spuId);
                baseEntity.setSort(1);
                baseEntity.setId(null);
                return baseEntity;
            }).collect(Collectors.toList());
            this.spuAttrValueService.saveBatch(spuAttrValueEntities);
        }

        // 2、保存sku相关信息：pms_sku pms_skuImages pms_sku_attr_value
        List<SkuVo> skus = spu.getSkus();
        if(CollectionUtils.isEmpty(skus)){
            return;
        }
        // 遍历保存sku的相关信息
        skus.forEach(skuVo -> {
            // 2.1、保存pms_sku
            skuVo.setSpuId(spuId);
            skuVo.setId(null);
            skuVo.setBrandId(spu.getBrandId());
            skuVo.setCatagoryId(spu.getCategoryId());
            List<String> images = skuVo.getImages();
            if(!CollectionUtils.isEmpty(images)){
                skuVo.setDefaultImage(skuVo.getDefaultImage() == null ? images.get(0) : skuVo.getDefaultImage());
            }
            skuMapper.insert(skuVo);

            Long skuId = skuVo.getId();

            // 2.2、保存pms_skuImages
            if(!CollectionUtils.isEmpty(images)){
                List<SkuImagesEntity> imagesEntities = images.stream().map(image ->{
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setId(null);
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setUrl(image);
                    skuImagesEntity.setSort(1);
                    skuImagesEntity.setDefaultStatus(0);
                    if(!StringUtils.equals(skuVo.getDefaultImage(), image)){
                        skuImagesEntity.setDefaultStatus(1);
                    }
                    return skuImagesEntity;
                }).collect(Collectors.toList());
                this.skuImagesService.saveBatch(imagesEntities);
            }
            // 2.3、保存pms_sku_attr_value
            List<SkuAttrValueEntity> saleAttrs = skuVo.getSaleAttrs();
            if(!CollectionUtils.isEmpty(saleAttrs)){
                saleAttrs.forEach(attr->{
                    attr.setSkuId(skuId);
                    attr.setId(null);
                    attr.setSort(0);
                });
                this.attrValueService.saveBatch(saleAttrs);
            }


            // 3、优惠信息表保存
            SkuSaleVo skuSaleVo = new SkuSaleVo();
            BeanUtils.copyProperties(skuVo,skuSaleVo);
            skuSaleVo.setSkuId(skuId);
            this.gmallSmsClient.saveSkuSales(skuSaleVo);

        });

        this.rabbitTemplate.convertAndSend("PMS_SPU_EXCHANGE","item.insert",spuId);

    }

}