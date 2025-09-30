package com.todo.eod.web;

import com.todo.eod.app.FeatureFlagProvider;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class FlagController {

    private final FeatureFlagProvider provider;

    @PutMapping("/flags/{key}")
    public ResponseEntity<?> set(@PathVariable("key") String key, @RequestBody FlagReq req) {
        if (req == null || req.getPercentage() == null) return ResponseEntity.badRequest().body("missing percentage");
        provider.setPercentage(key, req.getPercentage());
        return ResponseEntity.ok(Map.of("key", key, "percentage", req.getPercentage()));
    }

    @GetMapping("/flags/{key}")
    public ResponseEntity<?> get(@PathVariable("key") String key) {
        return provider.getPercentage(key)
                .<ResponseEntity<?>>map(p -> ResponseEntity.ok(Map.of("key", key, "percentage", p)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Data
    public static class FlagReq { private Integer percentage; }
}

