package com.atguigu.gmall.pms.controller;

import java.util.List;

import com.atguigu.gmall.pms.vo.GroupVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sun.org.apache.regexp.internal.RE;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.atguigu.gmall.pms.service.AttrGroupService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.bean.PageParamVo;

/**
 * 属性分组
 *
 * @author Yan
 * @email 18112918867@163.com
 * @date 2020-08-21 13:53:03
 */
@Api(tags = "属性分组 管理")
@RestController
@RequestMapping("pms/attrgroup")
public class AttrGroupController {

    @Autowired
    private AttrGroupService attrGroupService;

    @ApiOperation("根据cid3、spuId、skuId查询分组及组下的规格参数以及值")
    @GetMapping("cid/{cid}")
    public ResponseVo<List<GroupVo>> queryGroupVoByCidAndSpuIdAndSkuId(
            @PathVariable("cid")Long cid,
            @RequestParam("spuId")Long spuId,
            @RequestParam("skuId")Long skuId
    ){
        List<GroupVo> groupVos = this.attrGroupService.queryGroupVoByCidAndSpuIdAndSkuId(cid,spuId,skuId);
        return ResponseVo.ok(groupVos);
    }

    @GetMapping("withattrs/{catId}")
    public ResponseVo<List<AttrGroupEntity>> queryGroupWithAttrsByCid(@PathVariable("catId")Long cid){
        List<AttrGroupEntity> attrGroupEntities = this.attrGroupService.queryGroupWithAttrsByCid(cid);
        return ResponseVo.ok(attrGroupEntities);
    }

    @ApiOperation("根据三级分类id查询")
    @GetMapping("category/{cid}")
    public ResponseVo<List<AttrGroupEntity>> queryByCidPage(@PathVariable("cid")Long cid){

        List<AttrGroupEntity> groupEntities = this.attrGroupService.list(new QueryWrapper<AttrGroupEntity>().eq("category_id", cid));
        return ResponseVo.ok(groupEntities);
    }

    /**
     * 列表
     */
    @GetMapping
    @ApiOperation("分页查询")
    public ResponseVo<PageResultVo> queryAttrGroupByPage(PageParamVo paramVo){
        PageResultVo pageResultVo = attrGroupService.queryPage(paramVo);

        return ResponseVo.ok(pageResultVo);
    }


    /**
     * 信息
     */
    @GetMapping("{id}")
    @ApiOperation("详情查询")
    public ResponseVo<AttrGroupEntity> queryAttrGroupById(@PathVariable("id") Long id){
		AttrGroupEntity attrGroup = attrGroupService.getById(id);

        return ResponseVo.ok(attrGroup);
    }

    /**
     * 保存
     */
    @PostMapping
    @ApiOperation("保存")
    public ResponseVo<Object> save(@RequestBody AttrGroupEntity attrGroup){
		attrGroupService.save(attrGroup);

        return ResponseVo.ok();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    @ApiOperation("修改")
    public ResponseVo update(@RequestBody AttrGroupEntity attrGroup){
		attrGroupService.updateById(attrGroup);

        return ResponseVo.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @ApiOperation("删除")
    public ResponseVo delete(@RequestBody List<Long> ids){
		attrGroupService.removeByIds(ids);

        return ResponseVo.ok();
    }

}
