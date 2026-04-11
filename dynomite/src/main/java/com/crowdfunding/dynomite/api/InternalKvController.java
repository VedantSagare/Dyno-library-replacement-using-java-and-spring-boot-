package com.crowdfunding.dynomite.api;

import com.crowdfunding.dynomite.api.dto.KvPutRequest;
import com.crowdfunding.dynomite.store.KeyValueStore;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/internal/kv")
public class InternalKvController {

    private final KeyValueStore store;

    public InternalKvController(KeyValueStore store) {
        this.store = store;
    }

    @PutMapping("/{key}")
    public ResponseEntity<Void> put(@PathVariable String key, @Valid @RequestBody KvPutRequest request) {
        store.put(key, request.getValue(), request.getTtlSeconds());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{key}")
    public ResponseEntity<String> get(@PathVariable String key) {
        return store.get(key)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> delete(@PathVariable String key) {
        boolean deleted = store.delete(key);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
