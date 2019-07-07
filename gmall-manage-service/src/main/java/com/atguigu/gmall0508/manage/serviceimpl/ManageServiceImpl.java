package com.atguigu.gmall0508.manage.serviceimpl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0508.bean.*;
import com.atguigu.gmall0508.config.RedisUtil;
import com.atguigu.gmall0508.manage.constant.ManageConst;
import com.atguigu.gmall0508.manage.mapper.*;
import com.atguigu.gmall0508.service.ManageService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import sun.misc.Cache;

import java.util.List;

@Service
public class ManageServiceImpl implements ManageService {
    @Autowired
    private BaseCatalog1Mapper baseCatalog1Mapper;
    @Autowired
    private BaseCatalog2Mapper baseCatalog2Mapper;
    @Autowired
    private BaseCatalog3Mapper baseCatalog3Mapper;
    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;
    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;
    @Autowired
    private SpuInfoMapper spuInfoMapper;
    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;
    @Autowired
    private SpuImageMapper spuImageMapper;
    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;
    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;
    @Autowired
    private  SkuInfoMapper skuInfoMapper;
    @Autowired
    private SkuImageMapper skuImageMapper;
    @Autowired
    private  SkuSaleAttrValueMapper skuSaleAttrValueMapper;
    @Autowired
    private  SkuAttrValueMapper skuAttrValueMapper;
    @Autowired
    private RedisUtil redisUtil;

    @Override
    public List<BaseCatalog1> getCatalog1() {
        return baseCatalog1Mapper.selectAll();

    }

    @Override
    public List<BaseCatalog2> getCatalog2(String catalog1Id) {
        BaseCatalog2 baseCatalog2=new BaseCatalog2();
        baseCatalog2.setCatalog1Id(catalog1Id);
        return baseCatalog2Mapper.select(baseCatalog2);
    }

    @Override
    public List<BaseCatalog3> getCatalog3(String catalog2Id) {
        BaseCatalog3 baseCatalog3=new BaseCatalog3();
        baseCatalog3.setCatalog2Id(catalog2Id);
        return baseCatalog3Mapper.select(baseCatalog3);
    }

