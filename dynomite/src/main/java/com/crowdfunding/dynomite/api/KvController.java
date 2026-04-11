package com.crowdfunding.dynomite.api;

import com.crowdfunding.dynomite.api.dto.KvGetResponse;
import com.crowdfunding.dynomite.api.dto.KvPutRequest;
import com.crowdfunding.dynomite.service.KvRouter;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/kv")
public class KvController {

    private final KvRouter router;

    public KvController(KvRouter router) {
        this.router = router;
    }

    @PutMapping("/{key}")
    public ResponseEntity<Void> put(@PathVariable String key, @Valid @RequestBody KvPutRequest request) {
        router.put(key, request);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{key}")
    public ResponseEntity<KvGetResponse> get(@PathVariable String key) {
        return router.get(key)
                .map(result -> ResponseEntity.ok(new KvGetResponse(result.key(), result.value(), result.servedBy())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> delete(@PathVariable String key) {
        boolean deleted = router.delete(key);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
