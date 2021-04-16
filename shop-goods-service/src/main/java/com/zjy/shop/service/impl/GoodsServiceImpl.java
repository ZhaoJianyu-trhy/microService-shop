package com.zjy.shop.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.zjy.api.GoodsService;
import com.zjy.constant.ShopCode;
import com.zjy.entity.Result;
import com.zjy.exception.CastException;
import com.zjy.shop.mapper.TradeGoodsMapper;
import com.zjy.shop.mapper.TradeGoodsNumberLogMapper;
import com.zjy.shop.pojo.TradeGoods;
import com.zjy.shop.pojo.TradeGoodsNumberLog;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;

@Component
@Service(interfaceClass = GoodsService.class)
public class GoodsServiceImpl implements GoodsService {

    @Resource
    private TradeGoodsMapper goodsMapper;

    @Resource
    private TradeGoodsNumberLogMapper goodsNumberLogMapper;

    @Override
    public TradeGoods findOne(Long goodsId) {
        if (goodsId == null) {
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        return goodsMapper.selectByPrimaryKey(goodsId);
    }

    @Override
    public Result reduceGoodsNum(TradeGoodsNumberLog goodsNumberLog) {
        if (goodsNumberLog == null || goodsNumberLog.getGoodsNumber() == null || goodsNumberLog.getOrderId() == null
            || goodsNumberLog.getGoodsId() == null || goodsNumberLog.getGoodsNumber().intValue() <= 0) {
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        TradeGoods goods = goodsMapper.selectByPrimaryKey(goodsNumberLog.getGoodsId());
        if (goods.getGoodsNumber() < goodsNumberLog.getGoodsNumber()) {
            CastException.cast(ShopCode.SHOP_GOODS_NUM_NOT_ENOUGH);
        }
        //减库存
        goods.setGoodsNumber(goods.getGoodsNumber() - goodsNumberLog.getGoodsNumber());
        goodsMapper.updateByPrimaryKey(goods);

        //日志记录
        goodsNumberLog.setGoodsNumber(-(goodsNumberLog.getGoodsNumber()));
        goodsNumberLog.setLogTime(new Date());
        goodsNumberLogMapper.insert(goodsNumberLog);
        return new Result(ShopCode.SHOP_SUCCESS.getSuccess(), ShopCode.SHOP_SUCCESS.getMessage());
    }
}
