package com.atguigu.gmall.cart.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.cart.feign.GmallSmsClient;
import com.atguigu.gmall.cart.feign.GmallWmsClient;
import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.common.bean.UserInfo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.CartException;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private CartAsyncService cartAsyncService;

    private final static String KEY_PREFIX = "cart:info:";

    private final static String PRICE_PREFIX = "cart:price:";

    private final static ObjectMapper MAPPER = new ObjectMapper();


    public void addCart(Cart cart) {
        // 获取key
        String userId = getUserId();
        String key = KEY_PREFIX + userId;

        // 2.获取该用户的购物车，hashOps相当于内存map<skuId, cart的json字符串>
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);

        // 3.判断该用户是否已有该购物记录
        String skuString = cart.getSkuId().toString();
        BigDecimal count = cart.getCount();
        try {
            if (hashOps.hasKey(skuString)) {
                // 有，则更新数量

                String json = hashOps.get(skuString).toString();
                cart = MAPPER.readValue(json, Cart.class);
                cart.setCount(cart.getCount().add(count));

                // 写回数据库
                //this.cartMapper.update(cart, new UpdateWrapper<Cart>().eq("user_id", userId).eq("sku_id", skuString));
                this.cartAsyncService.updateByUserIdAndSkuId(userId, cart, skuString);

            } else {

                // 无，则新增购物记录 cart中只有skuId、count，其他的参数需要设置
                cart.setUserId(userId);
                ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(cart.getSkuId());
                SkuEntity skuEntity = skuEntityResponseVo.getData();
                if (skuEntity == null) {
                    throw new CartException("您加入购物车的商品不存在。。。");
                }
                cart.setDefaultImage(skuEntity.getDefaultImage());
                cart.setPrice(skuEntity.getPrice());
                cart.setTitle(skuEntity.getTitle());

                // 查询销售属性
                ResponseVo<List<SkuAttrValueEntity>> saleAttrsResponse = this.pmsClient.querySaleAttrValuesBySkuId(cart.getSkuId());
                List<SkuAttrValueEntity> skuAttrValueEntities = saleAttrsResponse.getData();
                cart.setSaleAttrs(MAPPER.writeValueAsString(skuAttrValueEntities));

                // 查询营销信息
                ResponseVo<List<ItemSaleVo>> responseVo = this.smsClient.querySaleVoBySkuId(cart.getSkuId());
                List<ItemSaleVo> itemSaleVos = responseVo.getData();
                cart.setSales(MAPPER.writeValueAsString(itemSaleVos));

                // 查询商品库存信息
                ResponseVo<List<WareSkuEntity>> wareResponseVo = this.wmsClient.queryWareSkuBySkuId(cart.getSkuId());
                List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
                if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                    cart.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
                }

                cart.setCheck(true);
                this.cartAsyncService.addCart(userId, cart);
                this.redisTemplate.opsForValue().set(PRICE_PREFIX + skuString, skuEntity.getPrice().toString());
            }
            // 写入redis中
            hashOps.put(skuString, MAPPER.writeValueAsString(cart));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

    }

    public List<Cart> queryCartsByUserId() {
        // 1.获取userKey，查询未登录的购物车
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userKey = userInfo.getUserKey();
        String unLoginKey = KEY_PREFIX + userKey;
        // 查询未登录的购物车
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(unLoginKey);
        List<Object> values = hashOps.values(); // json集合
        List<Cart> unLoginCarts = null;
        if (!CollectionUtils.isEmpty(values)){
            unLoginCarts = values.stream().map(cartJson -> {
                try {
                    Cart cart = MAPPER.readValue(cartJson.toString(), Cart.class);
                    cart.setCurrentPrice(new BigDecimal(this.redisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId().toString())));
                    return cart;
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                    return null;
                }
            }).collect(Collectors.toList());
        }

        // 2.获取userId，获取userId是否为空
        Long userId = userInfo.getUserId();
        if (userId == null){
            return unLoginCarts;
        }

        // 3.合并购物车
        String loginKey = KEY_PREFIX + userId;
        BoundHashOperations<String, Object, Object> loginHashOps = this.redisTemplate.boundHashOps(loginKey);
        if (!CollectionUtils.isEmpty(unLoginCarts)){
            unLoginCarts.forEach( cart -> {
                try {
                    if (loginHashOps.hasKey(cart.getSkuId().toString())){
                        BigDecimal count = cart.getCount();
                        String json = loginHashOps.get(cart.getSkuId().toString()).toString();
                        cart = MAPPER.readValue(json, Cart.class);
                        cart.setCount(cart.getCount().add(count));
                        this.cartAsyncService.updateByUserIdAndSkuId(userId.toString(),cart,  cart.getSkuId().toString());
                    } else {
                        cart.setUserId(userId.toString());
                        this.addCart(cart);
                    }
                    loginHashOps.put(cart.getSkuId().toString(), MAPPER.writeValueAsString(cart));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            });
        }

        // 4.删除未登录的购物车
        this.redisTemplate.delete(unLoginKey);
        this.cartAsyncService.deleteCartByUserId(userKey);

        // 5.查询登录状态的购物车
        List<Object> cartJsons = loginHashOps.values();
        if (!CollectionUtils.isEmpty(cartJsons)){
            return cartJsons.stream().map(cartJson ->{
                try {
                    Cart cart = MAPPER.readValue(cartJson.toString(), Cart.class);
                    cart.setCurrentPrice(new BigDecimal(this.redisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId().toString())));
                    return cart;
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                return null;
            }).collect(Collectors.toList());
        }

        return null;
    }

    private String getUserId() {
        // 获取key
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userId = null;
        if (userInfo.getUserId() == null) {
            userId = userInfo.getUserKey();
        } else {
            userId = userInfo.getUserId().toString();
        }
        return userId;
    }

    public Cart queryCartBySkuId(Long skuId) throws JsonProcessingException {
        String userId = this.getUserId();
        String key = KEY_PREFIX + userId;

        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);

        if (hashOps.hasKey(skuId.toString())){
            String json = hashOps.get(skuId.toString()).toString();
            if (StringUtils.isNotBlank(json)){
                return MAPPER.readValue(json, Cart.class);
            }
        }
        return null;
    }

    @Async
    public void executor1(){
        try {
            System.out.println("异步方法executor1开始执行" + Thread.currentThread().getName());
            TimeUnit.SECONDS.sleep(5);
            System.out.println("异步方法executor1执行结束。。。。");

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Async
    public void executor2(){
        try {
            System.out.println("异步方法executor2开始执行" + Thread.currentThread().getName());
            TimeUnit.SECONDS.sleep(4);
            int i = 1 /0;
            System.out.println("异步方法executor2执行结束。。。。");

        } catch (InterruptedException e) {
            System.out.println("service 捕获异常后打印" + e.getMessage());
        }

    }

    public void updateNum(Cart cart) {
        String userId = this.getUserId();
        String key = KEY_PREFIX + userId;


        // 获取该用户的购物车操作对象
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        if (hashOps.hasKey(cart.getSkuId().toString())){
            try {
                BigDecimal count = cart.getCount();
                String cartJson = hashOps.get(cart.getSkuId().toString()).toString();
                cart = MAPPER.readValue(cartJson, Cart.class);
                cart.setCount(count);

                hashOps.put(cart.getSkuId().toString(), MAPPER.writeValueAsString(cart));
                this.cartAsyncService.updateByUserIdAndSkuId(userId, cart, cart.getSkuId().toString());
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }

    public void deleteCart(Long skuId) {

        String userId = this.getUserId();
        String key = KEY_PREFIX + userId;

        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        if (hashOps.hasKey(skuId.toString())){
            hashOps.delete(skuId.toString());
            this.cartAsyncService.deleteCartByUserIdAndSkuId(userId, skuId);
        }
    }

    public List<Cart> queryCheckedCartsByUserId(Long userId) {
        // 查询该用户所有的购物车记录
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId.toString());
        List<Object> jsons = hashOps.values();
        if (CollectionUtils.isEmpty(jsons)){
            return null;
        }

        // 查询该用户已勾选的购物记录
        List<Cart> carts = jsons.stream().map(json -> {
            return JSON.parseObject(json.toString(), Cart.class);
        }).filter(Cart::getCheck).collect(Collectors.toList());

        return carts;
    }


//    @Async
//    public ListenableFuture<String> executor1(){
//        try {
//            System.out.println("异步方法executor1开始执行");
//            TimeUnit.SECONDS.sleep(5);
//            System.out.println("异步方法executor1执行结束。。。。");
//
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//            return AsyncResult.forExecutionException(e);
//        }
//
//        return AsyncResult.forValue("hello executor1");
//    }
//
//    @Async
//    public ListenableFuture<String> executor2(){
//        try {
//            System.out.println("异步方法executor2开始执行");
//            TimeUnit.SECONDS.sleep(4);
//            int i = 1 /0;
//            System.out.println("异步方法executor2执行结束。。。。");
//
//        } catch (Exception e) {
//            System.out.println("service 捕获异常后打印" + e.getMessage());
//            return AsyncResult.forExecutionException(e);
//        }
//
//        return AsyncResult.forValue("hello executor2");
//    }
}
