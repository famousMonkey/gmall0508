package com.atguigu.gmall0508.service;

import com.atguigu.gmall0508.bean.PaymentInfo;

public interface PaymentService {
    public void  savyPaymentInfo(PaymentInfo paymentInfo);
    public PaymentInfo getPaymentInfo(PaymentInfo paymentInfoQuery);
    public void updatePaymentInfo(PaymentInfo paymentInfo);
    public void updatePaymentInfoByOutTradeNo(String out_trade_no, PaymentInfo paymentInfo);
    public void sendPaymentResult(PaymentInfo paymentInfo,String result);



}
