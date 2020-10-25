package com.leyou.page.service;

import com.leyou.common.exception.pojo.ExceptionEnum;
import com.leyou.common.exception.pojo.LyException;
import com.leyou.item.client.ItemClient;
import com.leyou.item.dto.SpecGroupDTO;
import com.leyou.item.dto.SpuDTO;
import com.leyou.item.entity.Brand;
import com.leyou.item.entity.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PageService {

    @Value("${ly.static.itemDir}")
    private String itemDir;

    @Value("${ly.static.itemTemplate}")
    private String itemTemplate;

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Autowired
    private ItemClient itemClient;

    public Map<String, Object> loadItemData(Long spuId) {
        //根据spuId查询SpuDTO对象
        SpuDTO spuDTO = itemClient.findSpuDTOById(spuId);
        //根据三个分类id的集合查询分类对象的集合
        List<Category> categories = itemClient.findCategoriesByIds(spuDTO.getCategoryIds());
        //根据品牌id查询品牌对象
        Brand brand = itemClient.findBrandById(spuDTO.getBrandId());
        //根据cid3查询规格组集合且每个规格组对象中要包含当前规格组对应的规格参数集合
        List<SpecGroupDTO> groupDTOS = itemClient.findSpecGroupDTOSByCid(spuDTO.getCid3());

        Map<String, Object> itemData = new HashMap<>();
        itemData.put("categories", categories);
        itemData.put("brand", brand);
        itemData.put("spuName", spuDTO.getName());
        itemData.put("subTitle", spuDTO.getSubTitle()); 
        itemData.put("detail", spuDTO.getSpuDetail());
        itemData.put("skus", spuDTO.getSkus());
        itemData.put("specs", groupDTOS);
        return itemData;
    }

    /**
     * 生成静态化页面的业务方法
     */
    public void createStaticItemPage(Long spuId){
        //创建上下文
        Context context = new Context();
        context.setVariables(loadItemData(spuId));
        //创建一个静态化页面服务器文件对象
        File serverFile = new File(itemDir);
        //指定静态化页面的名称
        String pageName = spuId+".html";
        //创建一个静态化页面的文件对象
        File pageFile = new File(serverFile, pageName);
        //创建打印流
        try(PrintWriter write = new PrintWriter(pageFile)) {
            //生成静态化页面并写入到指定的服务文件中
            templateEngine.process(itemTemplate, context, write);
        } catch (Exception e) {
            throw new LyException(ExceptionEnum.FILE_WRITER_ERROR);
        }
    }

    public void addPage(Long id) {
        createStaticItemPage(id);
    }

    public void delPage(Long id) {
        //创建出要删除的文件
        //创建一个静态化页面服务器文件对象
        File serverFile = new File(itemDir);
        //指定静态化页面的名称
        String pageName = id+".html";
        //创建一个静态化页面的文件对象
        File pageFile = new File(serverFile, pageName);
        //判断文件是否存在
        if(pageFile.exists()){
            pageFile.delete();
        }
    }
}
