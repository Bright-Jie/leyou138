package com.leyou.item.client;

import com.leyou.common.pojo.PageResult;
import com.leyou.item.dto.SpecGroupDTO;
import com.leyou.item.dto.SpuDTO;
import com.leyou.item.entity.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient("item-service")
public interface ItemClient {

    /**
     * 分页查询商品列表
     */
    @GetMapping("/spu/page")
    public PageResult<SpuDTO> spuPageQuery(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                           @RequestParam(value = "rows", defaultValue = "5") Integer rows,
                                           @RequestParam(value = "key", required = false) String key,
                                           @RequestParam(value = "saleable", required = false) Boolean saleable);

    /**
     * 查询规格参数
     */
    @GetMapping("/spec/params")
    public List<SpecParam> fondParams(@RequestParam(value = "gid", required = false) Long gid,
                                      @RequestParam(value = "cid", required = false) Long cid,
                                      @RequestParam(value = "searching", required = false) Boolean searching);

    /**
     * 根据SpuId查询SpuDetail对象
     */
    @GetMapping("/spu/detail")
    public SpuDetail findSpuDetailById(@RequestParam("id") Long id);

    /**
     * 根据SpuId查询Sku集合
     */
    @GetMapping("/sku/of/spu")
    public List<Sku> findSkusBySpuId(@RequestParam("id") Long id);

    /**
     * 根据分类id集合查询分类对象集合
     */
    @GetMapping("/category/list")
    public List<Category> findCategoriesByIds(@RequestParam("ids") List<Long> ids);

    /**
     * 根据品牌id集合查询品牌对象集合
     */
    @GetMapping("/brand/list")
    public List<Brand> findBrandsByIds(@RequestParam("ids") List<Long> ids);

    /**
     * 根据spuId查询SpuDTO对象
     */
    @GetMapping("/spu/{id}")
    public SpuDTO findSpuDTOById(@PathVariable("id") Long id);

    /**
     * 根据品牌id查询品牌对象
     */
    @GetMapping("/brand/{id}")
    public Brand findBrandById(@PathVariable("id") Long id);

    /**
     * 根据分类id查询规格组集合及其规格组对象内规格参数集合
     */
    @GetMapping("/spec/of/category")
    public List<SpecGroupDTO> findSpecGroupDTOSByCid(@RequestParam("id") Long id);

}
