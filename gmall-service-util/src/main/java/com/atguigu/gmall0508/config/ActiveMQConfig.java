package com.atguigu.gmall0508.config;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.stereotype.Component;

import javax.jms.Session;

@Component
public class ActiveMQConfig {

    @Value("${spring.activemq.broker-url:disabled}")
    String brokerUrl;

    @Value("${activemq.listener.enable:disabled}")
    String listenerEnable;


    //获取ActiveMQUtil
    @Bean
    public ActiveMQUtil getActiveMQUtil(){
        if("disable".equals(brokerUrl)){
            return null;
        }
        ActiveMQUtil activeMQUtil = new ActiveMQUtil();
        activeMQUtil.init(brokerUrl);
        return activeMQUtil;
    }

    //获取消息队列连接工厂的监听器
    @Bean(name = "jmsQueueListener")
    public DefaultJmsListenerContainerFactory getJmsListenerContainerFactory(ActiveMQConnectionFactory activeMQConnectionFactory){
        if("disabled".equals(listenerEnable)){
            return null;
        }
        DefaultJmsListenerContainerFactory factory =
                new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(activeMQConnectionFactory);
        //设置事物
        factory.setSessionTransacted(false);
        //设置自动签收
        factory.setSessionAcknowledgeMode(Session.AUTO_ACKNOWLEDGE);
        //设置重连的间隔时间
        factory.setRecoveryInterval(5000L);
        //设置并发数
        factory.setConcurrency("5");
        return factory;
    }

    //获取消息队列的连接工厂
    @Bean
    public ActiveMQConnectionFactory getactiveMQConnectionFactory(){
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
        return connectionFactory;
    }


}
