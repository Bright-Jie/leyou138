package com.leyou.item.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.leyou.common.exception.pojo.ExceptionEnum;
import com.leyou.common.exception.pojo.LyException;
import com.leyou.item.entity.Brand;
import com.leyou.item.entity.Category;
import com.leyou.item.mapper.CategoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoryService extends ServiceImpl<CategoryMapper, Category> {

    @Autowired
    private CategoryMapper categoryMapper;

    public List<Category> findCategoriesByPid(Long pid) {
        //封装条件 方式一
//        QueryWrapper<Category> wrapper = new QueryWrapper<>();
//        wrapper.eq("pid", pid);

        //封装条件 方式二 仅限于多个条件之间的关系必须是and关系，而且都必须是等值条件
        Category entity = new Category();
        entity.setParentId(pid);
        QueryWrapper<Category> wrapper = Wrappers.query(entity);

        //条件查询
        List<Category> categoryList = categoryMapper.selectList(wrapper);
        //判空
        if(CollectionUtils.isEmpty(categoryList)){
            throw new LyException(ExceptionEnum.CATEGORY_NOT_FOUND);
        }
        return categoryList;
    }

    public List<Category> findCategoriesByIds(List<Long> ids) {
        List<Category> categoryList = categoryMapper.selectBatchIds(ids);
        if(CollectionUtils.isEmpty(categoryList)){
            throw new LyException(ExceptionEnum.CATEGORY_NOT_FOUND);
        }
        return categoryList;
    }

    public List<Brand> findCategoriesByBid(Long id) {
        List<Brand> brands = categoryMapper.findCategoriesByBid(id);
        if(CollectionUtils.isEmpty(brands)){
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        return brands;
    }
}
