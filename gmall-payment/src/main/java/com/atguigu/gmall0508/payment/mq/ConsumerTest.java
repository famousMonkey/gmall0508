package com.atguigu.gmall0508.payment.mq;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

public class ConsumerTest {

    public static void main(String[] args) throws JMSException {
        //创建消费者消息队列的连接工厂，传入参数 用户名，密码(默认都是admin)以及brokerURL
        ActiveMQConnectionFactory connectionFactory =
                new ActiveMQConnectionFactory(ActiveMQConnection.DEFAULT_USER, ActiveMQConnection.DEFAULT_PASSWORD, "tcp://192.168.236.130:61616");
        Connection connection = connectionFactory.createConnection();
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue queue = session.createQueue("monkey");
        MessageConsumer consumer = session.createConsumer(queue);
        //设置一个消息的监听器，去监测是否有消息产生
        consumer.setMessageListener(new MessageListener() {
            @Override
            public void onMessage(Message message) {
                if(message instanceof TextMessage){
                    try {
                        String text = ((TextMessage) message).getText();
                        System.out.println("消息内容"+text);
                    } catch (JMSException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        consumer.close();
        session.close();
        connection.close();
    }


}
