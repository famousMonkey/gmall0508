package com.atguigu.gmall0508.list.serviceimpl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall0508.bean.SkuLsInfo;
import com.atguigu.gmall0508.bean.SkuLsParams;
import com.atguigu.gmall0508.bean.SkuLsResult;
import com.atguigu.gmall0508.config.RedisUtil;
import com.atguigu.gmall0508.service.ListService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;

import io.searchbox.core.Update;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.sort.SortParseElement;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ListServiceImpl implements ListService {

    @Autowired
    private JestClient jestClient;

    @Autowired
    private RedisUtil redisUtil;
    // 保存的index ，type
    public static final String ES_INDEX="gmall";
    public static final String ES_TYPE="SkuInfo";

    //保存数据库中的数据到ES中
    @Override
    public void saveSkuInfo(SkuLsInfo skuLsInfo) {
        Index index = new Index.Builder(skuLsInfo).index(ES_INDEX).type(ES_TYPE).id(skuLsInfo.getId()).build();
        try {
            jestClient.execute(index);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //根据条件在ES中查询
    @Override
    public SkuLsResult search(SkuLsParams skuLsParams) {
        SkuLsResult skuLsResult=null;
        //1编写dsl语句
        String query=makeQueryStringForSearch(skuLsParams);
        //2执行，得到查询结果集
        Search search = new Search.Builder(query).addIndex(ES_INDEX).addType(ES_TYPE).build();
        SearchResult searchResult=null;
        try {
            searchResult = jestClient.execute(search);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //3封装结果集返回SkuLsResult对象
        skuLsResult=makeResultForSearch(skuLsParams,searchResult);




        return skuLsResult;
    }

    @Override
    public void incrHotScore(String skuId) {
        Jedis jedis = redisUtil.getJedis();
        Double score = jedis.zincrby("hotScore", 1, "skuId:" + skuId);
        if(score%5==0){
            updateHotScore(skuId,Math.round(score));
        }

    }

    private void updateHotScore(String skuId, long hotScore) {
        String query="{\n" +
                "   \"doc\":{\n" +
                "     \"hotScore\":"+hotScore+"\n" +
                "   }\n" +
                "}";
        Update update = new Update.Builder(query).index(ES_INDEX).type(ES_TYPE).id(skuId).build();

        try {
            jestClient.execute(update);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //封装查询结果集
    private SkuLsResult makeResultForSearch(SkuLsParams skuLsParams, SearchResult searchResult) {
        SkuLsResult skuLsResult=new SkuLsResult();

//        List<SkuLsInfo> skuLsInfoList;
//        long total;
//        long totalPages;
//        List<String> attrValueIdList;

        ArrayList<SkuLsInfo> skuLsInfoArrayList = new ArrayList<>();
        List<SearchResult.Hit<SkuLsInfo, Void>> hits = searchResult.getHits(SkuLsInfo.class);
        for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
            SkuLsInfo skuLsInfo = hit.source;
            //设置高亮
            if(hit.highlight!=null&&hit.highlight.size()>0){
                List<String> list = hit.highlight.get("skuName");
                String skuNameHl = list.get(0);
                skuLsInfo.setSkuName(skuNameHl);
            }
            skuLsInfoArrayList.add(skuLsInfo);
        }
        skuLsResult.setSkuLsInfoList(skuLsInfoArrayList);

        // 设置总条数
        skuLsResult.setTotal(searchResult.getTotal());
        // 总页数totalPages
        long page = (searchResult.getTotal()+skuLsParams.getPageSize()-1)/skuLsParams.getPageSize();
        skuLsResult.setTotalPages(page);
        // 从聚合中取得valueId
        MetricAggregation aggregations = searchResult.getAggregations();
        // 取得分组的名称
        TermsAggregation groupby_attr = aggregations.getTermsAggregation("groupby_attr");
        // groupby_attr:buckets
        List<TermsAggregation.Entry> buckets = groupby_attr.getBuckets();
        // 声明一个集合对象来存储valueId值
        ArrayList<String> arrayList = new ArrayList<>();
        for (TermsAggregation.Entry bucket : buckets) {
            // 取得平台属性值的Id
            String valueId = bucket.getKey();
            // 将平台属性值的Id 放入一个集合中
            arrayList.add(valueId);
        }
        skuLsResult.setAttrValueIdList(arrayList);
        return skuLsResult;
    }

    //创建ES查询语句
    private String makeQueryStringForSearch(SkuLsParams skuLsParams) {
        // 将dsl 语句，变为动态。将查询条件赋予dsl中
        // 1.先构建一个查询对象 query
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // 构建bool
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        // 构建查询条件 skuName
        if (skuLsParams.getKeyword()!=null && skuLsParams.getKeyword().length()>0){
            // 构建一个must,match { "skuName": "R730" }{ "skuName": "R730" }
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName", skuLsParams.getKeyword());
            // bool:must:match
            boolQueryBuilder.must(matchQueryBuilder);
            // 设置高亮 highlight
            HighlightBuilder highlighter = searchSourceBuilder.highlighter();
            highlighter.field("skuName");
            highlighter.preTags("<span style='color: red'>");
            highlighter.postTags("</span>");
            searchSourceBuilder.highlight(highlighter);
        }
        // 设置catalog3Id
        if (skuLsParams.getCatalog3Id()!=null && skuLsParams.getCatalog3Id().length()>0){
            // 创建term
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id", skuLsParams.getCatalog3Id());
            // bool:filter ：term
            boolQueryBuilder.filter(termQueryBuilder);
        }
        // 平台属性值
        /*
        字符串长度： length();
        数组长度： length;
        集合长度： size();
        文件长度：length();
         */
        if (skuLsParams.getValueId()!=null && skuLsParams.getValueId().length>0){
            for (int i = 0; i <  skuLsParams.getValueId().length; i++) {
                String valueId = skuLsParams.getValueId()[i];
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId", valueId);
                // bool:filter ：term
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }
        // 设置分页
        // 从第几条数据开始？
        // （pageNo-1）*pageSize
        int from = (skuLsParams.getPageNo()-1)*skuLsParams.getPageSize();
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(skuLsParams.getPageSize());
        // 设置排序
        searchSourceBuilder.sort("hotScore", SortOrder.DESC);
        // 聚合aggs terms.field {平台属性值}
        TermsBuilder groupby_attr = AggregationBuilders.terms("groupby_attr").field("skuAttrValueList.valueId");
        searchSourceBuilder.aggregation(groupby_attr);

        // 主要执行是query
        searchSourceBuilder.query(boolQueryBuilder);
        String query = searchSourceBuilder.toString();
        System.out.println("query:="+query);
        return query;
    }
}
