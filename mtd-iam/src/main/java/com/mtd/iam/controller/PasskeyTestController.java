package com.mtd.iam.controller;

import cn.hutool.core.codec.Base64Encoder;
import com.mtd.iam.resp.tmp.WebAuthnLoginRequest;
import com.mtd.iam.resp.tmp.WebAuthnRegisterRequest;
import com.mtd.iam.util.PasskeyHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/passkeyTest")
@RequiredArgsConstructor
public class PasskeyTestController {
    private final Map<String, String> store = new HashMap<>();

    @GetMapping("/register/options")
    public Map<String, Object> regOpt() {
        return Map.of(
                "challenge", Base64Encoder.encodeUrlSafe(PasskeyHelper.generateChallenge()),
                "rpId", "localhost",
                "rpName", "Demo",
                "userId", Base64Encoder.encodeUrlSafe("1001".getBytes()),
                "userName", "user@demo.com",
                "userDisplayName", "测试用户"
        );
    }

    @PostMapping("/register")
    public Map<String, Object> reg(@RequestBody WebAuthnRegisterRequest req) {
        store.put(req.getCredentialId(), req.getPublicKey());
        return Map.of("success", true);
    }

    @GetMapping("/login/options")
    public Map<String, Object> loginOpt() {
        return Map.of(
                "challenge", Base64Encoder.encodeUrlSafe(PasskeyHelper.generateChallenge()),
                "rpId", "localhost"
        );
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody WebAuthnLoginRequest req) throws Exception {
        String pk = store.get(req.getCredentialId());
        boolean ok = PasskeyHelper.verifySignature(
                pk,
                req.getAuthenticatorData(),
                req.getClientDataJSON(),
                req.getSignature()
        );
        if (!ok) throw new RuntimeException("验签失败");
        return Map.of("success", true, "token", "LOGIN_TOKEN_" + System.currentTimeMillis());
    }
}
