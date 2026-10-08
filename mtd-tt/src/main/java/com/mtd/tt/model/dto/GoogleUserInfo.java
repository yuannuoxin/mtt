package com.mtd.tt.model.dto;

/**
 * Google OAuth userinfo 结果（GET https://www.googleapis.com/oauth2/v2/userinfo）
 */
public record GoogleUserInfo(String id, String email, String name, String picture) {
}
