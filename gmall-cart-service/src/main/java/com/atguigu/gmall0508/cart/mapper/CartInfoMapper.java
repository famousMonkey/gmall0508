package com.atguigu.gmall0508.cart.mapper;

import com.atguigu.gmall0508.bean.CartInfo;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface CartInfoMapper extends Mapper<CartInfo> {
    public List<CartInfo> selectCartListWithCurPrice(String userId);
}
