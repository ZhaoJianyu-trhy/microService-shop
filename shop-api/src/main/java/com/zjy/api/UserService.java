package com.zjy.api;

import com.zjy.entity.Result;
import com.zjy.shop.pojo.TradeUser;
import com.zjy.shop.pojo.TradeUserMoneyLog;

public interface UserService {

    TradeUser findOne(Long userId);

    Result updateMoneyPaid(TradeUserMoneyLog userMoneyLog);
}
