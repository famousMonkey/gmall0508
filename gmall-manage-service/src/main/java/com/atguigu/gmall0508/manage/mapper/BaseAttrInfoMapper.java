package com.atguigu.gmall0508.manage.mapper;

import com.atguigu.gmall0508.bean.BaseAttrInfo;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface BaseAttrInfoMapper extends Mapper<BaseAttrInfo> {
    public List<BaseAttrInfo> getBaseAttrInfoListByCatalog3Id(long catalog3Id);

    List<BaseAttrInfo> selectAttrInfoListByIds(@Param(value = "attrValueIds") String attrValueIds);
}
