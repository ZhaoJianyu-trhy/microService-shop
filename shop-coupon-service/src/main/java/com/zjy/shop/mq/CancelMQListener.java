package com.zjy.shop.mq;

import com.alibaba.fastjson.JSON;
import com.zjy.constant.ShopCode;
import com.zjy.entity.MQEntity;
import com.zjy.shop.mapper.TradeCouponMapper;
import com.zjy.shop.pojo.TradeCoupon;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;

@Slf4j
@Component
@RocketMQMessageListener(topic = "${mq.order.topic}", consumerGroup = "${mq.order.consumer.group.name}",
                         messageModel = MessageModel.BROADCASTING)
public class CancelMQListener implements RocketMQListener<MessageExt> {

    @Resource
    private TradeCouponMapper couponMapper;

    @Override
    public void onMessage(MessageExt messageExt) {
        //解析消息
        MQEntity mqEntity = null;
        try {
            mqEntity = JSON.parseObject(new String(messageExt.getBody(), "UTF-8"), MQEntity.class);
            log.info("接收到消息");
            if (mqEntity.getCouponId() == null) return;
            TradeCoupon coupon = couponMapper.selectByPrimaryKey(mqEntity.getCouponId());
            coupon.setIsUsed(ShopCode.SHOP_COUPON_UNUSED.getCode());
            coupon.setUsedTime(null);
            couponMapper.updateByPrimaryKey(coupon);
            log.info("回退优惠券成功");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            log.error("回退优惠券失败");
        }
    }
}
