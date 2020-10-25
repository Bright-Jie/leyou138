package com.leyou.item.controller;

import com.leyou.common.pojo.PageResult;
import com.leyou.item.entity.Brand;
import com.leyou.item.service.BrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class BrandController {

    @Autowired
    private BrandService brandService;

    /**
     * 品牌分页查询
     */
    @GetMapping("/brand/page")
    public ResponseEntity<PageResult<Brand>> brandPageQuery(@RequestParam(value = "key", required = false) String key,
                                                            @RequestParam(value = "page", defaultValue = "1") Integer page,
                                                            @RequestParam(value = "rows", defaultValue = "5") Integer rows,
                                                            @RequestParam(value = "sortBy", required = false) String sortBy,
                                                            @RequestParam(value = "desc", required = false) Boolean desc){
        PageResult<Brand> pageResult = brandService.brandPageQuery(key, page, rows, sortBy, desc);
        return ResponseEntity.ok(pageResult);
    }

    /**
     * 添加品牌
     */
    @PostMapping("/brand")
    public ResponseEntity<Void> saveBrand(Brand brand, @RequestParam("cids") List<Long> cids){
        brandService.saveBrand(brand, cids);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 根据品牌id查询品牌对象
     */
    @GetMapping("/brand/{id}")
    public ResponseEntity<Brand> findBrandById(@PathVariable("id") Long id){
        Brand brand = brandService.findBrandById(id);
        return ResponseEntity.ok(brand);
    }

    /**
     * 根据品牌id集合查询品牌对象集合
     */
    @GetMapping("/brand/list")
    public ResponseEntity<List<Brand>> findBrandsByIds(@RequestParam("ids") List<Long> ids){
        List<Brand> brands = brandService.findBrandsByIds(ids);
        return ResponseEntity.ok(brands);
    }


}