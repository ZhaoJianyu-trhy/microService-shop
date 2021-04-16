package com.zjy.api;

import com.zjy.entity.Result;
import com.zjy.shop.pojo.TradeOrder;

public interface OrderService {

    /**
     * 下单接口
     * @param order
     * @return
     */
    Result confirmOrder(TradeOrder order);

}
