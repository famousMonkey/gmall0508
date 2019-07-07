package com.atguigu.gmall0508.service;

import com.atguigu.gmall0508.bean.UserAddress;

import java.util.List;

public interface UserAddressService {

    public List<UserAddress> getUserAddressList(String userId);
}
