package com.mtd.demo.resp;

import lombok.Data;

/**
 * 当面付查询订单响应结果
 */
@Data
public class EasyPayQueryOrderResponse {

    /**
     * 商户订单号
     */
    private String outTradeNo;

    /**
     * 支付宝交易号
     */
    private String tradeNo;

    /**
     * 订单状态
     * SUCCESS - 支付成功
     * FAILED - 支付失败
     * WAIT_BUYER_PAY - 等待买家付款
     * TRADE_CLOSED - 交易关闭
     */
    private String tradeStatus;

    /**
     * 订单金额
     */
    private Double totalAmount;

    /**
     * 实收金额
     */
    private Double receiptAmount;

    /**
     * 付款时间
     */
    private java.util.Date sendPayDate;

    /**
     * 买家支付宝用户 ID(openid)
     */
    private String buyerOpenId;

    /**
     * 买家支付宝账号
     */
    private String buyerLogonId;
}
