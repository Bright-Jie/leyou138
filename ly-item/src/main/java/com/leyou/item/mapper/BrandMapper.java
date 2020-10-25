package com.leyou.item.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leyou.item.entity.Brand;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BrandMapper extends BaseMapper<Brand> {

    public void saveBrandAndCategory(@Param("bid") Long brandId, @Param("cids") List<Long> cids);

}