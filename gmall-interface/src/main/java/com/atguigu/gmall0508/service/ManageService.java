package com.atguigu.gmall0508.service;

import com.atguigu.gmall0508.bean.*;

import java.util.List;

public interface ManageService {

    public List<BaseCatalog1> getCatalog1();

    public List<BaseCatalog2> getCatalog2(String catalog1Id);

    public List<BaseCatalog3> getCatalog3(String catalog2Id);

    public List<BaseAttrInfo> getAttrList(String catalog3Id);

    public void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    public BaseAttrInfo getAttrInfo(String attrId);

    public List<SpuInfo> getSpuInfoList(SpuInfo spuInfo);

    public List<BaseSaleAttr> getBaseSaleAttrList();

    // 保存spuInfo 信息
    public void saveSpuInfo(SpuInfo spuInfo);

    public List<SpuImage> getSpuImageList(String spuId);

    public List<SpuSaleAttr> getSpuSaleAttrList(String spuId);

    public void saveSkuInfo(SkuInfo skuInfo);

    public SkuInfo getSkuInfoBySkuId(String skuId);

    public List<SpuSaleAttr> selectSpuSaleAttrListCheckBySku(SkuInfo skuInfo);

    public List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId);

    public List<BaseAttrInfo> getAttrList(List<String> attrValueIdList);
}
