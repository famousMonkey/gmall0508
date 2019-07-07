package com.atguigu.gmall0508.order.mq;


import com.atguigu.gmall0508.bean.enums.ProcessStatus;
import com.atguigu.gmall0508.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class OrderConsumer {

    @Autowired
    private OrderService orderService;

    @JmsListener(destination = "PAYMENT_RESULT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumerPaymentResult(MapMessage mapMessage) throws JMSException {
        String result = mapMessage.getString("result");
        String orderId = mapMessage.getString("orderId");
        if("success".equals(result)){
            // 修改订单状态
            orderService.updateOrderStatus(orderId, ProcessStatus.PAID);
            // 发送一个通知告诉库存
            orderService.sendOrderStatus(orderId);
            // 变成待状态
            orderService.updateOrderStatus(orderId, ProcessStatus.NOTIFIED_WARE);
        }

    }


    @JmsListener(destination = "SKU_DEDUCT_QUEUE",containerFactory = "jmsQueueListener")
    public void  consumeSkuDeduct(MapMessage mapMessage) throws JMSException {
        String orderId = mapMessage.getString("orderId");
        String status = mapMessage.getString("status");
        if ("DEDUCTED".equals(status)){
            System.out.println("--- 消费减库存！");
            orderService.updateOrderStatus(orderId, ProcessStatus.DELEVERED);
        }
    }



}
