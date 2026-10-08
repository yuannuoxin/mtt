package com.mtd.tt.model.dto;

/**
 * 三方登录用户信息（provider 无关的通用视图），
 * 由各 provider 的 exchange 方法统一转换，供登录/自动注册/绑定复用。
 */
public record OAuthProviderUser(String id, String email, String name, String avatar) {
}
