package com.atguigu.gmall.index.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.config.GmallCache;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.index.util.DistributedLock;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class IndexService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private DistributedLock distributedLock;

    @Autowired
    private RedissonClient redissonClient;

    public static final String KEY_PREFIX = "index:category:";

    public List<CategoryEntity> querylvlOneCategories() {

        // 从缓存获取一级分类
        String categories = this.redisTemplate.opsForValue().get(KEY_PREFIX);
        if(StringUtils.isNotBlank(categories)){
            // 如果缓存中，直接返回
            List<CategoryEntity> categoryEntities = JSON.parseArray(categories, CategoryEntity.class);
            return categoryEntities;
        }

        // 缓存中没有，直接从数据库中获取
        ResponseVo<List<CategoryEntity>> responseVo = pmsClient.queryCategoriesByPid(0l);
        // 将数据缓存到redis中
        this.redisTemplate.opsForValue().set(KEY_PREFIX,JSON.toJSONString(responseVo.getData()),30, TimeUnit.DAYS);
        return responseVo.getData();
    }

    // 缓存注解
    @GmallCache(prefix = KEY_PREFIX, timeout = 43200, random = 7200, lock = "lock")
    public List<CategoryEntity> querylvlTwoCategoriesWithSub(Long pid) {

        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategoriesWithSubByPid(pid);
        List<CategoryEntity> categoryEntityList = listResponseVo.getData();
        return categoryEntityList;

    }

    public List<CategoryEntity> querylvlTwoCategoriesWithSub2(Long pid) {
        // 从缓存中获取
        String json = this.redisTemplate.opsForValue().get(KEY_PREFIX + pid);
        if(StringUtils.isNotBlank(json)){
            // 如果缓存中有，直接返回
            List<CategoryEntity> categoryEntities = JSON.parseArray(json, CategoryEntity.class);
            return categoryEntities;
        }

        // 缓存中没有，添加锁，避免缓存击穿
        RLock lock = this.redissonClient.getLock("lock" + pid);
        lock.lock();

        // 再次判断缓存中是否有数据，可能其他线程已经将数据存到缓存中
        String json2 = this.redisTemplate.opsForValue().get(KEY_PREFIX + pid);
        if(StringUtils.isNotBlank(json2)){
            // 如果缓存中有，直接返回
            List<CategoryEntity> categoryEntities = JSON.parseArray(json2, CategoryEntity.class);
            lock.unlock();
            return categoryEntities;
        }

        // 缓存中没有，从数据库中查询
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategoriesWithSubByPid(pid);
        List<CategoryEntity> categoryEntityList = listResponseVo.getData();
        // 将数据缓存到redis中
        // 防止缓存穿透，即使数据为null，依然要缓存
        // 防止缓存雪崩，给缓存过期时间添加随机值
        this.redisTemplate.opsForValue().set(KEY_PREFIX + pid, JSON.toJSONString(categoryEntityList),30 + new Random().nextInt(5), TimeUnit.DAYS);

        lock.unlock();
        return categoryEntityList;

    }

    public void testLock(){
        RLock lock = this.redissonClient.getLock("lock");
        lock.lock();
        // 获取锁成功，执行操作
        String numString = this.redisTemplate.opsForValue().get("num");
        if(StringUtils.isBlank(numString)){
            this.redisTemplate.opsForValue().set("num","0");
            numString = "0";
        }
        int num = Integer.parseInt(numString);
        this.redisTemplate.opsForValue().set("num",String.valueOf(++num));

        lock.unlock();
    }

    public void testLock3() {
        // 通过setnx获取锁
        String uuid = UUID.randomUUID().toString();
        Boolean lock = this.redisTemplate.opsForValue().setIfAbsent("lock", uuid, 3, TimeUnit.SECONDS);
        // 获取失败，则重试
        if(!lock){
            try {
                Thread.sleep(100);
                testLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            //this.redisTemplate.expire("lock", 3, TimeUnit.SECONDS);
            // 获取成功，则执行业务操作
            String numString = this.redisTemplate.opsForValue().get("num");
            if (StringUtils.isBlank(numString)) {
                this.redisTemplate.opsForValue().set("num", "0");
                return;
            }
            int num = Integer.parseInt(numString);
            this.redisTemplate.opsForValue().set("num", String.valueOf(++num));

            // 释放锁（del）
            // 为了防误删，需要判断当前锁是不是自己的锁
            // 为了保证原子性，这里来使用lua脚本
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            this.redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList("lock"), uuid);

//            if(StringUtils.equals(uuid, this.redisTemplate.opsForValue().get("lock"))){
//                this.redisTemplate.delete("lock");
//            }
        }
    }

    public void testLock2(){

        String uuid = UUID.randomUUID().toString();
        // 获取锁
        Boolean lock = this.distributedLock.tryLock("lock", uuid, 30l);
        if(lock){
            // 获取锁成功，执行操作
            String numString = this.redisTemplate.opsForValue().get("num");
            if(StringUtils.isBlank(numString)){
                this.redisTemplate.opsForValue().set("num","0");
                numString = "0";
            }
            try {
                TimeUnit.SECONDS.sleep(60);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int num = Integer.parseInt(numString);
            this.redisTemplate.opsForValue().set("num",String.valueOf(++num));
        }

        // 重入锁
        testSubLock(uuid);

        // 释放锁
        this.distributedLock.unLock("lock", uuid);
    }


    public void testSubLock(String uuid){
        this.distributedLock.tryLock("lock", uuid, 30l);
        //System.out.println("==========================================");
        this.distributedLock.unLock("lock", uuid);
    }

    public void testRead() {
        RReadWriteLock rwLock = this.redissonClient.getReadWriteLock("rwLock");
        rwLock.readLock().lock(10, TimeUnit.SECONDS);
        System.out.println("读操作。。。");
    }

    public void testWrite() {
        RReadWriteLock rwLock = this.redissonClient.getReadWriteLock("rwLock");
        rwLock.writeLock().lock(10, TimeUnit.SECONDS);
        System.out.println("这是写操作。。。");
        //rwLock.writeLock().unlock();
    }

    public void testCountDown() {
        RCountDownLatch latch = this.redissonClient.getCountDownLatch("latch");
        // TODO：业务
        latch.countDown();
    }

    public void testLatch() throws InterruptedException {
        RCountDownLatch latch = this.redissonClient.getCountDownLatch("latch");
        latch.trySetCount(6);
        latch.await();
        // TODO：业务
    }
}
