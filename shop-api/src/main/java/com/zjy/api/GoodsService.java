package com.zjy.api;

import com.zjy.entity.Result;
import com.zjy.shop.pojo.TradeGoods;
import com.zjy.shop.pojo.TradeGoodsNumberLog;

public interface GoodsService {


    /**
     * 根据id查询商品对象
     * @param goodsId
     * @return
     */
    TradeGoods findOne(Long goodsId);

    /**
     * 扣减库存
     * @param goodsNumberLog
     * @return
     */
    Result reduceGoodsNum(TradeGoodsNumberLog goodsNumberLog);
}
