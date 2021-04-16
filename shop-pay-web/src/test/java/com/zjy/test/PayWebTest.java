package com.zjy.test;

import com.zjy.constant.ShopCode;
import com.zjy.entity.Result;
import com.zjy.shop.PayWebApplication;
import com.zjy.shop.pojo.TradePay;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = PayWebApplication.class)
public class PayWebTest {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${shop.pay.baseURI}")
    private String baseURI;

    @Value("${shop.pay.createPayment}")
    private String createPaymentPath;

    @Value("${shop.pay.callbackPayment}")
    private String callBackPaymentPath;

    @Test
    public void createPayment(){
        long orderId = 580104793649975296L;
        TradePay tradePay = new TradePay();
        tradePay.setOrderId(orderId);
        tradePay.setPayAmount(new BigDecimal(4880));

        Result result = restTemplate.postForEntity(baseURI + createPaymentPath, tradePay, Result.class).getBody();
        System.out.println(result);
    }

    @Test
    public void callBackPayment(){
        long payId = 580106050770968576L;
        long orderId = 580104793649975296L;

        TradePay tradePay = new TradePay();
        tradePay.setPayId(payId);
        tradePay.setOrderId(orderId);
        tradePay.setIsPaid(ShopCode.SHOP_ORDER_PAY_STATUS_IS_PAY.getCode());
        Result result = restTemplate.postForEntity(baseURI + callBackPaymentPath, tradePay, Result.class).getBody();
        System.out.println(result);
    }


}
