package com.mtd.demo.controller;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.mtd.common.core.result.Result;
import com.mtd.demo.request.EasyPayOrderRequest;
import com.mtd.demo.request.EasyPayQueryOrderRequest;
import com.mtd.demo.response.EasyPayOrderResponse;
import com.mtd.demo.response.EasyPayQueryOrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当面付接口控制器
 */
@Slf4j
@Tag(name = "当面付", description = "支付宝当面付相关接口")
@RestController
@RequestMapping("/easyPay")
public class EasyPayController {

    @Value("${alipay.gateway-url}")
    private String gatewayUrl;

    @Value("${alipay.app-id}")
    private String appId;

    @Value("${alipay.private-key}")
    private String privateKey;

    @Value("${alipay.alipay-public-key}")
    private String alipayPublicKey;

    @Value("${alipay.sign-type:RSA2}")
    private String signType;

    @Value("${alipay.charset:utf-8}")
    private String charset;

    @Value("${alipay.format:json}")
    private String format;

    /**
     * 获取 AlipayClient
     */
    private AlipayClient getAlipayClient() {
        return new DefaultAlipayClient(
                gatewayUrl,
                appId,
                privateKey,
                format,
                charset,
                alipayPublicKey,
                signType
        );
    }

    @Operation(summary = "扫码支付预下单 (生成二维码)", 
               description = "用于生成支付二维码，用户扫码后完成支付")
    @PostMapping("/order/preCreate")
    public Result<EasyPayOrderResponse> preCreate(@RequestBody EasyPayOrderRequest request) {
        log.info("【接口调用】扫码支付预下单：{}", JSONUtil.toJsonStr(request));
        
        // 参数校验
        if (request.getOutTradeNo() == null || request.getOutTradeNo().isEmpty()) {
            request.setOutTradeNo(IdUtil.fastSimpleUUID());
            log.info("【当面付】未传入订单号，已自动生成：{}", request.getOutTradeNo());
        }
        if (request.getSubject() == null || request.getSubject().isEmpty()) {
            return Result.error(400, "订单标题不能为空");
        }
        if (request.getTotalAmount() == null || request.getTotalAmount() <= 0) {
            return Result.error(400, "订单金额必须大于 0");
        }

        try {
            AlipayClient alipayClient = getAlipayClient();
            
            // 构建预下单请求 - 使用通用 SDK 方式
            String bizContent = "{" +
                    "\"out_trade_no\":\"" + request.getOutTradeNo() + "\"," +
                    "\"subject\":\"" + request.getSubject() + "\"," +
                    "\"total_amount\":" + request.getTotalAmount() + "," +
                    (request.getBody() != null ? "\"body\":\"" + request.getBody() + "\"," : "") +
                    "\"timeout_express\":\"2m\"" +
                    "}";
            
            com.alipay.api.request.AlipayTradePrecreateRequest alipayRequest = 
                new com.alipay.api.request.AlipayTradePrecreateRequest();
            alipayRequest.setBizContent(bizContent);
            
            com.alipay.api.response.AlipayTradePrecreateResponse response = 
                alipayClient.execute(alipayRequest);

            // 检查响应
            if (!response.isSuccess()) {
                log.error("【当面付】预下单失败：{}", response.getSubMsg());
                return Result.error(Integer.valueOf(response.getCode()), response.getSubMsg());
            }

            log.info("【当面付】预下单成功，outTradeNo: {}, qrCode: {}", response.getOutTradeNo(), response.getQrCode());

            // 构建响应
            EasyPayOrderResponse result = new EasyPayOrderResponse();
            result.setOutTradeNo(response.getOutTradeNo());
            result.setQrCode(response.getQrCode());
            result.setPayUrl(response.getQrCode());

            return Result.success(result);
        } catch (AlipayApiException e) {
            log.error("【当面付】预下单异常", e);
            return Result.error(500, "下单异常：" + e.getMessage());
        }
    }

    @Operation(summary = "查询订单状态", 
               description = "根据商户订单号或支付宝交易号查询订单状态")
    @PostMapping("/order/query")
    public Result<EasyPayQueryOrderResponse> query(@RequestBody EasyPayQueryOrderRequest request) {
        log.info("【接口调用】查询订单：{}", JSONUtil.toJsonStr(request));

        // 参数校验
        if ((request.getOutTradeNo() == null || request.getOutTradeNo().isEmpty()) &&
            (request.getTradeNo() == null || request.getTradeNo().isEmpty())) {
            return Result.error(400, "商户订单号或支付宝交易号不能同时为空");
        }

        try {
            AlipayClient alipayClient = getAlipayClient();
            
            // 构建查询请求 - 使用通用 SDK 方式
            String bizContent = "{";
            if (request.getOutTradeNo() != null && !request.getOutTradeNo().isEmpty()) {
                bizContent += "\"out_trade_no\":\"" + request.getOutTradeNo() + "\"";
            } else {
                bizContent += "\"trade_no\":\"" + request.getTradeNo() + "\"";
            }
            bizContent += "}";
            
            com.alipay.api.request.AlipayTradeQueryRequest alipayRequest = 
                new com.alipay.api.request.AlipayTradeQueryRequest();
            alipayRequest.setBizContent(bizContent);

            com.alipay.api.response.AlipayTradeQueryResponse response = 
                alipayClient.execute(alipayRequest);

            // 检查响应
            if (!response.isSuccess()) {
                log.error("【当面付】查询订单失败：{}", response.getSubMsg());
                return Result.error(Integer.valueOf(response.getCode()), response.getSubMsg());
            }

            log.info("【当面付】查询订单成功，tradeNo: {}, tradeStatus: {}", 
                    response.getTradeNo(), response.getTradeStatus());

            // 构建响应
            EasyPayQueryOrderResponse result = new EasyPayQueryOrderResponse();
            result.setOutTradeNo(response.getOutTradeNo());
            result.setTradeNo(response.getTradeNo());
            result.setTradeStatus(response.getTradeStatus());
            result.setTotalAmount(Double.valueOf(response.getTotalAmount()));
            result.setReceiptAmount(Double.valueOf(response.getReceiptAmount()));
            result.setSendPayDate(response.getSendPayDate());
            result.setBuyerOpenId(response.getBuyerOpenId());
            result.setBuyerLogonId(response.getBuyerLogonId());

            return Result.success(result);
        } catch (AlipayApiException e) {
            log.error("【当面付】查询订单异常", e);
            return Result.error(500, "查询异常：" + e.getMessage());
        }
    }
}
