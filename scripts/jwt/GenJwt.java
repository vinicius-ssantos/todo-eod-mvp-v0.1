import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Minimal HS256 JWT generator (no external deps)
 * Usage:
 *   javac scripts/jwt/GenJwt.java && java -cp scripts/jwt GenJwt \
 *     --secret change-me --sub dev --scope "tasks:* webhooks:ingest" --exp 3600
 */
public class GenJwt {
    public static void main(String[] args) throws Exception {
        Map<String,String> a = parse(args);
        String secret = a.getOrDefault("secret", System.getenv().getOrDefault("APP_JWT_SECRET", "change-me"));
        String sub = a.getOrDefault("sub", "dev");
        String scope = a.getOrDefault("scope", "tasks:* webhooks:ingest");
        int expSec = Integer.parseInt(a.getOrDefault("exp", a.getOrDefault("expSeconds", "3600")));
        long now = System.currentTimeMillis() / 1000L;

        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson = String.format("{\"sub\":\"%s\",\"scope\":\"%s\",\"iat\":%d,\"exp\":%d}", sub, scope, now, now + expSec);

        String encHeader = b64url(headerJson.getBytes(StandardCharsets.UTF_8));
        String encPayload = b64url(payloadJson.getBytes(StandardCharsets.UTF_8));
        String toSign = encHeader + "." + encPayload;
        String sig = hmacSha256Url(toSign, secret);
        System.out.println(toSign + "." + sig);
    }

    static String hmacSha256Url(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return b64url(raw);
    }

    static String b64url(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes)
                .replace("=", "")
                .replace("+", "-")
                .replace("/", "_");
    }

    static Map<String,String> parse(String[] args) {
        Map<String,String> map = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--")) {
                String k = args[i].substring(2);
                String v = (i + 1 < args.length && !args[i+1].startsWith("--")) ? args[++i] : "true";
                map.put(k, v);
            }
        }
        return map;
    }
}

