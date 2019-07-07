package com.atguigu.gmall0508.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall0508.bean.OrderInfo;
import com.atguigu.gmall0508.bean.PaymentInfo;
import com.atguigu.gmall0508.bean.enums.PaymentStatus;
import com.atguigu.gmall0508.config.LoginRequire;
import com.atguigu.gmall0508.payment.config.AlipayConfig;
import com.atguigu.gmall0508.service.OrderService;

import com.atguigu.gmall0508.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PaymentController {

    @Reference
    private OrderService orderService;

    @Reference
    private PaymentService paymentService;

    @Autowired
    private AlipayClient alipayClient;

    @RequestMapping("/index")
    @LoginRequire(autoRedirect = true)
    public String index(HttpServletRequest request, Map<String,String> map){

        String orderId = request.getParameter("orderId");
        request.setAttribute("orderId",orderId);
        OrderInfo orderInfo=orderService.getOrderInfoById(orderId);
        BigDecimal totalAmount = orderInfo.getTotalAmount();
        request.setAttribute("totalAmount",totalAmount);
        return "paymentIndex";
    }

    @RequestMapping("/alipay/submit")
    @ResponseBody
    public String submitPayment(HttpServletRequest request, HttpServletResponse response){

        String orderId = request.getParameter("orderId");
        OrderInfo orderInfo = orderService.getOrderInfoById(orderId);

        PaymentInfo paymentInfo=new PaymentInfo();
        paymentInfo.setOrderId(orderId);
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setSubject("！！购买测试！！");
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID);
        //保存支付信息
        paymentService.savyPaymentInfo(paymentInfo);

        //制作支付宝所需的参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);

        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no",paymentInfo.getOutTradeNo());
        map.put("product_code","FAST_INSTANT_TRADE_PAY");
        map.put("subject",paymentInfo.getSubject());
        map.put("total_amount",paymentInfo.getTotalAmount());

        String jsonMap = JSON.toJSONString(map);
        alipayRequest.setBizContent(jsonMap);
        String form="";
        try {
            //调用SDK生成表单
            form = alipayClient.pageExecute(alipayRequest).getBody();
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        response.setContentType("text/html;charset=UTF-8");
        return form;

    }


    // 测试同步回调
    @RequestMapping("/alipay/callback/return")
    public String callbackReturn(){
        return "redirect:"+AlipayConfig.return_order_url;
    }


    // 异步回调 -- 业务逻辑。
    @RequestMapping("callback/notify")
    @ResponseBody
    public String paymentNotify(@RequestParam Map<String,String> paramMap, HttpServletRequest request) throws AlipayApiException {
        // 将异步回调通知的参数封装到一个paramMap集合中
            boolean signVerified = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, "utf-8",AlipayConfig.sign_type); //调用SDK验证签名
            if(signVerified){
                // 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
                //   trade_status == TRADE_SUCCESS TRADE_FINISHED
            //   如果该订单未支付，且不能关闭才是成功！ paymentInfo 表中记录支付信息 【out_trade_no】
            String trade_status = paramMap.get("trade_status");
            if ("TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)){
                String out_trade_no = paramMap.get("out_trade_no");
                PaymentInfo paymentInfoQuery  = new PaymentInfo();
                paymentInfoQuery.setOutTradeNo(out_trade_no);
                PaymentInfo paymentInfo = paymentService.getPaymentInfo(paymentInfoQuery);
                // 获取paymentInfo的支付状态
                if (paymentInfo.getPaymentStatus()==PaymentStatus.ClOSED || paymentInfo.getPaymentStatus()==PaymentStatus.PAID){
                    return "fail";
                }
                // 创建一个更新的对象
                paymentInfo.setPaymentStatus(PaymentStatus.PAID);
                paymentInfo.setCallbackTime(new Date());
                paymentInfo.setCallbackContent(paramMap.toString());

                paymentService.updatePaymentInfo(paymentInfo);
                //paymentService.updatePaymentInfoByOutTradeNo(out_trade_no,paymentInfo);

                // 发送通知给订单
                paymentService.sendPaymentResult(paymentInfo,"success");

                return "success";
            }else {
                return "fail";
            }
        }else{
            // 验签失败则记录异常日志，并在response中返回failure.
            return "fail";
        }
    }





    //
    @RequestMapping("/sendPaymentResult")
    @ResponseBody
    public String sendPaymentResult(PaymentInfo paymentInfo, @RequestParam("result")String result ){
        paymentService.sendPaymentResult(paymentInfo,result);
        return "success";

    }


}
