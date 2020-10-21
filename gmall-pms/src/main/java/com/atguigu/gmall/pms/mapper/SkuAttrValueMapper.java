package com.atguigu.gmall.pms.mapper;

import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * sku销售属性&值
 * 
 * @author Yan
 * @email 18112918867@163.com
 * @date 2020-08-21 13:53:03
 */
@Mapper
public interface SkuAttrValueMapper extends BaseMapper<SkuAttrValueEntity> {

    List<SkuAttrValueEntity> querySearchAttrValuesBySkuId(Long skuId);

    List<Map<String, Object>> querySaleAttrValuesMappingSkuIdBySpuId(Long spuId);
}
