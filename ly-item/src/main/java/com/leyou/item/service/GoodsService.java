package com.leyou.item.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.leyou.common.constants.MQConstants;
import com.leyou.common.exception.pojo.ExceptionEnum;
import com.leyou.common.exception.pojo.LyException;
import com.leyou.common.pojo.PageResult;
import com.leyou.common.utils.BeanHelper;
import com.leyou.item.dto.SpuDTO;
import com.leyou.item.entity.Category;
import com.leyou.item.entity.Sku;
import com.leyou.item.entity.Spu;
import com.leyou.item.entity.SpuDetail;
import com.leyou.item.mapper.SkuMapper;
import com.leyou.item.mapper.SpuDetailMapper;
import com.leyou.item.mapper.SpuMapper;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class GoodsService extends ServiceImpl<SkuMapper, Sku> {

    @Autowired
    private SpuMapper spuMapper;

    @Autowired
    private SpuDetailMapper detailMapper;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private BrandService brandService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private AmqpTemplate amqpTemplate;

    public PageResult<SpuDTO> spuPageQuery(Integer page, Integer rows, String key, Boolean saleable) {
        //封装mybatis-plus的分页对象
        IPage<Spu> iPage = new Page<>(page, rows);
        //创建一个封装复杂条件的对象
        QueryWrapper<Spu> wrapper = Wrappers.query();
        //判断是否有查询条件
        if(StringUtils.isNotEmpty(key)){
            wrapper.like("name", key);
        }
        //判断是否有上下架条件
        if(saleable!=null){
            wrapper.eq("saleable", saleable);
        }
        //spu分页查询
        iPage = spuMapper.selectPage(iPage, wrapper);
        //获取spu列表
        List<Spu> records = iPage.getRecords();
        if(CollectionUtils.isEmpty(records)){
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        //将Spu集合转成SpuDTO集合
        List<SpuDTO> spuDTOS = BeanHelper.copyWithCollection(records, SpuDTO.class);
        //给SpuDTO集合中每个SpuDTO对象中的品牌名称和分类名称赋值
        handlerCategoriesNamesAndBrandName(spuDTOS);
        //封装自定义的分页对象
        PageResult<SpuDTO> pageResult = new PageResult<>(
                iPage.getTotal(),
                iPage.getPages(),
                spuDTOS
        );
        return pageResult;
    }

    //给SpuDTO集合中每个SpuDTO对象中的品牌名称和分类名称赋值
    private void handlerCategoriesNamesAndBrandName(List<SpuDTO> spuDTOS) {
        //遍历
        spuDTOS.forEach(spuDTO -> {
            //根据品牌id查询品牌名称
            String brandName = brandService.findBrandById(spuDTO.getBrandId()).getName();
            //给品牌名称赋值
            spuDTO.setBrandName(brandName);

            //根据分类id的集合获取到分类名称的字符串拼接
            String categoriesNames = categoryService.findCategoriesByIds(spuDTO.getCategoryIds())//Category集合
                    .stream()
                    .map(Category::getName)//收集每个Category对象中的name值
                    .collect(Collectors.joining("|"));//将所有Category对象中的name值收集为字符串
            //给分类名称赋值
            spuDTO.setCategoryName(categoriesNames);
        });
    }

    public void saveGoods(SpuDTO spuDTO) {
        try {
            //得到spu对象
            Spu spu = BeanHelper.copyProperties(spuDTO, Spu.class);
            //设置所有新添加的商品为下架
            spu.setSaleable(false);
            //添加spu
            spuMapper.insert(spu);

            //得到spu的id
            Long spuId = spu.getId();

            //获取SpuDetail对象
            SpuDetail spuDetail = spuDTO.getSpuDetail();
            //给主键赋值
            spuDetail.setSpuId(spuId);
            //保存SpuDetail对象
            detailMapper.insert(spuDetail);

            //获取sku集合
            List<Sku> skus = spuDTO.getSkus();
            //给每个Sku中的spuId赋值
            skus.forEach(sku -> {
                sku.setSpuId(spuId);
            });
            //批量添加sku对象
            saveBatch(skus);
        }catch (Exception e){
            throw new LyException(ExceptionEnum.INSERT_OPERATION_FAIL);
        }
    }

    /**
     * 商品上下架
     * @param id
     * @param saleable
     */
    public void updateSaleable(Long id, Boolean saleable) {
        Spu entity = new Spu();
        entity.setId(id);
        entity.setSaleable(saleable);
        int count = spuMapper.updateById(entity);
        if(count==0){
            throw new LyException(ExceptionEnum.UPDATE_OPERATION_FAIL);
        }

        //数据同步
        //同步：feign调用搜索服务和静态页服务进行数据同步。
        //指定routingKey
        String routingKey = saleable ? MQConstants.RoutingKey.ITEM_UP_KEY : MQConstants.RoutingKey.ITEM_DOWN_KEY;
        //异步：把数据同步相关数据放入MQ队列中。
        amqpTemplate.convertAndSend(MQConstants.Exchange.ITEM_EXCHANGE_NAME, routingKey, id);
    }

    public SpuDetail findSpuDetailById(Long id) {
        SpuDetail spuDetail = detailMapper.selectById(id);
        if(spuDetail==null){
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        return spuDetail;
    }

    public List<Sku> findSkusBySpuId(Long id) {
        Sku entity = new Sku();
        entity.setSpuId(id);
        QueryWrapper<Sku> wrapper = Wrappers.query(entity);
        List<Sku> skus = skuMapper.selectList(wrapper);
        if(skus==null){
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        return skus;
    }

    public SpuDTO findSpuDTOById(Long id) {
        //根据id查询Spu对象
        Spu spu = spuMapper.selectById(id);
        //判空
        if(spu==null){
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        //将Spu对象转成SpuDTO对象
        SpuDTO spuDTO = BeanHelper.copyProperties(spu, SpuDTO.class);
        //根据id查询SpuDetail
        SpuDetail spuDetail = findSpuDetailById(id);
        //把SpuDetail赋值给spuDto
        spuDTO.setSpuDetail(spuDetail);
        //根据spuId查询sku集合
        List<Sku> skus = findSkusBySpuId(id);
        //把Sku集合赋值给SpuDTO
        spuDTO.setSkus(skus);
        return spuDTO;
    }

}
