package com.zjy.shop.mq;

import com.alibaba.fastjson.JSON;
import com.zjy.constant.ShopCode;
import com.zjy.entity.MQEntity;
import com.zjy.shop.mapper.TradeGoodsMapper;
import com.zjy.shop.mapper.TradeGoodsNumberLogMapper;
import com.zjy.shop.mapper.TradeMqConsumerLogMapper;
import com.zjy.shop.pojo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.util.Date;

@Slf4j
@Component
@RocketMQMessageListener(topic = "${mq.order.topic}", consumerGroup = "${mq.order.consumer.group.name}",
        messageModel = MessageModel.BROADCASTING)
public class CancelMQListener implements RocketMQListener<MessageExt> {

    @Value("${mq.order.consumer.group.name}")
    private String groupName;

    @Resource
    private TradeMqConsumerLogMapper mqConsumerLogMapper;

    @Resource
    private TradeGoodsMapper goodsMapper;

    @Resource
    private TradeGoodsNumberLogMapper goodsNumberLogMapper;

    @Override
    public void onMessage(MessageExt messageExt) {
        //解析消息内容
        String msgId = null;
        String tags = null;
        String keys = null;
        String body = null;
        try {
            msgId = messageExt.getMsgId();
            tags = messageExt.getTags();
            keys = messageExt.getKeys();
            body = new String(messageExt.getBody(), "UTF-8");
            log.info("接收消息成功");

            //查询消费记录
            TradeMqConsumerLogKey key = new TradeMqConsumerLogKey();
            key.setGroupName(groupName);
            key.setMsgKey(keys);
            key.setMsgTag(tags);
            TradeMqConsumerLog mqConsumerLog = mqConsumerLogMapper.selectByPrimaryKey(key);

            if (mqConsumerLog != null) {//消费过
                //获得消费状态
                Integer status = mqConsumerLog.getConsumerStatus();
                //处理过了
                if (ShopCode.SHOP_MQ_MESSAGE_STATUS_SUCCESS.getCode().intValue() == status.intValue()) {
                    log.info("消息：" + msgId + " 已经处理成功");
                    return;
                }
                //正在处理
                if (ShopCode.SHOP_MQ_MESSAGE_STATUS_PROCESSING.getCode().intValue() == status.intValue()) {
                    log.info("消息：" + msgId + " 正在处理");
                    return;
                }
                //处理失败
                if (ShopCode.SHOP_MQ_MESSAGE_STATUS_FAIL.getCode().intValue() == status.intValue()) {
                    //获得已经处理过的次数
                    Integer consumerTimes = mqConsumerLog.getConsumerTimes();
                    if (consumerTimes > 3) {
                        log.info("消息：" + msgId + " 消息处理超过3次，停止处理");
                        return;
                    }
                    mqConsumerLog.setConsumerStatus(ShopCode.SHOP_MQ_MESSAGE_STATUS_PROCESSING.getCode());

                    //使用乐观锁更新
                    TradeMqConsumerLogExample example = new TradeMqConsumerLogExample();
                    TradeMqConsumerLogExample.Criteria criteria = example.createCriteria();
                    criteria.andMsgTagEqualTo(mqConsumerLog.getMsgTag());
                    criteria.andMsgKeyEqualTo(mqConsumerLog.getMsgKey());
                    criteria.andGroupNameEqualTo(groupName);
                    criteria.andConsumerTimesEqualTo(mqConsumerLog.getConsumerTimes());
                    int r = mqConsumerLogMapper.updateByExampleSelective(mqConsumerLog, example);
                    if (r <= 0) {
                        //未修改成功，由其他线程并发修改
                        log.info("并发修改，稍后处理");
                    }
                }
            } else {//如果没有消费过
                mqConsumerLog = new TradeMqConsumerLog();
                mqConsumerLog.setGroupName(groupName);
                mqConsumerLog.setMsgKey(keys);
                mqConsumerLog.setMsgTag(tags);
                mqConsumerLog.setConsumerStatus(ShopCode.SHOP_MQ_MESSAGE_STATUS_PROCESSING.getCode());
                mqConsumerLog.setMsgBody(body);
                mqConsumerLog.setMsgId(msgId);
                //失败的次数
                mqConsumerLog.setConsumerTimes(0);
                //入库
                mqConsumerLogMapper.insert(mqConsumerLog);
            }
            //回退库存
            MQEntity mqEntity = JSON.parseObject(body, MQEntity.class);
            Long goodsId = mqEntity.getGoodsId();
            TradeGoods tradeGoods = goodsMapper.selectByPrimaryKey(goodsId);
            tradeGoods.setGoodsNumber(tradeGoods.getGoodsNumber() + mqEntity.getGoodsNum());
            //入库更新
            goodsMapper.updateByPrimaryKey(tradeGoods);


            //记录消费日志
            TradeGoodsNumberLog goodsNumberLog = new TradeGoodsNumberLog();
            goodsNumberLog.setGoodsNumber(mqEntity.getGoodsNum());
            goodsNumberLog.setOrderId(mqEntity.getOrderId());
            goodsNumberLog.setLogTime(new Date());
            goodsNumberLog.setGoodsId(goodsId);
            goodsNumberLogMapper.insert(goodsNumberLog);


            //将消息的处理状态设置为成功
            mqConsumerLog.setConsumerStatus(ShopCode.SHOP_MQ_MESSAGE_STATUS_SUCCESS.getCode());
            mqConsumerLog.setConsumerTimestamp(new Date());
            mqConsumerLogMapper.updateByPrimaryKey(mqConsumerLog);
            log.info("回退库存成功");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            TradeMqConsumerLogKey key = new TradeMqConsumerLogKey();
            key.setMsgKey(keys);
            key.setMsgTag(tags);
            key.setGroupName(groupName);
            TradeMqConsumerLog mqConsumerLog = mqConsumerLogMapper.selectByPrimaryKey(key);
            if (mqConsumerLog == null) {
                //数据库没有记录
                mqConsumerLog = new TradeMqConsumerLog();
                mqConsumerLog.setGroupName(groupName);
                mqConsumerLog.setMsgKey(keys);
                mqConsumerLog.setMsgTag(tags);
                mqConsumerLog.setConsumerStatus(ShopCode.SHOP_MQ_MESSAGE_STATUS_FAIL.getCode());
                mqConsumerLog.setMsgBody(body);
                mqConsumerLog.setMsgId(msgId);
                mqConsumerLog.setConsumerTimes(1);
                mqConsumerLogMapper.insert(mqConsumerLog);
            } else {
                mqConsumerLog.setConsumerTimes(mqConsumerLog.getConsumerTimes() + 1);
                mqConsumerLogMapper.updateByPrimaryKey(mqConsumerLog);
            }
        }
    }
}