    @Override
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        // 第一，发展的眼光看问题！ 插入，保存写在同一个方法了。null == ""; 123
        if (baseAttrInfo.getId()!=null && baseAttrInfo.getId().length()>0){
            // 做更新
            baseAttrInfoMapper.updateByPrimaryKeySelective(baseAttrInfo);
        }else {
            // 做插入 mysql 主键自增，必须当前字段为null
            if (baseAttrInfo.getId().length()==0){ // id="";
                baseAttrInfo.setId(null);
            }
            baseAttrInfoMapper.insertSelective(baseAttrInfo);
        }
        // baseAttrValue; 先删除，在插入。
        BaseAttrValue baseAttrValue = new BaseAttrValue();
        baseAttrValue.setAttrId(baseAttrInfo.getId());
        // delete from baseAttrValue where attrId = ?
        baseAttrValueMapper.delete(baseAttrValue);

        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        // 判断当前集合是否为空
        if (attrValueList!=null && attrValueList.size()>0){
            for (BaseAttrValue attrValue : attrValueList) {
                // 防止当前的id为"";
                if (attrValue.getId().length()==0){
                    attrValue.setId(null);
                }
                // attrJson["attrValueList["+i+"].attrId"]=平台属性名.id; 在插入数据库的时候，直接赋值即可！
                attrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insertSelective(attrValue);
            }
        }
    }

    @Override
    public List<SpuImage> getSpuImageList(String spuId) {
        SpuImage spuImage=new SpuImage();
        spuImage.setSpuId(spuId);
        return spuImageMapper.select(spuImage);


    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(String spuId) {
        return spuSaleAttrMapper.selectSpuSaleAttrList(Long.parseLong(spuId));
    }

    @Override
    public List<SpuSaleAttr> selectSpuSaleAttrListCheckBySku(SkuInfo skuInfo) {
       return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(Long.parseLong(skuInfo.getId()),Long.parseLong(skuInfo.getSpuId()));
    }

    @Override
    public List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId) {
        List<SkuSaleAttrValue> skuSaleAttrValues = skuSaleAttrValueMapper.selectSkuSaleAttrValueListBySpu(spuId);
        return skuSaleAttrValues;
    }

    @Override
    public List<BaseAttrInfo> getAttrList(List<String> attrValueIdList) {

        String attrValueIds = StringUtils.join(attrValueIdList.toArray(), ",");
        List<BaseAttrInfo> baseAttrInfoList= baseAttrInfoMapper.selectAttrInfoListByIds(attrValueIds);
        return baseAttrInfoList;
    }

    @Override
    public SkuInfo getSkuInfoBySkuId(String skuId) {
        SkuInfo skuInfo=null;
        try {
            Jedis jedis = redisUtil.getJedis();
            String skuInfoKey=ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKUKEY_SUFFIX;
            String skuJson = jedis.get(skuInfoKey);
            if(skuJson==null||"".equals(skuJson)){
                //System.out.println("未命中缓存");
                //从DB中查
                String skuLockKey=ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKULOCK_SUFFIX;
                String lockKey=jedis.set(skuLockKey,"OK","NX","PX",ManageConst.SKULOCK_EXPIRE_PX);
                if("OK".equals(lockKey)){
                    //从数据库中取数据，放入缓存
                    //System.out.println("获取分布式锁");
                    skuInfo = getSkuInfoDB(skuId);
                    jedis.setex(skuInfoKey,ManageConst.SKUKEY_TIMEOUT,JSON.toJSONString(skuInfo));
                    jedis.close();
                    return skuInfo;
                }else{
                    //其他的请求睡一会
                    Thread.sleep(1000);
                    //自旋
                    return getSkuInfoBySkuId(skuId);

                }
            }else{
                skuInfo = JSON.parseObject(skuJson, SkuInfo.class);
                jedis.close();
                return skuInfo;
            }


        }catch (Exception e){
            e.printStackTrace();
        }


        return getSkuInfoDB(skuId);

    }
    //从数据库获取skuinfo
    public SkuInfo getSkuInfoDB(String skuId){
        SkuInfo skuInfo=skuInfoMapper.selectByPrimaryKey(skuId);
        SkuImage skuImage=new SkuImage();
        skuImage.setSkuId(skuId);
        List<SkuImage> skuImageList=skuImageMapper.select(skuImage);
        skuInfo.setSkuImageList(skuImageList);
        //平台属性值集合
        SkuAttrValue skuAttrValue=new SkuAttrValue();
        skuAttrValue.setSkuId(skuId);
        List<SkuAttrValue> skuAttrValueList = skuAttrValueMapper.select(skuAttrValue);
        skuInfo.setSkuAttrValueList(skuAttrValueList);

        return skuInfo;

    }

    @Override
    public void saveSkuInfo(SkuInfo skuInfo) {
        // sku_info
        if (skuInfo.getId()==null || skuInfo.getId().length()==0){
            skuInfo.setId(null);
            skuInfoMapper.insertSelective(skuInfo);
        }else {
            skuInfoMapper.updateByPrimaryKeySelective(skuInfo);
        }

        // sku_img,
        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuInfo.getId());
        skuImageMapper.delete(skuImage);
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (skuImageList!=null && skuImageList.size()>0){
            for (SkuImage image : skuImageList) {
                if (image.getId()!=null && image.getId().length()==0){
                    image.setId(null);
                }
                image.setSkuId(skuInfo.getId());
                skuImageMapper.insertSelective(image);
            }
        }
        //sku_attr_value,
        SkuAttrValue skuAttrValue = new SkuAttrValue();
        skuAttrValue.setSkuId(skuInfo.getId());
        skuAttrValueMapper.delete(skuAttrValue);
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (skuAttrValueList!=null && skuAttrValueList.size()>0){
            for (SkuAttrValue attrValue : skuAttrValueList) {
                if (attrValue.getId()!=null && attrValue.getId().length()==0){
                    attrValue.setId(null);
                }
                attrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insertSelective(attrValue);
            }
        }
        // sku_sale_attr_value,
        SkuSaleAttrValue skuSaleAttrValue = new SkuSaleAttrValue();
        skuSaleAttrValue.setSkuId(skuInfo.getId());
        skuSaleAttrValueMapper.delete(skuSaleAttrValue);
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (skuSaleAttrValueList!=null && skuSaleAttrValueList.size()>0){
            for (SkuSaleAttrValue saleAttrValue : skuSaleAttrValueList) {
                if (saleAttrValue.getId()!=null && saleAttrValue.getId().length()==0){
                    saleAttrValue.setId(null);
                }
                saleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValueMapper.insertSelective(saleAttrValue);
            }
        }

    }

    @Override
    public void saveSpuInfo(SpuInfo spuInfo) {

        //保存spuinfo信息
        if(spuInfo.getId()!=null&&spuInfo.getId().length()>0){
            spuInfoMapper.updateByPrimaryKeySelective(spuInfo);
        }else{
            spuInfo.setId(null);
            spuInfoMapper.insertSelective(spuInfo);
        }
        //保存spuImage信息。先删除再保存
        SpuImage spuImage=new SpuImage();
        spuImage.setSpuId(spuInfo.getId());
        spuImageMapper.delete(spuImage);
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if(spuImageList!=null&&spuImageList.size()>0){
            for (SpuImage image : spuImageList) {
                if(image.getId()==null||image.getId().length()==0){
                    image.setId(null);
                }
                image.setSpuId(spuInfo.getId());
                spuImageMapper.insertSelective(image);
            }
        }

        //保存销售属性和销售属性值
        //先删除
        SpuSaleAttr spuSaleAttr=new SpuSaleAttr();
        spuSaleAttr.setSpuId(spuInfo.getId());
        spuSaleAttrMapper.delete(spuSaleAttr);

        SpuSaleAttrValue spuSaleAttrValue=new SpuSaleAttrValue();
        spuSaleAttrValue.setSpuId(spuInfo.getId());
        spuSaleAttrValueMapper.delete(spuSaleAttrValue);

        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if(spuSaleAttrList!=null&&spuSaleAttrList.size()>0){
            for (SpuSaleAttr saleAttr : spuSaleAttrList) {
                if(saleAttr.getId()==null||saleAttr.getId().length()==0){
                    saleAttr.setId(null);
                }
                saleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insertSelective(saleAttr);
                List<SpuSaleAttrValue> spuSaleAttrValueList = saleAttr.getSpuSaleAttrValueList();
                if(spuSaleAttrValueList!=null&&spuSaleAttrValueList.size()>0){
                    for (SpuSaleAttrValue saleAttrValue : spuSaleAttrValueList) {
                        if(saleAttrValue.getId()==null||saleAttrValue.getId().length()==0){
                            saleAttrValue.setId(null);
                        }
                        saleAttrValue.setSpuId(spuInfo.getId());
                        spuSaleAttrValueMapper.insertSelective(saleAttrValue);
                    }
                }
            }
        }

    }

    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return baseSaleAttrMapper.selectAll();
    }

    @Override
    public List<SpuInfo> getSpuInfoList(SpuInfo spuInfo) {
        return spuInfoMapper.select(spuInfo);
    }

    @Override
    public BaseAttrInfo getAttrInfo(String attrId) {
        // 通过attrId 获取BaseAttrInfo。 attrId = id
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectByPrimaryKey(attrId);

        // 根据attrId 查询baseAttrValue
        BaseAttrValue baseAttrValue = new BaseAttrValue();
        baseAttrValue.setAttrId(baseAttrInfo.getId());
        List<BaseAttrValue> baseAttrValueList  = baseAttrValueMapper.select(baseAttrValue);
        baseAttrInfo.setAttrValueList(baseAttrValueList);

        return baseAttrInfo;
    }

    @Override
    public List<BaseAttrInfo> getAttrList(String catalog3Id) {
        //BaseAttrInfo baseAttrInfo=new BaseAttrInfo();
       // baseAttrInfo.setCatalog3Id(catalog3Id);
        //return baseAttrInfoMapper.select(baseAttrInfo);
        return  baseAttrInfoMapper.getBaseAttrInfoListByCatalog3Id(Long.parseLong(catalog3Id));
    }
}
