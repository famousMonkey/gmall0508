package com.atguigu.gmall0508.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0508.bean.CartInfo;
import com.atguigu.gmall0508.bean.SkuInfo;
import com.atguigu.gmall0508.config.LoginRequire;
import com.atguigu.gmall0508.service.CartService;
import com.atguigu.gmall0508.service.ManageService;
import org.apache.catalina.connector.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
public class CartController {


    @Reference
    private CartService cartService;
    @Autowired
    private CartCookieHandler cartCookieHandler;
    @Reference
    private ManageService manageService;

    @RequestMapping("/index")
    public String index(){
        return "success";
    }


    @RequestMapping("addToCart")
    @LoginRequire(autoRedirect = false)
    public String addToCart(HttpServletRequest request, HttpServletResponse response){
        //在item.html页面中获取参数
        String num = request.getParameter("num");
        String skuId = request.getParameter("skuId");
        //因为有@LoginRequire，所以从拦截其中可以获取userid
        String userId = (String) request.getAttribute("userId");

        if(userId!=null&&userId.length()>0){
            //已登录
            cartService.addToCart(skuId,userId,Integer.parseInt(num));
        }else{
            //未登录，数据存储到cookie中
            cartCookieHandler.addToCart(request,response,skuId,userId,Integer.parseInt(num));
        }
        SkuInfo skuInfo = manageService.getSkuInfoBySkuId(skuId);
        request.setAttribute("skuInfo",skuInfo);
        request.setAttribute("num",num);
        return "success";
    }


    @RequestMapping("cartList")
    @LoginRequire(autoRedirect = false)
    public  String cartList(HttpServletRequest request,HttpServletResponse response){
        String userId = (String) request.getAttribute("userId");
        if (userId!=null){
            // 从cookie中查找购物车
            List<CartInfo> cartListFromCookie = cartCookieHandler.getCartList(request);
            List<CartInfo> cartList = null;
            if (cartListFromCookie!=null && cartListFromCookie.size()>0){
                // 合并
                cartList=cartService.mergeToCartList(cartListFromCookie,userId);
                // 删除cookie中的购物车
                cartCookieHandler.deleteCartCookie(request,response);
            }else{
                // 从redis中取得，或者从数据库中
                cartList= cartService.getCartList(userId);
            }
            request.setAttribute("cartList",cartList);
        }else{
            List<CartInfo> cartList = cartCookieHandler.getCartList(request);
            request.setAttribute("cartList",cartList);
        }
        return "cartList";
    }


    @RequestMapping("/checkCart")
    @ResponseBody
    @LoginRequire(autoRedirect = false)
    public void checkCart(HttpServletRequest request,HttpServletResponse response){
        String skuId = request.getParameter("skuId");
        String isChecked = request.getParameter("isChecked");
        String userId = (String) request.getAttribute("userId");
        if(userId!=null){
            cartService.checkCart(skuId,isChecked,userId);
        }else{
            cartCookieHandler.checkCart(request,response,skuId,isChecked);
        }

    }


    @RequestMapping("/toTrade")
    @LoginRequire(autoRedirect = true)
    public String toTrade(HttpServletRequest request,HttpServletResponse response){
        String userId = (String) request.getAttribute("userId");
        List<CartInfo> cookieHandlerCartList = cartCookieHandler.getCartList(request);
        if(cookieHandlerCartList!=null&&cookieHandlerCartList.size()>0){
            cartService.mergeToCartList(cookieHandlerCartList,userId);
            cartCookieHandler.deleteCartCookie(request, response);
        }
        return "redirect://order.gmall.com/trade";
    }


}
