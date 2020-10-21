package com.atguigu.gmall.search.listener;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.feign.GmallWmsClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValueVo;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GoodsListener {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GoodsRepository goodsRepository;


    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "SEARCH_GOODS_QUEUE",durable = "true"),
            exchange = @Exchange(value = "PMS_SPU_EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"item.insert"}
    ))
    public void listener(Long spuId, Channel channel, Message message) throws IOException {
        ResponseVo<List<SkuEntity>> skuResponseVo = this.pmsClient.querySkusBySpuId(spuId);
        List<SkuEntity> skuEntities = skuResponseVo.getData();
        if(!CollectionUtils.isEmpty(skuEntities)){
            List<Goods> goodsList = skuEntities.stream().map(skuEntity -> {
                Goods goods = new Goods();
                // sku相关信息
                goods.setSkuId(skuEntity.getId());
                goods.setTitle(skuEntity.getTitle());
                goods.setSubTitle(skuEntity.getSubtitle());
                goods.setPrice(skuEntity.getPrice().doubleValue());
                goods.setDefaultImage(skuEntity.getDefaultImage());

                // spu相关信息 创建时间
                ResponseVo<SpuEntity> spuEntityResponseVo = pmsClient.querySpuById(spuId);
                SpuEntity spuEntity = spuEntityResponseVo.getData();
                goods.setCreateTime(spuEntity.getCreateTime());

                // 品牌信息
                ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsClient.queryBrandById(skuEntity.getBrandId());
                BrandEntity brandEntity = brandEntityResponseVo.getData();
                if(brandEntity != null){
                    goods.setBrandId(brandEntity.getId());
                    goods.setBrandName(brandEntity.getName());
                    goods.setLogo(brandEntity.getLogo());
                }

                // 分类信息
                ResponseVo<CategoryEntity> categoryEntityResponseVo = this.pmsClient.queryCategoryById(skuEntity.getCatagoryId());
                CategoryEntity categoryEntity = categoryEntityResponseVo.getData();
                if(categoryEntity != null){
                    goods.setCategoryId(categoryEntity.getId());
                    goods.setCategoryName(categoryEntity.getName());
                }

                // 销量与库存
                ResponseVo<List<WareSkuEntity>> wakeSkuResponseVo = this.wmsClient.queryWareSkuBySkuId(skuEntity.getId());
                List<WareSkuEntity> wareSkuEntities = wakeSkuResponseVo.getData();
                if(!CollectionUtils.isEmpty(wareSkuEntities)) {
                    goods.setSales(wareSkuEntities.stream().map(WareSkuEntity::getSales).reduce((a,b)-> a + b).get());
                    goods.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
                }

                List<SearchAttrValueVo> attrValueVos = new ArrayList<>();  // 用于收集所有的搜索属性
                ResponseVo<List<SkuAttrValueEntity>> skuAttrValueResponseVo = this.pmsClient.querySearchAttrValuesBySkuId(skuEntity.getId());
                List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrValueResponseVo.getData();
                if(!CollectionUtils.isEmpty(skuAttrValueEntities)){
                    attrValueVos.addAll(skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                        SearchAttrValueVo searchAttrValueVo = new SearchAttrValueVo();
                        BeanUtils.copyProperties(skuAttrValueEntity,searchAttrValueVo);
                        return searchAttrValueVo;
                    }).collect(Collectors.toList()));
                }

                ResponseVo<List<SpuAttrValueEntity>> spuAttrValueResponseVo = this.pmsClient.querySearchAttrValuesBySpuId(spuEntity.getId());
                List<SpuAttrValueEntity> spuAttrValueEntities = spuAttrValueResponseVo.getData();
                if(!CollectionUtils.isEmpty(spuAttrValueEntities)){
                    attrValueVos.addAll(spuAttrValueEntities.stream().map(spuAttrValueEntity -> {
                        SearchAttrValueVo searchAttrValueVo = new SearchAttrValueVo();
                        BeanUtils.copyProperties(spuAttrValueEntity,searchAttrValueVo);
                        return searchAttrValueVo;
                    }).collect(Collectors.toList()));
                }

                goods.setSearchAttrs(attrValueVos);

                return goods;
            }).collect(Collectors.toList());
            this.goodsRepository.saveAll(goodsList);
        }

        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e) {
            e.printStackTrace();
            if(message.getMessageProperties().getRedelivered()){
                // 重试一次，如果依然无法正常消费，不再入队。如果绑定死信队列会进入死信队列，如果没有绑定死信队列会丢失
                // 可以把消费失败的消息保存到redis或者mysql数据库
                channel.basicReject(message.getMessageProperties().getDeliveryTag(),false);
            }else {
                // 不确定消息，requeue设置为true，就会重新入队，可以进行重试消费
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            }
        }
    }

}
