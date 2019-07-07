package com.atguigu.gmall0508.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0508.bean.SkuInfo;
import com.atguigu.gmall0508.bean.SkuSaleAttrValue;
import com.atguigu.gmall0508.bean.SpuSaleAttr;
import com.atguigu.gmall0508.config.LoginRequire;
import com.atguigu.gmall0508.service.ListService;
import com.atguigu.gmall0508.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;

@Controller
public class ItemController {

    @Reference
    private ManageService manageService;
    @Reference
    private ListService listService;


    @RequestMapping("/{skuId}.html")
    @LoginRequire(autoRedirect = true)
    public String skuInfoPage(@PathVariable("skuId") String skuId,HttpServletRequest request){
        SkuInfo skuInfo=manageService.getSkuInfoBySkuId(skuId);
        System.out.println(skuId);
        request.setAttribute("skuInfo",skuInfo);
        //销售属性
        List<SpuSaleAttr> spuSaleAttrList=manageService.selectSpuSaleAttrListCheckBySku(skuInfo);
        request.setAttribute("saleAttrList",spuSaleAttrList);


        List<SkuSaleAttrValue> skuSaleAttrValueListBySpu = manageService.getSkuSaleAttrValueListBySpu(skuInfo.getSpuId());
        String jsonKey ="";
        HashMap<String, String> map = new HashMap<>();
        for (int i = 0; i < skuSaleAttrValueListBySpu.size(); i++) {
            SkuSaleAttrValue skuSaleAttrValue = skuSaleAttrValueListBySpu.get(i);
            if (jsonKey.length()!=0){
                jsonKey+="|";
            }
            jsonKey+=skuSaleAttrValue.getSaleAttrValueId();
            if ((i+1)==skuSaleAttrValueListBySpu.size() || !skuSaleAttrValue.getSkuId().equals(skuSaleAttrValueListBySpu.get(i+1).getSkuId())){
                map.put(jsonKey,skuSaleAttrValue.getSkuId());
                jsonKey="";
            }
        }
        String valuesSkuJson =  JSON.toJSONString(map);
        System.out.println(valuesSkuJson);
        request.setAttribute("valuesSkuJson",valuesSkuJson);

        //调用商品热度排名
        listService.incrHotScore(skuId);


        return "item";
    }

}
