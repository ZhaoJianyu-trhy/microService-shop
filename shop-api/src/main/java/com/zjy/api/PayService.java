package com.zjy.api;

import com.zjy.entity.Result;
import com.zjy.shop.pojo.TradePay;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.remoting.exception.RemotingException;

public interface PayService {

    Result createPayment(TradePay tradePay);

    Result callBackPayment(TradePay tradePay) throws MQBrokerException, RemotingException, InterruptedException, MQClientException;
}
