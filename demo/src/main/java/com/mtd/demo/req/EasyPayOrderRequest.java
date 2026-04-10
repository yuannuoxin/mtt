package com.mtd.demo.req;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.mtd.common.core.request.BaseRequest;

/**
 * 当面付下单请求参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EasyPayOrderRequest extends BaseRequest {

    /**
     * 商户订单号（唯一）
     */
    private String outTradeNo;

    /**
     * 订单标题
     */
    private String subject;

    /**
     * 订单金额（单位：元）
     */
    private Double totalAmount;

    /**
     * 付款码（扫码支付场景）
     */
    private String authCode;

    /**
     * 用户 ID（可选）
     */
    private String userId;

    /**
     * 商品描述（可选）
     */
    private String body;
}
