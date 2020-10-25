package com.leyou.item.controller;

import com.leyou.item.entity.Brand;
import com.leyou.item.entity.Category;
import com.leyou.item.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

//@CrossOrigin({"http://manage.leyou.com", "*"})
@RestController
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 根据父id查询分类列表
     */
    @GetMapping("/category/of/parent")
    public ResponseEntity<List<Category>> findCategoriesByPid(@RequestParam("pid") Long pid){
        List<Category> categoryList = categoryService.findCategoriesByPid(pid);
        //return ResponseEntity.status(HttpStatus.OK).body(categoryList);
        return ResponseEntity.ok(categoryList);
    }

    /**
     * 根据分类id集合查询分类对象集合
     */
    @GetMapping("/category/list")
    public ResponseEntity<List<Category>> findCategoriesByIds(@RequestParam("ids") List<Long> ids){
        List<Category> categoryList = categoryService.findCategoriesByIds(ids);
        return ResponseEntity.ok(categoryList);
    }

    /**
     * 根据分类id查询品牌集合
     */
    @GetMapping("/brand/of/category")
    public ResponseEntity<List<Brand>> findCategoriesByBid(@RequestParam("id") Long id){
        List<Brand> brands = categoryService.findCategoriesByBid(id);
        return ResponseEntity.ok(brands);
    }
}
