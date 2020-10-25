package com.leyou.item.controller;

import com.leyou.common.pojo.PageResult;
import com.leyou.item.dto.SpuDTO;
import com.leyou.item.entity.Sku;
import com.leyou.item.entity.SpuDetail;
import com.leyou.item.service.GoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class GoodsController {

    @Autowired
    private GoodsService goodsService;

    /**
     * 分页查询商品列表
     */
    @GetMapping("/spu/page")
    public ResponseEntity<PageResult<SpuDTO>> spuPageQuery(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                                           @RequestParam(value = "rows", defaultValue = "5") Integer rows,
                                                           @RequestParam(value = "key", required = false) String key,
                                                           @RequestParam(value = "saleable", required = false) Boolean saleable){
        PageResult<SpuDTO> pageResult = goodsService.spuPageQuery(page, rows, key, saleable);
        return ResponseEntity.ok(pageResult);
    }

    /**
     * 添加商品
     */
    @PostMapping("/goods")
    public ResponseEntity<Void> saveGoods(@RequestBody SpuDTO spuDTO){
        goodsService.saveGoods(spuDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 商品上下架
     */
    @PutMapping("/spu/saleable")
    public ResponseEntity<Void> updateSaleable(@RequestParam("id") Long id,
                                               @RequestParam("saleable") Boolean saleable){
        goodsService.updateSaleable(id, saleable);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * 根据SpuId查询SpuDetail对象
     */
    @GetMapping("/spu/detail")
    public ResponseEntity<SpuDetail> findSpuDetailById(@RequestParam("id") Long id){
        SpuDetail spuDetail = goodsService.findSpuDetailById(id);
        return ResponseEntity.ok(spuDetail);
    }

    /**
     * 根据SpuId查询Sku集合
     */
    @GetMapping("/sku/of/spu")
    public ResponseEntity<List<Sku>> findSkusBySpuId(@RequestParam("id") Long id){
        List<Sku> skus = goodsService.findSkusBySpuId(id);
        return ResponseEntity.ok(skus);
    }

    /**
     * 根据spuId查询SpuDTO对象
     */
    @GetMapping("/spu/{id}")
    public ResponseEntity<SpuDTO> findSpuDTOById(@PathVariable("id") Long id){
        SpuDTO spuDTO = goodsService.findSpuDTOById(id);
        return ResponseEntity.ok(spuDTO);
    }

}