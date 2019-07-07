package com.atguigu.gmall0508.service;

import com.atguigu.gmall0508.bean.UserInfo;

import java.util.List;

public interface UserInfoService {

    public List<UserInfo> getAll();
    public UserInfo login(UserInfo userInfo);
    public UserInfo verify(String userId);
}
