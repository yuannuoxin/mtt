package com.mtd.tt.model.resp;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * OAuth 回调结果：登录模式下返回 login；绑定模式下返回 action=bound + 最新绑定列表。
 */
@Data
public class OAuthCallbackResp implements Serializable {
    /** login=登录成功；bound=绑定成功（已在登录态下执行绑定） */
    private String action;
    private LoginResp login;
    private List<OAuthBindingItem> bindings;
}
