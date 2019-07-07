package com.atguigu.gmall0508.usermanage.serviceimpl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall0508.bean.UserAddress;
import com.atguigu.gmall0508.bean.UserInfo;
import com.atguigu.gmall0508.service.UserAddressService;
import com.atguigu.gmall0508.usermanage.mapper.UserAddressMapper;
import org.springframework.beans.factory.annotation.Autowired;


import java.util.List;
@Service
public class UserAddressServiceImpl implements UserAddressService {

   @Autowired
    private UserAddressMapper userAddressMapper;
    
    @Override
    public List<UserAddress> getUserAddressList(String userId) {
        UserAddress userAddress = new UserAddress();
        userAddress.setUserId(userId);
        return userAddressMapper.select(userAddress);
    }
}
