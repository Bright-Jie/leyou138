package com.leyou.item.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.leyou.common.exception.pojo.ExceptionEnum;
import com.leyou.common.exception.pojo.LyException;
import com.leyou.common.pojo.PageResult;
import com.leyou.item.entity.Brand;
import com.leyou.item.mapper.BrandMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class BrandService extends ServiceImpl<BrandMapper, Brand> {
    @Autowired
    private BrandMapper brandMapper;

    public PageResult<Brand> brandPageQuery(String key, Integer page, Integer rows, String sortBy, Boolean desc) {
        //定义一个plus的分页对象
        IPage<Brand> iPage = new Page<>(page, rows);
        //创建一个封装条件的对象
        QueryWrapper<Brand> wrapper = Wrappers.query();
        //判断是否有查询条件
        if(StringUtils.isNotBlank(key)){
            wrapper.eq("id", key)
                    .or()
                    .like("name", key)
                    .or()
                    .eq("letter", key.toUpperCase());
        }
        //判断是否有排序字段
        if(StringUtils.isNotBlank(sortBy) && desc!=null){
            if(desc){
                wrapper.orderByDesc(sortBy);
            }else {
                wrapper.orderByAsc(sortBy);
            }
        }
        //分页查询
        iPage = brandMapper.selectPage(iPage, wrapper);
        //创建一个自定义的分页对象
        PageResult<Brand> pageResult = new PageResult<>(
                iPage.getTotal(),
                iPage.getPages(),
                iPage.getRecords()
        );
        return pageResult;
    }

    public void saveBrand(Brand brand, List<Long> cids) {
        try {
            //添加品牌
            brandMapper.insert(brand);
            //获取品牌id
            Long brandId = brand.getId();
            //维护中间表
            brandMapper.saveBrandAndCategory(brandId, cids);
        }catch (Exception e){
            throw new LyException(ExceptionEnum.INSERT_OPERATION_FAIL);
        }

    }

    public Brand findBrandById(Long id) {
        Brand brand = brandMapper.selectById(id);
        if(brand==null){
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        return brand;
    }

    public List<Brand> findBrandsByIds(List<Long> ids) {
        List<Brand> brands = brandMapper.selectBatchIds(ids);
        if(CollectionUtils.isEmpty(brands)){
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        return brands;
    }

}