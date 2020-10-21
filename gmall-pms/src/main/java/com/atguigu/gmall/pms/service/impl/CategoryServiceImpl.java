package com.atguigu.gmall.pms.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.CategoryMapper;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.service.CategoryService;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, CategoryEntity> implements CategoryService {
    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<CategoryEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public List<CategoryEntity> queryCategory(Long parentId) {

        // 构造查询条件
        QueryWrapper<CategoryEntity> queryWrapper = new QueryWrapper<>();
        // 如果parentId为-1，说明用户没有传该字段，查询所有
        if(parentId != -1){
            queryWrapper.eq("parent_id",parentId);
        }

        return categoryMapper.selectList(queryWrapper);
    }

    @Override
    public List<CategoryEntity> queryCategoriesWithSubByPid(Long pid) {
        return categoryMapper.queryCategoriesWithSubByPid(pid);
    }

    // 根据cid3查询一二三级分类集合
    @Override
    public List<CategoryEntity> queryAllCategoriesByCid3(Long cid) {
        // 查询三级分类
        CategoryEntity lvl3Cat = this.getById(cid);

        if(lvl3Cat != null){
            // 查询二级分类
            CategoryEntity lvl2Cat = this.getById(lvl3Cat.getParentId());

            // 查询一级分类
            CategoryEntity lvl1Cat = this.getById(lvl2Cat.getParentId());

            return Arrays.asList(lvl1Cat, lvl2Cat, lvl3Cat);
        }

        return null;
    }

}