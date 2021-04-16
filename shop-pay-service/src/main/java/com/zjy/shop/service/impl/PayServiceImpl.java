package com.zjy.shop.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.zjy.api.PayService;
import com.zjy.constant.ShopCode;
import com.zjy.entity.Result;
import com.zjy.exception.CastException;
import com.zjy.shop.mapper.TradeMqProducerTempMapper;
import com.zjy.shop.mapper.TradePayMapper;
import com.zjy.shop.pojo.TradeMqProducerTemp;
import com.zjy.shop.pojo.TradePay;
import com.zjy.shop.pojo.TradePayExample;
import com.zjy.utils.IDWorker;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.concurrent.ThreadPoolExecutor;


@Slf4j
@Component
@Service(interfaceClass = PayService.class)
public class PayServiceImpl implements PayService {

    @Resource
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Resource
    private TradeMqProducerTempMapper mqProducerTempMapper;

    @Resource
    private TradePayMapper payMapper;

    @Resource
    private IDWorker idWorker;

    @Value("${rocketmq.producer.group}")
    private String groupName;

    @Value("${mq.topic}")
    private String topic;

    @Value("${mq.pay.tag}")
    private String tag;

    @Override
    public Result createPayment(TradePay tradePay) {
        if (tradePay == null || tradePay.getOrderId() == null) {
            CastException.cast(ShopCode.SHOP_REQUEST_PARAMETER_VALID);
        }
        //判断订单支付状况
        TradePayExample example = new TradePayExample();
        TradePayExample.Criteria criteria = example.createCriteria();
        criteria.andOrderIdEqualTo(tradePay.getOrderId());
        criteria.andIsPaidEqualTo(ShopCode.SHOP_PAYMENT_IS_PAID.getCode());
        int r = payMapper.countByExample(example);
        if (r > 0) {
            CastException.cast(ShopCode.SHOP_PAYMENT_IS_PAID);
        }
        //设置订单状态未支付
        tradePay.setIsPaid(ShopCode.SHOP_ORDER_PAY_STATUS_NO_PAY.getCode());
        tradePay.setPayId(idWorker.nextId());
        //保存支付订单
        payMapper.insert(tradePay);
        return new Result(ShopCode.SHOP_SUCCESS.getSuccess(), ShopCode.SHOP_SUCCESS.getMessage());
    }

    @Override
    public Result callBackPayment(TradePay tradePay) throws MQBrokerException, RemotingException, InterruptedException, MQClientException {
        //判断用户支付的状态
        log.info("支付回调");
        if (tradePay.getIsPaid().intValue() == ShopCode.SHOP_ORDER_PAY_STATUS_IS_PAY.getCode().intValue()) {
            //更新支付订单的状态为已支付
            Long payId = tradePay.getPayId();
            TradePay pay = payMapper.selectByPrimaryKey(payId);
            if (pay == null) {
                CastException.cast(ShopCode.SHOP_PAYMENT_NOT_FOUND);
            }

            pay.setIsPaid(ShopCode.SHOP_ORDER_PAY_STATUS_IS_PAY.getCode());
            int r = payMapper.updateByPrimaryKeySelective(pay);
            log.info("订单支付状态改为已支付");
            if (r == 1) {
                //创建支付成功的消息
                TradeMqProducerTemp mqProducerTemp = new TradeMqProducerTemp();
                mqProducerTemp.setId(String.valueOf(idWorker.nextId()));
                mqProducerTemp.setGroupName(groupName);
                mqProducerTemp.setMsgTag(tag);
                mqProducerTemp.setMsgTopic(topic);
                mqProducerTemp.setMsgKey(String.valueOf(tradePay.getPayId()));
                mqProducerTemp.setMsgBody(JSON.toJSONString(tradePay));
                mqProducerTemp.setCreateTime(new Date());
                //将消息持久化到数据库
                mqProducerTempMapper.insert(mqProducerTemp);
                log.info("将支付成功消息持久到数据库");
                //发送消息
                //在线程池中进行处理
                threadPoolTaskExecutor.submit(new Runnable() {
                    @Override
                    public void run() {
                        SendResult sendResult = null;
                        try {
                            sendResult = sendMessage(topic, tag, String.valueOf(tradePay.getPayId()), JSON.toJSONString(tradePay));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (sendResult.getSendStatus() == SendStatus.SEND_OK) {
                            //如果MQ接收到消息，删除成功发送的消息
                            log.info("消息发送成功");
                            mqProducerTempMapper.deleteByPrimaryKey(mqProducerTemp.getId());
                            log.info("数据库备份消息删除");
                        }
                    }
                });
            }
            return new Result(ShopCode.SHOP_SUCCESS.getSuccess(), ShopCode.SHOP_SUCCESS.getMessage());
        } else {
            CastException.cast(ShopCode.SHOP_PAYMENT_PAY_ERROR);
            return new Result(ShopCode.SHOP_FAIL.getSuccess(), ShopCode.SHOP_FAIL.getMessage());
        }
    }

    /**
     * 发送支付成功的消息
     *
     * @param topic
     * @param tag
     * @param key
     * @param body
     */
    private SendResult sendMessage(String topic, String tag, String key, String body) throws MQBrokerException, RemotingException, InterruptedException, MQClientException {
        if (StringUtils.isEmpty(topic)) {
            CastException.cast(ShopCode.SHOP_MQ_TOPIC_IS_EMPTY);
        }
        if (StringUtils.isEmpty(body)) {
            CastException.cast(ShopCode.SHOP_MQ_MESSAGE_BODY_IS_EMPTY);
        }
        Message message = new Message(topic, tag, key, body.getBytes());
        SendResult sendResult = rocketMQTemplate.getProducer().send(message);
        return sendResult;
    }
}
