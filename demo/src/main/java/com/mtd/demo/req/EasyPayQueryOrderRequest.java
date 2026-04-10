package com.mtd.demo.req;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.mtd.common.core.request.BaseRequest;

/**
 * 当面付查询订单请求参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EasyPayQueryOrderRequest extends BaseRequest {

    /**
     * 商户订单号
     */
    private String outTradeNo;

    /**
     * 支付宝交易号（可选，优先使用商户订单号）
     */
    private String tradeNo;
}
