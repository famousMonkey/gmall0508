package com.atguigu.gmall0508.config;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;

import javax.jms.Connection;
import javax.jms.JMSException;

public class ActiveMQUtil {
    //声明一个连接池的工厂对象
    PooledConnectionFactory pooledConnectionFactory=null;

    //初始化连接池
    public void init(String brokerUrl){
        ActiveMQConnectionFactory connectionFactory =
                new ActiveMQConnectionFactory(brokerUrl);
        pooledConnectionFactory = new PooledConnectionFactory(connectionFactory);
        //最大连接数
        pooledConnectionFactory.setMaxConnections(5);
        //超时时间
        pooledConnectionFactory.setExpiryTimeout(2000L);
        //出现异常，是否重连
        pooledConnectionFactory.setReconnectOnException(true);

    }

    //
    public Connection getConnection(){
        Connection connection=null;
        try {
            connection = pooledConnectionFactory.createConnection();
        } catch (JMSException e) {
            e.printStackTrace();
        }
        return connection;
    }
}
