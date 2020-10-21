package com.atguigu.gmall.pms.mapper;

import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * spu属性值
 * 
 * @author Yan
 * @email 18112918867@163.com
 * @date 2020-08-21 13:53:02
 */
@Mapper
public interface SpuAttrValueMapper extends BaseMapper<SpuAttrValueEntity> {

    List<SpuAttrValueEntity> querySearchAttrValuesBySpuId(Long spuId);
}
