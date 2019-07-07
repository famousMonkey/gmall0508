package com.atguigu.gmall0508.manage.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ManageController {

    @RequestMapping("/index")
    public String index(){
        return "index";
    }

}
