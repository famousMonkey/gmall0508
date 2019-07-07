package com.atguigu.gmall0508.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0508.bean.*;
import com.atguigu.gmall0508.bean.enums.OrderStatus;
import com.atguigu.gmall0508.bean.enums.ProcessStatus;
import com.atguigu.gmall0508.config.LoginRequire;
import com.atguigu.gmall0508.service.CartService;
import com.atguigu.gmall0508.service.ManageService;
import com.atguigu.gmall0508.service.OrderService;
import com.atguigu.gmall0508.service.UserAddressService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@Controller
public class OrderController {

    @Reference
    private UserAddressService userAddressService;

    @Reference
    private CartService cartService;

    @Reference
    private OrderService orderService;

    @Reference
    private ManageService manageService;

    @RequestMapping("/findAllUserAddress")
    @ResponseBody
    public List<UserAddress> getUserAddressList(String userId){
        List<UserAddress> userAddressList = userAddressService.getUserAddressList(userId);
        return userAddressList;
    }

    @RequestMapping("/trade")
    @LoginRequire(autoRedirect = true)
    public String trade(HttpServletRequest request){
        String userId = (String) request.getAttribute("userId");

        //收货人地址
        List<UserAddress> userAddressList = userAddressService.getUserAddressList(userId);
        request.setAttribute("userAddressList",userAddressList);
        //获取购物车中选中的商品
        List<CartInfo> cartCheckedList = cartService.getCartCheckedList(userId);
        List<OrderDetail> orderDetailList=new ArrayList<>(cartCheckedList.size());
        for (CartInfo cartInfo : cartCheckedList) {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setImgUrl(cartInfo.getImgUrl());
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            orderDetail.setOrderPrice(cartInfo.getCartPrice());
            orderDetailList.add(orderDetail);
        }
        request.setAttribute("orderDetailList",orderDetailList);
        OrderInfo orderInfo=new OrderInfo();
        orderInfo.setOrderDetailList(orderDetailList);
        orderInfo.sumTotalAmount();
        request.setAttribute("totalAmount",orderInfo.getTotalAmount());

        String tradeNo = orderService.getTradeNo(userId);
        request.setAttribute("tradeNo",tradeNo);

        return "trade";
    }

    @RequestMapping("/submitOrder")
    @LoginRequire(autoRedirect = true)
    public String submitOrder(OrderInfo orderInfo,HttpServletRequest request){

        String userId = (String) request.getAttribute("userId");
        //校验流水号
        String tradeNo = request.getParameter("tradeNo");
        boolean flag = orderService.checkTradeCode(userId, tradeNo);
        if(!flag){
            request.setAttribute("errMsg","该页面已失效，请重新结算!");
            return "tradeFail";
        }

        orderInfo.setOrderStatus(OrderStatus.UNPAID);
        orderInfo.setProcessStatus(ProcessStatus.UNPAID);
        orderInfo.sumTotalAmount();
        orderInfo.setUserId(userId);

        // 校验库存，价格
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            //验价
            SkuInfo skuInfo = manageService.getSkuInfoBySkuId(orderDetail.getSkuId());
            if(skuInfo!=null){
                //BigDecimal比较大小
                int res = orderDetail.getOrderPrice().compareTo(skuInfo.getPrice());
                if(res!=0){
                    request.setAttribute("errMsg","商品价格发生变动，请重新下单！");
                    // 重新加载redis中购物车数据
                    cartService.loadCartCache(userId);
                    return "tradeFail";
                }
            }

            // 验库存
            boolean result = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
            if (!result){
                request.setAttribute("errMsg","商品库存不足，请重新下单！");
                return "tradeFail";
            }
        }



        String orderId = orderService.saveOrder(orderInfo);
       //删除流水号
        orderService.delTradeCode(userId);
        return "redirect://payment.gmall.com/index?orderId="+orderId;
    }





}
