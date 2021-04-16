package com.zjy.shop.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.zjy.api.CouponService;
import com.zjy.constant.ShopCode;
import com.zjy.entity.Result;
import com.zjy.exception.CastException;
import com.zjy.shop.mapper.TradeCouponMapper;
import com.zjy.shop.pojo.TradeCoupon;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Service(interfaceClass = CouponService.class)
public class CouponServiceImpl implements CouponService {

    @Resource
    private TradeCouponMapper couponMapper;

    @Override
    public TradeCoupon findOne(Long couponId) {
        if(couponId ==null){
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        return couponMapper.selectByPrimaryKey(couponId);
    }

    @Override
    public Result updateCouponStatus(TradeCoupon coupon) {
        if (coupon == null) {
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        couponMapper.updateByPrimaryKey(coupon);
        return new Result(ShopCode.SHOP_SUCCESS.getSuccess(), ShopCode.SHOP_SUCCESS.getMessage());
    }
}
