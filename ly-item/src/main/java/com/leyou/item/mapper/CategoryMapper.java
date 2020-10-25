package com.leyou.item.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leyou.item.entity.Brand;
import com.leyou.item.entity.Category;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface CategoryMapper extends BaseMapper<Category> {

    @Select("SELECT * FROM tb_brand b, tb_category_brand cb " +
            "WHERE b.id = cb.brand_id AND cb.category_id = #{随便写}")
    public List<Brand> findCategoriesByBid(Long id);
}
