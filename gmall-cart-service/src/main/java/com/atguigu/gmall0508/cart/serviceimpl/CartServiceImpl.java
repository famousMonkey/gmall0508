package com.atguigu.gmall0508.cart.serviceimpl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0508.bean.CartInfo;
import com.atguigu.gmall0508.bean.SkuInfo;
import com.atguigu.gmall0508.cart.constant.CartConst;
import com.atguigu.gmall0508.cart.mapper.CartInfoMapper;
import com.atguigu.gmall0508.config.RedisUtil;
import com.atguigu.gmall0508.service.CartService;
import com.atguigu.gmall0508.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.*;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Reference
    private ManageService manageService;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public void addToCart(String skuId, String userId, Integer skuNum) {
        CartInfo cartInfo=new CartInfo();
        cartInfo.setSkuId(skuId);
        cartInfo.setUserId(userId);
        cartInfo.setSkuNum(skuNum);
        CartInfo cartInfoExist = cartInfoMapper.selectOne(cartInfo);
        if(cartInfoExist!=null){
            //存在
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum()+skuNum);
            cartInfoExist.setSkuPrice(cartInfoExist.getCartPrice());
            cartInfoMapper.updateByPrimaryKeySelective(cartInfoExist);
        }else{
            //不存在
            SkuInfo skuInfo = manageService.getSkuInfoBySkuId(skuId);
            CartInfo cartInfo1=new CartInfo();
            cartInfo1.setSkuId(skuId);
            cartInfo1.setCartPrice(skuInfo.getPrice());
            cartInfo1.setSkuPrice(skuInfo.getPrice());
            cartInfo1.setSkuName(skuInfo.getSkuName());
            cartInfo1.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo1.setUserId(userId);
            cartInfo1.setSkuNum(skuNum);
            //插入到数据库
            cartInfoMapper.insertSelective(cartInfo1);
            cartInfoExist=cartInfo1;
        }
        //保存到redis缓存
        String userCartKey= CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        Jedis jedis = redisUtil.getJedis();
        String cartjson = JSON.toJSONString(cartInfoExist);
        jedis.hset(userCartKey,skuId,cartjson);
        String userInfoKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USERINFOKEY_SUFFIX;
        Long ttl = jedis.ttl(userInfoKey);
        jedis.expire(userCartKey,ttl.intValue());
        jedis.close();
    }

    @Override
    public List<CartInfo> getCartList(String userId) {
        //用户已登陆
        Jedis jedis = redisUtil.getJedis();
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        List<String> cartJsons  = jedis.hvals(userCartKey);
        if(cartJsons!=null&&cartJsons.size()>0){
            List<CartInfo> cartInfoList = new ArrayList<>();
            for (String cartJson : cartJsons) {
                CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
                cartInfoList.add(cartInfo);
            }
            //排序
            cartInfoList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    return o1.getSkuId().compareTo(o2.getId());
                }
            });

            return cartInfoList;
        }else{
            //从数据库中查询
            List<CartInfo> cartInfoList = loadCartCache(userId);
            return  cartInfoList;

        }
    }

    @Override
    public List<CartInfo> mergeToCartList(List<CartInfo> cartListFromCookie, String userId) {
        List<CartInfo> cartInfoListDB = cartInfoMapper.selectCartListWithCurPrice(userId);
        // 循环开始匹配
        for (CartInfo cartInfoCk : cartListFromCookie) {
            boolean isMatch =false;
            for (CartInfo cartInfoDB : cartInfoListDB) {
                if (cartInfoDB.getSkuId().equals(cartInfoCk.getSkuId())){
                    cartInfoDB.setSkuNum(cartInfoCk.getSkuNum()+cartInfoDB.getSkuNum());
                    cartInfoMapper.updateByPrimaryKeySelective(cartInfoDB);
                    isMatch = true;
                }
            }
            // 数据库中没有购物车，则直接将cookie中购物车添加到数据库
            if (!isMatch){
                cartInfoCk.setUserId(userId);
                cartInfoMapper.insertSelective(cartInfoCk);
            }
        }
        // 从新在数据库中查询并返回数据
        List<CartInfo> cartInfoList = loadCartCache(userId);

        //合并勾选的商品
        for (CartInfo cartInfoDB : cartInfoListDB) {
            for (CartInfo cartInfoCK : cartListFromCookie) {
                if(cartInfoDB.getSkuId().equals(cartInfoCK.getSkuId())){
                    if("1".equals(cartInfoCK.getIsChecked())){
                        cartInfoDB.setIsChecked(cartInfoCK.getIsChecked());
                        checkCart(cartInfoDB.getSkuId(),cartInfoCK.getIsChecked(),userId);
                    }
                }
            }
        }


        return cartInfoList;

    }

    @Override
    public void checkCart(String skuId, String isChecked, String userId) {
        Jedis jedis = redisUtil.getJedis();
       //获取购物车中的商品信息，更改选中状态
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        String cartJson = jedis.hget(userCartKey, skuId);
        CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
        cartInfo.setIsChecked(isChecked);
        String cartCheckdJson = JSON.toJSONString(cartInfo);
        jedis.hset(userCartKey,skuId,cartCheckdJson);
        //添加到已选中购物车
        String userCheckedKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CHECKED_KEY_SUFFIX;
        if("1".equals(isChecked)){
            jedis.hset(userCheckedKey,skuId,cartCheckdJson);
        }else{
            jedis.hdel(userCheckedKey,skuId);
        }
        jedis.close();
    }

    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        String userCheckedKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CHECKED_KEY_SUFFIX;
        Jedis jedis = redisUtil.getJedis();
        List<String> cartCheckedList = jedis.hvals(userCheckedKey);
        List<CartInfo> newCartList = new ArrayList<>();
        for (String cartJson : cartCheckedList) {
            CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
            newCartList.add(cartInfo);
        }
        return newCartList;
    }
    @Override
    public List<CartInfo> loadCartCache(String userId) {
        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCurPrice(userId);
        if(cartInfoList==null&&cartInfoList.size()==0){
            return null;
        }
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        Jedis jedis = redisUtil.getJedis();
        Map<String,String> map = new HashMap<>(cartInfoList.size());
        for (CartInfo cartInfo : cartInfoList) {
            String cartJson = JSON.toJSONString(cartInfo);
            map.put(cartInfo.getSkuId(),cartJson);
        }
        jedis.hmset(userCartKey,map);
        jedis.close();
        return  cartInfoList;

    }
}
