package com.mtd.tt.model.dto;

/**
 * Telegram Login Widget 登录用户（签名数据已验证后）的 provider 无关视图。
 * Telegram 不返回邮箱/头像，name 由 first_name/last_name/username 拼接，email 由系统占位生成。
 */
public record TelegramUserInfo(String id, String name, String username) {
}
