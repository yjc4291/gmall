package com.atguigu.gmall.ums.service.impl;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.ums.mapper.UserMapper;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.ums.service.UserService;
import org.springframework.util.CollectionUtils;


@Service("userService")
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<UserEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<UserEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public Boolean checkData(String data, Integer type) {
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        switch (type){
            case 1:
                queryWrapper.eq("username",data);
                break;
            case 2:
                queryWrapper.eq("phone", data);
                break;
            case 3:
                queryWrapper.eq("email", data);
                break;
            default:
                return null;
        }

        return this.userMapper.selectCount(queryWrapper) == 0;
    }

    @Override
    public void register(UserEntity userEntity, String code) {
        // 1.校验短信验证码
        // String cacheCode = this.redisTemplate.opsForValue().get(KEY_PREFIX + userEntity.getPhone());
        // if (!StringUtils.equals(code, cacheCode)) {
        //     return false;
        // }

        // 2.生成盐
        String salt = StringUtils.substring(UUID.randomUUID().toString(),0,6);
        userEntity.setSalt(salt);

        // 3.对密码加盐加密
        userEntity.setPassword(DigestUtils.md5Hex(userEntity.getPassword()+ salt));

        // 4.新增用户
        userEntity.setLevelId(1l);
        userEntity.setSourceType(1);
        userEntity.setIntegration(1000);
        userEntity.setGrowth(1000);
        userEntity.setStatus(1);
        userEntity.setCreateTime(new Date());
        boolean result = this.save(userEntity);

        // 5.删除redis中的验证码 TODO:
        // if(result){
        // 注册成功，删除redis中的记录
        // this.redisTemplate.delete(KEY_PREFIX + memberEntity.getPhone());
        // }
    }

    @Override
    public UserEntity queryUser(String loginName, String password) {

        // 1.先根据登录名查询用户
        List<UserEntity> userEntities = this.list(new QueryWrapper<UserEntity>()
                .eq("username", loginName)
                .or()
                .eq("email", loginName)
                .or()
                .eq("phone", loginName));

        // 2.判断用户信息是否为空
        if(CollectionUtils.isEmpty(userEntities)){
            return null;
        }

        String pwd = null;
        for (UserEntity userEntity : userEntities) {
            // 3.获取该用户的盐，对用户输入的密码加盐加密
            pwd = DigestUtils.md5Hex(password + userEntity.getSalt());

            // 4.用数据库中的密码和加密后的密码进行比较
            if(StringUtils.equals(userEntity.getPassword(), pwd)){
                return userEntity;
            }

        }

        return null;
    }

}