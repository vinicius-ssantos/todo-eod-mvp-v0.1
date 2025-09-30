package com.todo.eod.web;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;

@Component
public class WebhookSecurity {

    @Value("${eod.webhooks.github.secret:}")
    private String githubSecret;

    @Value("${eod.webhooks.gitlab.secret:}")
    private String gitlabSecret;

    @PostConstruct
    void init() { }

    public boolean verifyGitHub(Map<String, String> headers, String body) {
        String sig256 = header(headers, "X-Hub-Signature-256");
        String sig1 = header(headers, "X-Hub-Signature");
        if (isBlank(githubSecret)) return false;
        if (!isBlank(sig256)) {
            String expected = "sha256=" + hmacHex(body, githubSecret, "HmacSHA256");
            return constantTimeEquals(expected, sig256);
        }
        if (!isBlank(sig1)) {
            String expected = "sha1=" + hmacHex(body, githubSecret, "HmacSHA1");
            return constantTimeEquals(expected, sig1);
        }
        return false;
    }

    public boolean verifyGitLab(Map<String, String> headers, String body) {
        if (isBlank(gitlabSecret)) return false;

        String sig = header(headers, "X-Gitlab-Signature");
        if (!isBlank(sig)) {
            String base64Hmac = hmacBase64(body, gitlabSecret, "HmacSHA256");
            if (constantTimeEquals(base64Hmac, sig)) return true;
            String hexHmac = hmacHex(body, gitlabSecret, "HmacSHA256");
            if (constantTimeEquals(hexHmac, sig) || constantTimeEquals("sha256=" + hexHmac, sig)) return true;
        }
        String token = header(headers, "X-Gitlab-Token");
        return !isBlank(token) && constantTimeEquals(token, gitlabSecret);
    }

    private String hmacHex(String data, String secret, String algo) {
        try {
            Mac mac = Mac.getInstance(algo);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algo));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) sb.append(String.format(Locale.ROOT, "%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String hmacBase64(String data, String secret, String algo) {
        try {
            Mac mac = Mac.getInstance(algo);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algo));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(raw);
        } catch (Exception e) {
            return "";
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private String header(Map<String, String> headers, String name) {
        if (headers == null) return null;
        for (var e : headers.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(name)) return e.getValue();
        }
        return null;
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }
}
