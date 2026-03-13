package com.mtd.common.core.request;

import lombok.Data;
import java.io.Serializable;

/**
 * 统一请求参数基类
 * 所有 POST 请求的参数都应继承此类
 */
@Data
public abstract class BaseRequest implements Serializable {
}
