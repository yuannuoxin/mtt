package com.mtd.demo.response;

import lombok.Data;

/**
 * 当面付下单响应结果
 */
@Data
public class EasyPayOrderResponse {

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
     */
    private String tradeStatus;

    /**
     * 订单金额
     */
    private Double totalAmount;

    /**
     * 付款码（扫码支付场景返回）
     */
    private String authCode;

    /**
     * 二维码内容（生成二维码场景返回）
     */
    private String qrCode;

    /**
     * 支付链接（用于生成二维码）
     */
    private String payUrl;
}
