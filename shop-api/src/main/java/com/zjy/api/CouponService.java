package com.zjy.api;

import com.zjy.entity.Result;
import com.zjy.shop.pojo.TradeCoupon;

/**
 * 优惠券接口
 */
public interface CouponService {

    /**
     * 查询优惠券对象
     * @param couponId
     * @return
     */
    TradeCoupon findOne(Long couponId);

    /**
     * 更新优惠券状态
     * @param coupon
     * @return
     */
    Result updateCouponStatus(TradeCoupon coupon);
}
