package com.atguigu.gmall0508.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0508.bean.*;
import com.atguigu.gmall0508.service.ListService;
import com.atguigu.gmall0508.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
public class ListController {

    @Reference
    private ListService listService;

    @Reference
    private ManageService manageService;
    //http://localhost:8086/list.html?keyword=小米&catalog3Id=61&valueId=13&pageNo=1&pageSize=10
    @RequestMapping("/list.html")
    public String getList(SkuLsParams skuLsParams, HttpServletRequest request){

        skuLsParams.setPageSize(1);

        SkuLsResult skuLsResult = listService.search(skuLsParams);
        //String string = JSON.toJSONString(skuLsResult);

        //商品信息
        List<SkuLsInfo> skuLsInfoList = skuLsResult.getSkuLsInfoList();
        request.setAttribute("skuLsInfoList",skuLsInfoList);

        //平台属性
        List<String> attrValueIdList = skuLsResult.getAttrValueIdList();
        List<BaseAttrInfo> baseAttrInfoList= manageService.getAttrList(attrValueIdList);
        //制作一个url
        String urlParam=makeUrlParam(skuLsParams);

        ArrayList<BaseAttrValue> baseAttrValueArrayList = new ArrayList<>();

        for (Iterator<BaseAttrInfo> iterator = baseAttrInfoList.iterator(); iterator.hasNext(); ) {
            BaseAttrInfo baseAttrInfo =  iterator.next();
            List<BaseAttrValue> attrValueList=baseAttrInfo.getAttrValueList();
            for (BaseAttrValue baseAttrValue : attrValueList) {
                baseAttrValue.setUrlParam(urlParam);
                if(skuLsParams.getValueId()!=null&&skuLsParams.getValueId().length>0){
                    for(String valueId:skuLsParams.getValueId()){
                        if (valueId.equals(baseAttrValue.getId())){
                            iterator.remove();
                            BaseAttrValue baseAttrValueSelected = new BaseAttrValue();
                            // 属性名：属性值名称
                            baseAttrValueSelected.setValueName(baseAttrInfo.getAttrName()+":"+baseAttrValue.getValueName());
                            String makeUrlParam = makeUrlParam(skuLsParams, valueId);
                            baseAttrValueSelected.setUrlParam(makeUrlParam);
                            baseAttrValueArrayList.add(baseAttrValueSelected);
                        }
                    }
                }
            }
        }
        //保存面包屑
        request.setAttribute("baseAttrValueList",baseAttrValueArrayList);
        //分页
        request.setAttribute("totalPages",skuLsResult.getTotalPages());
        request.setAttribute("pageNo",skuLsParams.getPageNo());

        //保存平台属性集合
        request.setAttribute("baseAttrInfoList",baseAttrInfoList);
        request.setAttribute("keyword",skuLsParams.getKeyword());
        request.setAttribute("urlParam",urlParam);
        return "list";
    }

    private String makeUrlParam(SkuLsParams skuLsParams,String ... excludeValueIds) {
        String urlParam="";
        if(skuLsParams.getKeyword()!=null&&skuLsParams.getKeyword().length()>0){
            urlParam+="keyword="+skuLsParams.getKeyword();
        }

        if(skuLsParams.getCatalog3Id()!=null&& skuLsParams.getCatalog3Id().length()>0){
           if(urlParam.length()>0){
               urlParam+="&";
           }
            urlParam+="catalog3Id="+skuLsParams.getCatalog3Id();
        }
        if(skuLsParams.getValueId()!=null&&skuLsParams.getValueId().length>0){
            for (int i = 0; i < skuLsParams.getValueId().length; i++) {
                String valueId = skuLsParams.getValueId()[i];

                if (excludeValueIds!=null && excludeValueIds.length>0){
                    String excludeValueId = excludeValueIds[0];
                    if (excludeValueId.equals(valueId)){
                        continue;
                    }
                }


                if(urlParam.length()>0){
                    urlParam+="&";
                }
                urlParam+="valueId="+valueId;
            }
        }


        return urlParam;
    }


}
