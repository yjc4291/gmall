package com.atguigu.gmall.ums.mapper;

import com.atguigu.gmall.ums.entity.UserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表
 * 
 * @author Yan
 * @email 18112918867@163.com
 * @date 2020-08-21 19:12:05
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
	
}
