package com.atguigu.gmall0508.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0508.bean.UserInfo;
import com.atguigu.gmall0508.passport.config.JwtUtil;
import com.atguigu.gmall0508.service.UserInfoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {

    @Reference
    private UserInfoService userInfoService ;
    @Value("${token.key}")
    private String signKey;

    @RequestMapping("/index")
    public String index(HttpServletRequest request){
        String originUrl = request.getParameter("originUrl");
        request.setAttribute("originUrl",originUrl);
        return "index";
    }

    @RequestMapping("/login")
    @ResponseBody
    public String login(HttpServletRequest request,UserInfo userInfo){
        // 取得ip地址
        String remoteAddr  = request.getHeader("X-forwarded-for");
        //System.out.println("=================="+remoteAddr);
        if(userInfo!=null){
            UserInfo loginUser = userInfoService.login(userInfo);
            if(loginUser==null){
                return "fail";
            }else {
                //生成token
                Map<String,Object> map=new HashMap<>();
                map.put("userId",loginUser.getId());
                map.put("nickName",loginUser.getNickName());
                String encode = JwtUtil.encode(signKey, map, remoteAddr);
                return encode;
            }
        }
        return "fail";
    }

    @RequestMapping("/verify")
    @ResponseBody
    public String verify(HttpServletRequest request){
        String token = request.getParameter("token");
        String currentIp = request.getParameter("currentIp");

        Map<String, Object> decode = JwtUtil.decode(token, signKey, currentIp);
        if(decode!=null){
            String userId = (String) decode.get("userId");
            UserInfo userInfo=userInfoService.verify(userId);
            if(userInfo!=null){
                return "success";
            }else{
                return "fail";
            }
        }else{
            return "fail";
        }
    }
}
