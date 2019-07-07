package com.atguigu.gmall0508.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0508.bean.SkuInfo;
import com.atguigu.gmall0508.bean.SpuSaleAttr;
import com.atguigu.gmall0508.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class SkuManageController {

    @Reference
    private ManageService manageService;

    @RequestMapping("spuSaleAttrList")
    @ResponseBody
    public List<SpuSaleAttr> spuSaleAttrList(String spuId){
        /*valueName = 是哪个表中，查出的数据应该 sale_attr_value_name*/
        List<SpuSaleAttr> spuSaleAttrList = manageService.getSpuSaleAttrList(spuId);

        return spuSaleAttrList;
    }

    @PostMapping("/saveSku")
    @ResponseBody
    public String  saveSkuInfo(SkuInfo skuInfo){
        manageService.saveSkuInfo(skuInfo);
        return "success";
    }

}
