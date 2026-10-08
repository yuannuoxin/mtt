package com.mtd.tt.model.resp;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 三方绑定总览：系统支持的三方（含是否启用/是否已绑定）+ 当前用户已绑定列表。
 */
@Data
public class OAuthBindingsResp implements Serializable {
    private List<OAuthProviderItem> providers;
    private List<OAuthBindingItem> bindings;
}
