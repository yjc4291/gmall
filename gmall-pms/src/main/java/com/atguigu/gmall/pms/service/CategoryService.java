package com.atguigu.gmall.pms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.pms.entity.CategoryEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品三级分类
 *
 * @author Yan
 * @email 18112918867@163.com
 * @date 2020-08-21 13:53:03
 */
public interface CategoryService extends IService<CategoryEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    List<CategoryEntity> queryCategory(Long parentId);

    List<CategoryEntity> queryCategoriesWithSubByPid(Long pid);

    List<CategoryEntity> queryAllCategoriesByCid3(Long cid);
}

