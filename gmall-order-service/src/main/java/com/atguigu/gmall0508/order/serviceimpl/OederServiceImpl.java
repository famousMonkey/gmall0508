package com.atguigu.gmall0508.order.serviceimpl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0508.bean.OrderDetail;
import com.atguigu.gmall0508.bean.OrderInfo;
import com.atguigu.gmall0508.bean.enums.ProcessStatus;
import com.atguigu.gmall0508.config.ActiveMQUtil;
import com.atguigu.gmall0508.config.RedisUtil;
import com.atguigu.gmall0508.order.mapper.OrderDetailMapper;
import com.atguigu.gmall0508.order.mapper.OrderInfoMapper;
import com.atguigu.gmall0508.service.OrderService;
import com.atguigu.gmall0508.util.HttpClientUtil;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import javax.jms.*;
import javax.jms.Queue;
import java.util.*;

@Service
public class OederServiceImpl implements OrderService {

    @Autowired
    private OrderInfoMapper  orderInfoMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private ActiveMQUtil activeMQUtil;

    @Override
    public String saveOrder(OrderInfo orderInfo) {

        orderInfo.setCreateTime(new Date());
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE,1);
        orderInfo.setExpireTime(calendar.getTime());
        String outTradeNo="ATGUIGU"+System.currentTimeMillis()+""+new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        orderInfoMapper.insertSelective(orderInfo);

        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insertSelective(orderDetail);
        }
        String orderId = orderInfo.getId();
        return orderId;
    }

    @Override
    public String getTradeNo(String userId) {
        Jedis jedis = redisUtil.getJedis();
        String tradeNoKey="user:"+userId+":tradeCode";
        String tradeCode=UUID.randomUUID().toString();
        jedis.setex(tradeNoKey,10*60,tradeCode);
        jedis.close();
        return tradeCode;
    }

    @Override
    public boolean checkTradeCode(String userId, String tradeCodeNo) {
        Jedis jedis = redisUtil.getJedis();
        String tradeNoKey="user:"+userId+":tradeCode";
        String tradeCode = jedis.get(tradeNoKey);
        jedis.close();
        if(tradeCode!=null&&tradeCodeNo.equals(tradeCode)){
            return true;
        }
        return false;
    }

    @Override
    public void delTradeCode(String userId) {
        Jedis jedis = redisUtil.getJedis();
        String tradeNoKey="user:"+userId+":tradeCode";
        jedis.del(tradeNoKey);
        jedis.close();

    }

    @Override
    public boolean checkStock(String skuId, Integer skuNum) {
        String result = HttpClientUtil.doGet("http://www.gware.com/hasStock?skuId=" + skuId + "&num=" + skuNum);
        if ("1".equals(result)){
            return  true;
        }else {
            return  false;
        }

    }

    @Override
    public OrderInfo getOrderInfoById(String orderId) {
        return orderInfoMapper.selectByPrimaryKey(orderId);
    }

    @Override
    public void updateOrderStatus(String orderId, ProcessStatus paid) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        //设置订单状态
        orderInfo.setOrderStatus(paid.getOrderStatus());
        //设置订单进程状态
        orderInfo.setProcessStatus(paid);
        //更新订单状态
        orderInfoMapper.updateByPrimaryKeySelective(orderInfo);
    }

    @Override
    public void sendOrderStatus(String orderId) {
        Connection connection = activeMQUtil.getConnection();
        // 符合减库存的json 字符串
        String orderJson = initWareOrder(orderId);

        try {
            connection.start();
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue queue = session.createQueue("ORDER_RESULT_QUEUE");
            MessageProducer producer = session.createProducer(queue);
            ActiveMQTextMessage message = new ActiveMQTextMessage();
            message.setText(orderJson);
            producer.send(message);
            session.commit();

            //关闭资源
            producer.close();
            session.close();
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }

    }

    private String initWareOrder(String orderId) {
        OrderInfo orderInfo = getOrderInfoById(orderId);
        Map map = initWareOrder(orderInfo);
        return JSON.toJSONString(map);
    }

    private Map<String,Object> initWareOrder(OrderInfo orderInfo){
        Map<String,Object> map=new HashMap<>();
        map.put("orderId",orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel",orderInfo.getConsigneeTel());
        map.put("orderComment",orderInfo.getOrderComment());
        map.put("orderBody","!!!!!更改库存测试!!!!!");
        map.put("deliveryAddress",orderInfo.getDeliveryAddress());
        map.put("paymentWay","2");
        // 仓库Id 给拆单预留的。
        // map.put("wareId",orderInfo.getWareId());
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        List detailList = new ArrayList();
        for (OrderDetail orderDetail : orderDetailList) {
            // 存储一个map 集合
            Map detailMap = new HashMap();
            detailMap.put("skuId",orderDetail.getSkuId());
            detailMap.put("skuNum",orderDetail.getSkuNum());
            detailMap.put("skuName",orderDetail.getSkuName());
            detailList.add(detailMap);
        }
        map.put("details",detailList);
        return map;
    }
}
