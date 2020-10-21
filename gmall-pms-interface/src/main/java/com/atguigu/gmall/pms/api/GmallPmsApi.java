package com.atguigu.gmall.pms.api;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.GroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface GmallPmsApi {

    // 分页查询spu
    @PostMapping("pms/spu/json")
    public ResponseVo<List<SpuEntity>> querySpuByPagJson(@RequestBody PageParamVo paramVo);

    // 根据spuId查询spu下的所有sku
    @GetMapping("pms/sku/spu/{spuId}")
    public ResponseVo<List<SkuEntity>> querySkusBySpuId(@PathVariable("spuId")Long spuId);

    // 根据品牌的id查询品牌
    @GetMapping("pms/brand/{id}")
    public ResponseVo<BrandEntity> queryBrandById(@PathVariable("id") Long id);

    // 根据分类的id查询分类
    @GetMapping("pms/category/{id}")
    public ResponseVo<CategoryEntity> queryCategoryById(@PathVariable("id") Long id);

    // 根据skuId查询销售类型并且是搜索类型的规格参数及值
    @GetMapping("pms/skuattrvalue/sku/{skuId}")
    public ResponseVo<List<SkuAttrValueEntity>> querySearchAttrValuesBySkuId(@PathVariable("skuId")Long skuId);

    // 根据spuId查询基本类型并且是搜索类型的规格参数及值
    @GetMapping("pms/spuattrvalue/spu/{spuId}")
    public ResponseVo<List<SpuAttrValueEntity>> querySearchAttrValuesBySpuId(@PathVariable("spuId")Long spuId);

    // 根据spuId获取spu详细信息
    @GetMapping("pms/spu/{id}")
    public ResponseVo<SpuEntity> querySpuById(@PathVariable("id") Long id);

    // 根据父id查询分类
    @GetMapping("pms/category/parent/{parentId}")
    public ResponseVo<List<CategoryEntity>> queryCategoriesByPid(@PathVariable("parentId")Long parentId);

    // 根据一级分类查询二三级分类
    @GetMapping("pms/category/subs/{pid}")
    public ResponseVo<List<CategoryEntity>> queryCategoriesWithSubByPid(@PathVariable("pid")Long pid);

    // 详情查询
    @GetMapping("pms/sku/{id}")
    public ResponseVo<SkuEntity> querySkuById(@PathVariable("id") Long id);

    // 根据cid3查询一二三级分类集合
    @GetMapping("pms/category/all/{cid}")
    public ResponseVo<List<CategoryEntity>> queryAllCategoriesByCid3(@PathVariable("cid")Long cid);

    // 根据skuId查询sku的图片列表
    @GetMapping("pms/skuimages/sku/{skuId}")
    public ResponseVo<List<SkuImagesEntity>> queryImagesBySkuId(@PathVariable("skuId")Long skuId);

    // 根据spuId查询spu下所有sku的销售属性组合
    @GetMapping("pms/skuattrvalue/spu/{spuId}")
    public ResponseVo<List<SaleAttrValueVo>> querySaleAttrValueVoBySpuId(@PathVariable("spuId")Long spuId);

    // 根据spuId查询spu下所有销售属性组合和skuId的映射关系
    @GetMapping("pms/skuattrvalue/spu/sku/{spuId}")
    public ResponseVo<String> querySaleAttrValuesMappingSkuIdBySpuId(@PathVariable("spuId")Long spuId);

    // 根据spuId查询spu的海报信息列表
    @GetMapping("pms/spudesc/{spuId}")
    public ResponseVo<SpuDescEntity> querySpuDescById(@PathVariable("spuId") Long spuId);

    // 根据cid3、spuId、skuId查询分组及组下的规格参数以及值
    @GetMapping("pms/attrgroup/cid/{cid}")
    public ResponseVo<List<GroupVo>> queryGroupVoByCidAndSpuIdAndSkuId(
            @PathVariable("cid")Long cid,
            @RequestParam("spuId")Long spuId,
            @RequestParam("skuId")Long skuId);

    // 根据skuId查询当前sku的销售属性
    @GetMapping("pms/skuattrvalue/sku/sale/{skuId}")
    public ResponseVo<List<SkuAttrValueEntity>> querySaleAttrValuesBySkuId(@PathVariable("skuId")Long skuId);
}
