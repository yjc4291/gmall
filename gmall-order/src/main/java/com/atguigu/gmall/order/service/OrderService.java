package com.atguigu.gmall.order.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.bean.UserInfo;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.order.feign.*;
import com.atguigu.gmall.order.interceptor.LoginInterceptor;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private GmallCartClient cartClient;

    @Autowired
    private GmallOmsClient omsClient;

    @Autowired
    private GmallUmsClient umsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private final static String TOKEN_PREFIX = "order:token:";

    public OrderConfirmVo confirm() {
        OrderConfirmVo orderConfirmVo = new OrderConfirmVo();

        // 获取用户信息
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();

        // 获取用户收货地址
        ResponseVo<List<UserAddressEntity>> listResponseVo = this.umsClient.queryAddressesByUserId(userId);
        List<UserAddressEntity> addresses = listResponseVo.getData();
        orderConfirmVo.setAddresses(addresses);

        // 获取购物车中选中的对象
        ResponseVo<List<Cart>> cartResponseVo = this.cartClient.queryCheckedCartsByUserId(userId);
        List<Cart> carts = cartResponseVo.getData();
        if (CollectionUtils.isEmpty(carts)){
            throw new OrderException("没有选中的购物车信息。。。。");
        }

        orderConfirmVo.setOrderItems(carts.stream().map( cart -> {
            OrderItemVo orderItemVo = new OrderItemVo();
            orderItemVo.setSkuId(cart.getSkuId());
            orderItemVo.setCount(cart.getCount());

            // 查询订单列表中的sku相关信息
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(cart.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity != null){
                orderItemVo.setDefaultImage(skuEntity.getDefaultImage());
                orderItemVo.setPrice(skuEntity.getPrice());
                orderItemVo.setTitle(skuEntity.getTitle());
                orderItemVo.setWeight(new BigDecimal(skuEntity.getWeight()));
            }

            // 查询库存信息
            ResponseVo<List<WareSkuEntity>> wareResponseVo = this.wmsClient.queryWareSkuBySkuId(cart.getSkuId());
            List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)){
                orderItemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }

            // 查询销售属性
            ResponseVo<List<SkuAttrValueEntity>> skuAttrValueResponseVo = this.pmsClient.querySaleAttrValuesBySkuId(cart.getSkuId());
            List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrValueResponseVo.getData();
            orderItemVo.setSaleAttrs(skuAttrValueEntities);

            // 查询营销信息
            ResponseVo<List<ItemSaleVo>> itemSaleResponseVo = this.smsClient.querySaleVoBySkuId(cart.getSkuId());
            List<ItemSaleVo> itemSaleVos = itemSaleResponseVo.getData();
            orderItemVo.setSales(itemSaleVos);

            return orderItemVo;
        }).collect(Collectors.toList()));

        // 根据用户id查询用户信息
        ResponseVo<UserEntity> userEntityResponseVo = this.umsClient.queryUserById(userId);
        UserEntity userEntity = userEntityResponseVo.getData();
        orderConfirmVo.setBounds(userEntity.getIntegration());

        // 设置防重的唯一标识，响应给页面一份，保存到redis中一份
        String orderToken = IdWorker.getTimeId();
        orderConfirmVo.setOrderToken(orderToken);
        System.out.println("orderToken：" +  orderToken);
        this.redisTemplate.opsForValue().set(TOKEN_PREFIX + orderToken, orderToken);

        return orderConfirmVo;
    }

    public void submit(OrderSubmitVo submitVo) {
        //1.防重：判断是否重复提交     redis完成
        String orderToken = submitVo.getOrderToken();
        System.out.println("orderToken：" +  orderToken);
        if (StringUtils.isBlank(orderToken)){
            throw new OrderException("非法请求。。。");
        }
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] " +
                "then return redis.call('del', KEYS[1]) " +
                "else return 0 " +
                "end";
        Boolean flag = this.redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class),
                Arrays.asList(TOKEN_PREFIX + orderToken), orderToken);
        if(!flag){
            throw new OrderException("请不要重复提交。。。");
        }

        //2.验总价：获取页面上的总价，和数据库中的商品实时价格是否一致，将来根据orderItems中的skuId查询sku即可
        BigDecimal totalPrice = submitVo.getTotalPrice(); // 用户提交的页面总价格
        if (totalPrice == null){
            throw new OrderException("非法请求。。。");
        }
        List<OrderItemVo> items = submitVo.getItems();
        if (CollectionUtils.isEmpty(items)){
            throw new OrderException("请选择要购买的商品，再来下单。。。");
        }
        BigDecimal currentTotalPrice = items.stream().map(item -> {
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(item.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity != null) {
                return skuEntity.getPrice().multiply(item.getCount());
            }
            return new BigDecimal(0);
        }).reduce((a, b) -> a.add(b)).get();
        if (totalPrice.compareTo(currentTotalPrice) != 0){
            throw new OrderException("页面已过期，请刷新后再试。。。");
        }

        //3.验库存并锁库存
        List<SkuLockVo> skuLockVos = items.stream().map( item -> {
            SkuLockVo skuLockVo = new SkuLockVo();
            skuLockVo.setSkuId(item.getSkuId());
            skuLockVo.setCount(item.getCount().intValue());
            return skuLockVo;
        }).collect(Collectors.toList());
        ResponseVo<List<SkuLockVo>> skuLockVoList = this.wmsClient.checkAndLockStock(skuLockVos, orderToken);
        List<SkuLockVo> lockVos = skuLockVoList.getData();
        if (!CollectionUtils.isEmpty(lockVos)){
            throw new OrderException(JSON.toJSONString(lockVos));
        }

        //4.创建订单并添加订单详情
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();
        try {
            this.omsClient.addOrder(submitVo, userId);
            this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "order.ttl", orderToken);
        } catch (Exception e) {
            e.printStackTrace();
            // 解锁库存
            this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "order.failure", orderToken);
            throw new OrderException("服务器错误！");
        }


        //5.删除购物车中对应的商品记录，异步删除
        Map<String, Object> map = new HashMap<>();
        map.put("userId",userId);
        List<Long> skuIds = items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
        map.put("skuIds", JSON.toJSONString(skuIds));
        this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "cart_delete", map);


    }



}

