package com.zjy.shop.mq;

import com.alibaba.fastjson.JSON;
import com.zjy.api.UserService;
import com.zjy.constant.ShopCode;
import com.zjy.entity.MQEntity;
import com.zjy.shop.pojo.TradeUserMoneyLog;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.Date;

@Slf4j
@Component
@RocketMQMessageListener(topic = "${mq.order.topic}", consumerGroup = "${mq.order.consumer.group.name}",
        messageModel = MessageModel.BROADCASTING)
public class CancelMQListener implements RocketMQListener<MessageExt> {


    @Resource
    private UserService userService;

    @Override
    public void onMessage(MessageExt messageExt) {
        try {
            //解析消息
            MQEntity mqEntity = JSON.parseObject(new String(messageExt.getBody(), "UTF-8"), MQEntity.class);
            log.info("接收到消息");
            if (mqEntity.getUserMoney() == null && mqEntity.getUserMoney().compareTo(BigDecimal.ZERO) == 1) return;
            TradeUserMoneyLog userMoneyLog = new TradeUserMoneyLog();
            userMoneyLog.setUserId(mqEntity.getUserId());
            userMoneyLog.setUseMoney(mqEntity.getUserMoney());
            userMoneyLog.setMoneyLogType(ShopCode.SHOP_USER_MONEY_REFUND.getCode());
            userMoneyLog.setOrderId(mqEntity.getOrderId());
            userMoneyLog.setCreateTime(new Date());
            userService.updateMoneyPaid(userMoneyLog);
            log.info("余额回退成功");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            log.info("回退余额失败");
        }
        //调用业务层进行余额修改
    }
}