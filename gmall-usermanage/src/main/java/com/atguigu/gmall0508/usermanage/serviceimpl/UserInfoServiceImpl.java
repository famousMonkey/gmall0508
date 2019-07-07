package com.atguigu.gmall0508.usermanage.serviceimpl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0508.bean.UserInfo;
import com.atguigu.gmall0508.config.RedisUtil;
import com.atguigu.gmall0508.service.UserInfoService;
import com.atguigu.gmall0508.usermanage.mapper.UserInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import redis.clients.jedis.Jedis;


import java.util.List;
@Service
public class UserInfoServiceImpl implements UserInfoService {

    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private RedisUtil redisUtil;

    public String userKey_prefix="user:";
    public String userinfoKey_suffix=":info";
    public int userKey_timeOut=60*60*24;


    @Override
    public List<UserInfo> getAll() {
         return userInfoMapper.selectAll();
    }

    @Override
    public UserInfo login(UserInfo userInfo) {
        String password = DigestUtils.md5DigestAsHex(userInfo.getPasswd().getBytes());
        userInfo.setPasswd(password);
        UserInfo user = userInfoMapper.selectOne(userInfo);
        String userKey=userKey_prefix+user.getId()+userinfoKey_suffix;
        if(user!=null){
            Jedis jedis = redisUtil.getJedis();
            jedis.setex(userKey,userKey_timeOut, JSON.toJSONString(user));
            jedis.close();
            return user;
        }
        return null;
    }

    @Override
    public UserInfo verify(String userId) {
        Jedis jedis = redisUtil.getJedis();
        String userKey=userKey_prefix+userId+userinfoKey_suffix;
        String userJson = jedis.get(userKey);
        //重置用户时间
        jedis.expire(userKey,userKey_timeOut);

        if(userJson!=null){
            UserInfo userInfo = JSON.parseObject(userJson, UserInfo.class);
            return userInfo;
        }
        return null;
    }
}
