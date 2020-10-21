package com.atguigu.gmall.item.service;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.ItemException;
import com.atguigu.gmall.item.feign.GmallPmsClient;
import com.atguigu.gmall.item.feign.GmallSmsClient;
import com.atguigu.gmall.item.feign.GmallWmsClient;
import com.atguigu.gmall.item.vo.ItemVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.GroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class ItemService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    public ItemVo load(Long skuId) {
        ItemVo itemVo = new ItemVo();

        CompletableFuture<SkuEntity> skuCompletableFuture = CompletableFuture.supplyAsync(() -> {
            //1.根据skuId查询sku信息
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(skuId);
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity == null) {
                throw new ItemException("该skuId对应的商品不存在");
            }
            itemVo.setSkuId(skuId);
            itemVo.setTitle(skuEntity.getTitle());
            itemVo.setSubTitle(skuEntity.getSubtitle());
            itemVo.setDefaultImages(skuEntity.getDefaultImage());
            itemVo.setPrice(skuEntity.getPrice());
            itemVo.setWeight(new BigDecimal(skuEntity.getWeight()));
            return skuEntity;
        }, threadPoolExecutor);

        CompletableFuture<Void> cateCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuEntity -> {
            //2.根据cid3查询一二三级分类集合
            ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryAllCategoriesByCid3(skuEntity.getCatagoryId());
            List<CategoryEntity> categoryEntities = listResponseVo.getData();
            if (!CollectionUtils.isEmpty(categoryEntities)) {
                itemVo.setCategoryEntities(categoryEntities);
            }
        }, threadPoolExecutor);

        CompletableFuture<Void> brandCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuEntity -> {
            //3.根据品牌id查询品牌信息
            ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsClient.queryBrandById(skuEntity.getBrandId());
            BrandEntity brandEntity = brandEntityResponseVo.getData();
            if (brandEntity != null) {
                itemVo.setBrandName(brandEntity.getName());
                itemVo.setBrandId(brandEntity.getId());
            }
        }, threadPoolExecutor);

        CompletableFuture<Void> spuCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuEntity -> {
            //4.根据spuId查询spu信息
            ResponseVo<SpuEntity> spuEntityResponseVo = this.pmsClient.querySpuById(skuEntity.getSpuId());
            SpuEntity spuEntity = spuEntityResponseVo.getData();
            if (spuEntity != null) {
                itemVo.setSpuId(spuEntity.getId());
                itemVo.setSpuName(spuEntity.getName());
            }
        }, threadPoolExecutor);

        CompletableFuture<Void> salesCompletableFuture = CompletableFuture.runAsync(() -> {
            //5.根据skuId查询优惠信息（sms）
            ResponseVo<List<ItemSaleVo>> itemSaleVoListResponse = this.smsClient.querySaleVoBySkuId(skuId);
            List<ItemSaleVo> itemSaleVos = itemSaleVoListResponse.getData();
            if (!CollectionUtils.isEmpty(itemSaleVos)) {
                itemVo.setSales(itemSaleVos);
            }
        }, threadPoolExecutor);

        CompletableFuture<Void> wareCompletableFuture = CompletableFuture.runAsync(() -> {
            //6.根据skuId查询库存信息 wms
            ResponseVo<List<WareSkuEntity>> wareSkuEntityListResponse = this.wmsClient.queryWareSkuBySkuId(skuId);
            List<WareSkuEntity> wareSkuEntities = wareSkuEntityListResponse.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                itemVo.setStore(true);
            }
        }, threadPoolExecutor);

        CompletableFuture<Void> imageCompletableFuture = CompletableFuture.runAsync(() -> {
            //7.根据skuId查询sku的图片列表
            ResponseVo<List<SkuImagesEntity>> skuImagesEntityList = this.pmsClient.queryImagesBySkuId(skuId);
            List<SkuImagesEntity> skuImagesEntities = skuImagesEntityList.getData();
            if (!CollectionUtils.isEmpty(skuImagesEntities)) {
                itemVo.setImages(skuImagesEntities);
            }
        }, threadPoolExecutor);

        CompletableFuture<Void> saleAttrsCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuEntity -> {
            //8.根据spuId查询spu下所有sku的销售属性组合
            ResponseVo<List<SaleAttrValueVo>> saleAttrValueVoResponse = this.pmsClient.querySaleAttrValueVoBySpuId(skuEntity.getSpuId());
            List<SaleAttrValueVo> saleAttrValueVos = saleAttrValueVoResponse.getData();
            if (!CollectionUtils.isEmpty(saleAttrValueVos)) {
                itemVo.setSaleAttrs(saleAttrValueVos);
            }
        }, threadPoolExecutor);

        CompletableFuture<Void> saleAttrCompletableFuture = CompletableFuture.runAsync(() -> {
            //9.根据skuId查询当前sku的销售属性
            ResponseVo<List<SkuAttrValueEntity>> saleAttrValuesResponse = this.pmsClient.querySaleAttrValuesBySkuId(skuId);
            List<SkuAttrValueEntity> skuAttrValueEntities = saleAttrValuesResponse.getData();
            if (!CollectionUtils.isEmpty(skuAttrValueEntities)) {
                // [{attrId:3, attrName: 机身颜色, attrValue: 白色},{}, {}] ==> {3: 白色, 4: 8G, 5: 512G}
                Map<Long, String> saleAttr = skuAttrValueEntities.stream().collect(Collectors.toMap(SkuAttrValueEntity::getAttrId, SkuAttrValueEntity::getAttrValue));
                itemVo.setSaleAttr(saleAttr);
            }
        }, threadPoolExecutor);

        CompletableFuture<Void> mappingCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuEntity -> {
            //10.根据spuId查询spu下所有销售属性组合和skuId的映射关系
            ResponseVo<String> mappingResponseVo = this.pmsClient.querySaleAttrValuesMappingSkuIdBySpuId(skuEntity.getSpuId());
            String json = mappingResponseVo.getData();
            itemVo.setSkuJsons(json);
        }, threadPoolExecutor);

        CompletableFuture<Void> descCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuEntity -> {
            //11.根据spuId查询spu的海报信息列表
            ResponseVo<SpuDescEntity> spuDescEntityResponseVo = this.pmsClient.querySpuDescById(skuEntity.getSpuId());
            SpuDescEntity spuDescEntity = spuDescEntityResponseVo.getData();
            if (spuDescEntity != null && StringUtils.isNotBlank(spuDescEntity.getDecript())) {
                String[] urls = StringUtils.split(spuDescEntity.getDecript(), ",");
                itemVo.setSpuImages(Arrays.asList(urls));
            }
        }, threadPoolExecutor);

        CompletableFuture<Void> groupCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<List<GroupVo>> groupResponseVo = this.pmsClient.queryGroupVoByCidAndSpuIdAndSkuId(skuEntity.getCatagoryId(), skuEntity.getSpuId(), skuId);
            List<GroupVo> groupVos = groupResponseVo.getData();
            itemVo.setGroups(groupVos);
        }, threadPoolExecutor);
        //12.根据cid3、spuId、skuId查询分组及组下的规格参数以及值

        CompletableFuture.allOf(cateCompletableFuture,brandCompletableFuture,spuCompletableFuture,
                salesCompletableFuture,wareCompletableFuture,imageCompletableFuture,
                saleAttrsCompletableFuture, saleAttrCompletableFuture,mappingCompletableFuture,
                descCompletableFuture,groupCompletableFuture
        ).join();

        return itemVo;
    }
}
