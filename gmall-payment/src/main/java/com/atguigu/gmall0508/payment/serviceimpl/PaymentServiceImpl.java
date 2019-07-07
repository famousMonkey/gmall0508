package com.atguigu.gmall0508.payment.serviceimpl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall0508.bean.PaymentInfo;
import com.atguigu.gmall0508.config.ActiveMQUtil;
import com.atguigu.gmall0508.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall0508.service.PaymentService;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private ActiveMQUtil activeMQUtil;

    @Override
    public void savyPaymentInfo(PaymentInfo paymentInfo) {
        paymentInfoMapper.insertSelective(paymentInfo);
    }

    @Override
    public PaymentInfo getPaymentInfo(PaymentInfo paymentInfoQuery) {
       return paymentInfoMapper.selectOne(paymentInfoQuery);

    }

    @Override
    public void updatePaymentInfo(PaymentInfo paymentInfo) {
        paymentInfoMapper.updateByPrimaryKey(paymentInfo);
    }

    @Override
    public void updatePaymentInfoByOutTradeNo(String out_trade_no, PaymentInfo paymentInfo) {
        Example example=new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("outTradeNo",out_trade_no);
        paymentInfoMapper.updateByExampleSelective(paymentInfo,example);
    }

    @Override
    public void sendPaymentResult(PaymentInfo paymentInfo, String result) {
        Connection connection = activeMQUtil.getConnection();
        try {
            //开启连接
            connection.start();
            //创建session
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            //创建消息队列
            Queue queue = session.createQueue("PAYMENT_RESULT_QUEUE");
            //创建消息生产者
            MessageProducer producer = session.createProducer(queue);
            //创建消息对象
            ActiveMQMapMessage message = new ActiveMQMapMessage();
            //设置消息内容
            message.setString("orderId",paymentInfo.getOrderId());
            message.setString("result",result);
            //生产者发送消息
            producer.send(message);
            //关闭资源
            producer.close();
            session.close();
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
