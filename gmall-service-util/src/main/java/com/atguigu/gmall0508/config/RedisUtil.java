package com.atguigu.gmall0508.config;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisUtil {

    private JedisPool jedisPool;

    //初始化jedispool连接池
    public void initJedisPool(String host,int port,int database){
        JedisPoolConfig jedisPoolConfig=new JedisPoolConfig();
        // 连接总数
        jedisPoolConfig.setMaxTotal(200);
        // 获取连接时等待的最大毫秒
        jedisPoolConfig.setMaxWaitMillis(10*1000);
        // 最少剩余数
        jedisPoolConfig.setMinIdle(10);
        // 如果到最大数，设置等待
        jedisPoolConfig.setBlockWhenExhausted(true);
        // 在获取连接时，检查是否有效
        jedisPoolConfig.setTestOnBorrow(true);
        jedisPool=new JedisPool(jedisPoolConfig,host,port,20*1000);

    }

    //获取jedis对象
    public Jedis getJedis(){
        //从连接池中获取连接
        Jedis jedis = jedisPool.getResource();
        return jedis;
    }

}
