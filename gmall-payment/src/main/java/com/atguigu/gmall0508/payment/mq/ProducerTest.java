package com.atguigu.gmall0508.payment.mq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;

import javax.jms.*;

public class ProducerTest {
    public static void main(String[] args) throws JMSException {
        //创建生产者消息队列的连接工厂
        ActiveMQConnectionFactory connectionFactory =
                new ActiveMQConnectionFactory("tcp://192.168.236.130:61616");
        //获取连接
        Connection connection = connectionFactory.createConnection();
        //开启连接
        connection.start();
        //创建session，第一个参数代表是否开启事务
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        //创建消息队列
        Queue queue = session.createQueue("monkey");
        //创建提供者
        MessageProducer producer = session.createProducer(queue);
        //创建消息对象
        ActiveMQTextMessage message = new ActiveMQTextMessage();
        //设置消息
        message.setText("famous Monkey!");
        //提供者发送消息
        producer.send(message);

        //关闭资源
        producer.close();
        session.close();
        connection.close();
    }

}
