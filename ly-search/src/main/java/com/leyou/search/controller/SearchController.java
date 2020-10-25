package com.leyou.search.controller;

import com.leyou.search.dto.GoodsDTO;
import com.leyou.search.dto.SearchRequest;
import com.leyou.search.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class SearchController {

    @Autowired
    private SearchService searchService;

    /**
     * 获取渲染搜索页面的数据
     */
    @PostMapping("/load/search/page")
    public ResponseEntity<Map<String, Object>> loadSearchPageData(@RequestBody SearchRequest request){
        Map<String, Object> resultData = searchService.loadSearchPageData(request);
        return ResponseEntity.ok(resultData);
    }

    /**
     * 搜索页切换页码
     */
    @PostMapping("/page/change")
    public ResponseEntity<List<GoodsDTO>> pageChange(@RequestBody SearchRequest request){
        List<GoodsDTO> resultData = searchService.pageChange(request);
        return ResponseEntity.ok(resultData);
    }
}
