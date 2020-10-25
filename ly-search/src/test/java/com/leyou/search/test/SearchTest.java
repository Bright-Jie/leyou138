package com.leyou.search.test;

import com.leyou.common.pojo.PageResult;
import com.leyou.item.client.ItemClient;
import com.leyou.item.dto.SpuDTO;
import com.leyou.item.entity.SpecParam;
import com.leyou.search.entity.Goods;
import com.leyou.search.repository.SearchRepository;
import com.leyou.search.service.SearchService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SearchTest {

    @Autowired
    private ItemClient itemClient;

    @Autowired
    private SearchRepository searchRepository;

    @Autowired
    private SearchService searchService;

    /**
     * 将数据库的数据写入索引库
     */
    @Test
    public void buildGoods() {
        Integer page = 1, rows=100;
        Long totalPage = 1L;
        do{
            //查询出所有的spuDTO
            PageResult<SpuDTO> pageResult = itemClient.spuPageQuery(page, rows, null, true);
            //获取总SpuDTO列表
            List<SpuDTO> spuDTOS = pageResult.getItems();
            //将SpuDTO集合转成Goods集合
            List<Goods> goodsList = spuDTOS.stream().map(searchService::buildGoods).collect(Collectors.toList());
            searchRepository.saveAll(goodsList);

            //获取总页数
            totalPage = pageResult.getTotalPage();
            page++;
        }while (page<=totalPage);


    }

    @Test
    public void fondParams(){
        List<SpecParam> specParams = itemClient.fondParams(null, 76L, true);
        System.out.println(specParams);
    }



}
