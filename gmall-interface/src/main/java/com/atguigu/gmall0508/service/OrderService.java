package com.atguigu.gmall0508.service;

import com.atguigu.gmall0508.bean.OrderInfo;
import com.atguigu.gmall0508.bean.enums.ProcessStatus;

public interface OrderService {
    public  String  saveOrder(OrderInfo orderInfo);
    public  String getTradeNo(String userId);
    public  boolean checkTradeCode(String userId,String tradeCodeNo);
    public void  delTradeCode(String userId);
    public boolean checkStock(String skuId, Integer skuNum);
    public OrderInfo getOrderInfoById(String orderId);
    public void updateOrderStatus(String orderId, ProcessStatus paid);

    public void sendOrderStatus(String orderId);
}
