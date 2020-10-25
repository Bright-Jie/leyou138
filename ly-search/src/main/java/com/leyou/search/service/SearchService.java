package com.leyou.search.service;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.leyou.common.exception.pojo.ExceptionEnum;
import com.leyou.common.exception.pojo.LyException;
import com.leyou.common.pojo.PageResult;
import com.leyou.common.utils.BeanHelper;
import com.leyou.common.utils.JsonUtils;
import com.leyou.item.client.ItemClient;
import com.leyou.item.dto.SpuDTO;
import com.leyou.item.entity.*;
import com.leyou.search.dto.GoodsDTO;
import com.leyou.search.dto.SearchRequest;
import com.leyou.search.entity.Goods;
import com.leyou.search.repository.SearchRepository;
import com.leyou.search.utils.HighlightUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class SearchService {

    @Autowired
    private SearchRepository searchRepository;

    @Autowired
    private ElasticsearchTemplate esTemplate;

    @Autowired
    private ItemClient itemClient;

    /**
     * 创建Goods对象
     */
    public Goods buildGoods(SpuDTO spuDTO){
        //根据spuId查询Sku集合
        List<Sku> skus = itemClient.findSkusBySpuId(spuDTO.getId());
        List<Map<String, Object>> skuList = new ArrayList<>();
        skus.forEach(sku -> {
            Map<String, Object> skuMap = new HashMap<>();
            skuMap.put("id", sku.getId());
            skuMap.put("image", StringUtils.substringBefore(sku.getImages(), ","));
            skuMap.put("price", sku.getPrice());
            skuMap.put("title", sku.getTitle().substring(spuDTO.getName().length()));
            skuList.add(skuMap);
        });
        String all = spuDTO.getBrandName()
                + spuDTO.getCategoryName()
                + skus.stream().map(Sku::getTitle).collect(Collectors.joining());

        Set<Long> prices = skus.stream().map(Sku::getPrice).collect(Collectors.toSet());

        //给动态过滤条件赋值
        //创建一个存储动态过滤条件的map
        Map<String, Object> specs = new HashMap<>();
        //根据cid3查询参与搜索的规格参数
        List<SpecParam> specParams = itemClient.fondParams(null, spuDTO.getCid3(), true);
        //根据SpuId查询SpuDetail对象
        SpuDetail spuDetail = itemClient.findSpuDetailById(spuDTO.getId());
        //获取通用规格参数json字符串
        String genericSpecStr = spuDetail.getGenericSpec();
        //将取通用规格参数json字符串转成Map
        Map<Long, Object> genericSpecObj = JsonUtils.toMap(genericSpecStr, Long.class, Object.class);
        //获取特有规格参数json字符串
        String specialSpecStr = spuDetail.getSpecialSpec();
        //将特有规格参数json字符串转成Map
        Map<Long, List<Object>> specialSpecObj = JsonUtils.nativeRead(specialSpecStr, new TypeReference<Map<Long, List<Object>>>() {});
        //遍历动态过滤条件的key所在的对象集合specParams
        specParams.forEach(specParam -> {
            //获取key值
            String key = specParam.getName();
            //获取value值
            Object value = null;
            //判断value值的来源
            if(specParam.getGeneric()){
                value = genericSpecObj.get(specParam.getId());
            }else {
                value = specialSpecObj.get(specParam.getId());
            }
            if(specParam.getNumeric()){
                //对所有数字类型的过滤条件转成区间
                value = chooseSegment(value, specParam);
            }
            specs.put(key, value);
        });

        //创建Goods
        Goods goods = new Goods();
        goods.setId(spuDTO.getId());
        goods.setSpuName(spuDTO.getName());
        goods.setSubTitle(spuDTO.getSubTitle());
        goods.setBrandId(spuDTO.getBrandId());
        goods.setCategoryId(spuDTO.getCid3());
        goods.setCreateTime(spuDTO.getCreateTime().getTime());
        goods.setSkus(JsonUtils.toString(skuList));//{"":""}
        goods.setAll(all);
        goods.setPrice(prices);
        goods.setSpecs(specs);
        return goods;
    }

    private String chooseSegment(Object value, SpecParam p) {
        if (value == null || StringUtils.isBlank(value.toString())) {
            return "其它";
        }
        double val = parseDouble(value.toString());
        String result = "其它";
        // 保存数值段
        for (String segment : p.getSegments().split(",")) {
            String[] segs = segment.split("-");
            // 获取数值范围
            double begin = parseDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if (segs.length == 2) {
                end = parseDouble(segs[1]);
            }
            // 判断是否在范围内
            if (val >= begin && val < end) {
                if (segs.length == 1) {
                    result = segs[0] + p.getUnit() + "以上";
                } else if (begin == 0) {
                    result = segs[1] + p.getUnit() + "以下";
                } else {
                    result = segment + p.getUnit();
                }
                break;
            }
        }
        return result;
    }

    private double parseDouble(String str) {
        try {
            return Double.parseDouble(str);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 获取渲染搜索页面的数据
     * @param request
     * @return
     */
    public Map<String, Object> loadSearchPageData(SearchRequest request) {
        //索引库分页查询
        PageResult<GoodsDTO> pageResult = itemPageQuery(request);
        //索引库聚合查询
        Map<String, List<?>> filterConditionsMap = aggQuery(request);
        //封装一个返回值对象
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("filterConditions", filterConditionsMap);
        resultMap.put("itemsList", pageResult.getItems());
        resultMap.put("totalCount", pageResult.getTotal());
        resultMap.put("totalPages", pageResult.getTotalPage());
        return resultMap;
    }

    /**
     * -------------------------------------------------索引库聚合查询
     * @param request
     * @return
     */
    public Map<String, List<?>> aggQuery(SearchRequest request) {
        //创建一个结果map
        Map<String, List<?>> resultMap = new LinkedHashMap<>();
        //创建一个可以封装复杂条件的查询构建器
        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
        //封装分页条件  【可以不写】
        nativeSearchQueryBuilder.withPageable(PageRequest.of(0, 1));
        //封装要查询的字段   【可以不写】
        nativeSearchQueryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{""}, null));
        //封装搜索条件
        nativeSearchQueryBuilder.withQuery(createQueryBuilder(request));
        //指定分类聚合的名称
        String categoryAgg = "categoryAgg";
        //添加分类的聚合条件
        nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms(categoryAgg).field("categoryId"));
        //指定品牌聚合的名称
        String brandAgg = "brandAgg";
        //添加品牌的聚合条件
        nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms(brandAgg).field("brandId"));

        //聚合查询
        AggregatedPage<Goods> aggResult = esTemplate.queryForPage(nativeSearchQueryBuilder.build(), Goods.class);

        //获取所有聚合结果
        Aggregations aggregations = aggResult.getAggregations();
        //根据分类名称获取分类聚合结果
        Terms categoryTerms = aggregations.get(categoryAgg);
        //将分类的聚合结果封装到结果map中
        List<Long> cids = handlerCategoryTerms(resultMap, categoryTerms);
        //根据品牌名称获取品牌聚合结果
        Terms brandTerms = aggregations.get(brandAgg);
        //将品牌的聚合结果封装到结果map中
        handlerBrandTerms(resultMap, brandTerms);
        //将动态过滤条件封装到结果map中
        handlerActiveSpecParamTerms(resultMap, cids, createQueryBuilder(request));
        return resultMap;
    }

    /**
     * =========================================将动态过滤条件封装到结果map中
     * @param resultMap
     * @param cids
     * @param queryBuilder
     */
    private void handlerActiveSpecParamTerms(Map<String, List<?>> resultMap, List<Long> cids, QueryBuilder queryBuilder) {
        cids.forEach(cid->{
            //创建一个可以封装复杂条件的查询构建器
            NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
            //封装分页条件  【可以不写】
            nativeSearchQueryBuilder.withPageable(PageRequest.of(0, 1));
            //封装要查询的字段   【可以不写】
            nativeSearchQueryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{""}, null));
            //封装搜索条件
            nativeSearchQueryBuilder.withQuery(queryBuilder);

            //根据分类id查询参与搜索的规格参数
            List<SpecParam> specParams = itemClient.fondParams(null, cid, true);

            //遍历
            specParams.forEach(specParam -> {
                //指定规格参数聚合的名称
                String specParamAgg = specParam.getName();
                //得到当前规格参数的域名
                String field = "specs."+specParamAgg+".keyword";
                //添加规格参数的聚合条件
                nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms(specParamAgg).field(field));
            });

            //聚合查询
            AggregatedPage<Goods> aggResult = esTemplate.queryForPage(nativeSearchQueryBuilder.build(), Goods.class);

            //获取所有的聚合结果
            Aggregations aggregations = aggResult.getAggregations();
            //遍历规格参数对象集合，取出并封装每个桶信息
            specParams.forEach(specParam -> {
                //指定规格参数聚合的名称
                String specParamAgg = specParam.getName();
                //根据聚合名称获取到桶中的key的信息的集合
                Terms specParamTerms = aggregations.get(specParamAgg);
                List<String> aggList = specParamTerms.getBuckets()
                        .stream()
                        .map(Terms.Bucket::getKeyAsString)
                        .collect(Collectors.toList());
                //将聚合结果放入到结果map中
                resultMap.put(specParamAgg, aggList);
            });


        });
    }

    /**
     * ======================================================将品牌的聚合结果封装到结果map中
     * @param resultMap
     * @param brandTerms
     */
    private void handlerBrandTerms(Map<String, List<?>> resultMap, Terms brandTerms) {
        //获取所有分类的id的集合
        List<Long> bids = brandTerms.getBuckets()
                .stream()
                .map(Terms.Bucket::getKeyAsNumber)
                .map(Number::longValue)
                .collect(Collectors.toList());
        //根据品牌id集合查询品牌对象集合
        List<Brand> brands = itemClient.findBrandsByIds(bids);
        //将品牌对象集合放入结果map
        resultMap.put("品牌", brands);
    }

    /**
     * =================================================将分类的聚合结果封装到结果map中
     * @param resultMap
     * @param categoryTerms
     * @return
     */
    private List<Long> handlerCategoryTerms(Map<String, List<?>> resultMap, Terms categoryTerms) {
        //解析分类聚合的桶
        List<Long> cids = categoryTerms.getBuckets()
                .stream()
                .map(Terms.Bucket::getKeyAsNumber)
                .map(Number::longValue)
                .collect(Collectors.toList());
        //根据分类id集合查询分类对象集合
        List<Category> categories = itemClient.findCategoriesByIds(cids);
        //把分类对象放入结果map中
        resultMap.put("分类", categories);
        return cids;
    }

    /**
     * ---------------------------------------------------索引库分页查询
     * @param request
     * @return
     */
    public PageResult<GoodsDTO> itemPageQuery(SearchRequest request) {
        //创建一个可以封装复杂条件的查询构建器
        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
        //封装分页条件
        nativeSearchQueryBuilder.withPageable(PageRequest.of(request.getPage()-1, request.getSize()));
        //封装要查询的字段
        nativeSearchQueryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{"id", "spuName", "subTitle", "skus"}, null));
        //封装搜索条件
        nativeSearchQueryBuilder.withQuery(createQueryBuilder(request));
        //高亮字段设置
        HighlightUtils.highlightField(nativeSearchQueryBuilder, "spuName");
        //分页查询
        AggregatedPage<Goods> esPageResult = esTemplate.queryForPage(nativeSearchQueryBuilder.build(), Goods.class, HighlightUtils.highlightBody(Goods.class, "spuName"));
        //获取列表数据
        List<Goods> content = esPageResult.getContent();
        if(CollectionUtils.isEmpty(content)){
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        //创建一个自定义的分页对象
        PageResult<GoodsDTO> pageResult = new PageResult<>(
                esPageResult.getTotalElements(),
                Long.valueOf(esPageResult.getTotalPages()),
                BeanHelper.copyWithCollection(content, GoodsDTO.class)
        );
        return pageResult;
    }

    /**
     * 查询条件封装
     */
    private QueryBuilder createQueryBuilder(SearchRequest request) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        //添加搜索条件
        boolQuery.must(QueryBuilders.multiMatchQuery(request.getKey(), "spuName", "all").operator(Operator.AND));
        //如果有过滤条件，添加过滤条件
        if(CollectionUtils.isNotEmpty(request.getFilterParams())){
            Map<String, Object> filterParams = request.getFilterParams();
            //遍历添加过滤条件
            filterParams.entrySet().forEach(entry->{
                //获取key
                String key = entry.getKey();
                //对key值进行处理
                if(key.equals("分类")){
                    key = "categoryId";
                }else if(!key.equals("brandId")){
                    key = "specs."+key+".keyword";
                }
                //获取value
                Object value = entry.getValue();
                //添加过滤条件
                boolQuery.filter(QueryBuilders.termQuery(key, value));
            });
        }
        return boolQuery;
    }

    /**
     * 搜索页切换页码
     */
    public List<GoodsDTO> pageChange(SearchRequest request) {
        //索引库分页查询
        PageResult<GoodsDTO> pageResult = itemPageQuery(request);
        return pageResult.getItems();
    }

    public void addIndex(Long id) {
        SpuDTO spuDTO = itemClient.findSpuDTOById(id);
        Goods goods = buildGoods(spuDTO);
        searchRepository.save(goods);
    }

    public void delIndex(Long id) {
        searchRepository.deleteById(id);
    }
}
