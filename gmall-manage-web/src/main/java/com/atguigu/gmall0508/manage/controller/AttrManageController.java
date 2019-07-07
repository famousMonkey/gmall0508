package com.atguigu.gmall0508.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0508.bean.*;
import com.atguigu.gmall0508.service.ListService;
import com.atguigu.gmall0508.service.ManageService;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

@Controller
public class AttrManageController {

    @Reference
    private ManageService manageService;
    @Reference
    private ListService listService;

    @RequestMapping("/onSale")
    @ResponseBody
    public String onSale(String skuId){
        SkuInfo skuInfo = manageService.getSkuInfoBySkuId(skuId);
        SkuLsInfo skuLsInfo = new SkuLsInfo();
        try {
            BeanUtils.copyProperties(skuLsInfo, skuInfo);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        listService.saveSkuInfo(skuLsInfo);
        return "success";
    }



    @RequestMapping("/attrListPage")
    public String attrListPage(){
        return "attrListPage";
    }

    /**
     * 获取一级分类属性
     * @return
     */
    @RequestMapping("/getCatalog1")
    @ResponseBody
    public List<BaseCatalog1> getCatalog1(){
        List<BaseCatalog1> catalog1List = manageService.getCatalog1();
        return catalog1List;
    }

    /**
     * 获取二级分类属性
     * @param catalog1Id
     * @return
     */
    @RequestMapping("/getCatalog2")
    @ResponseBody
    public List<BaseCatalog2> getCatalog2(String catalog1Id){
       return manageService.getCatalog2(catalog1Id);
    }

    /**
     * 获取三级分类属性
     * @param catalog2Id
     * @return
     */
    @RequestMapping("/getCatalog3")
    @ResponseBody
    public List<BaseCatalog3> getCatalog3(String catalog2Id){
        return manageService.getCatalog3(catalog2Id);
    }

    /**
     * 获取平台属性列表
     * @param catalog3Id
     * @return
     */
    @RequestMapping("/attrInfoList")
    @ResponseBody
    public List<BaseAttrInfo> getAttrInfo(String catalog3Id){
        return manageService.getAttrList(catalog3Id);
    }

    @RequestMapping("getAttrValueList")
    @ResponseBody
    public List<BaseAttrValue> getAttrValueList(String attrId){
        // 调用服务
        BaseAttrInfo baseAttrInfo =  manageService.getAttrInfo(attrId);
        // baseAttrInfo.id = baseAttrValue.attrId;
        return baseAttrInfo.getAttrValueList();
    }

    @RequestMapping(value = "saveAttrInfo",method = RequestMethod.POST)
    @ResponseBody
    public String saveAttrInfo(BaseAttrInfo baseAttrInfo){
        manageService.saveAttrInfo(baseAttrInfo);
        return "success";
    }

}
