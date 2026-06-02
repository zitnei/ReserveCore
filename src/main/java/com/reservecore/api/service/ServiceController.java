package com.reservecore.api.service;

import com.reservecore.domain.service.ServiceCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * サービス（メニュー）API（店舗にぶら下がる）
 * POST /api/stores/{storeId}/services : サービス登録（ADMIN または 自店STAFF）
 * GET  /api/stores/{storeId}/services : サービス一覧（認証済みなら誰でも）
 */
@RestController
@RequestMapping("/api/stores/{storeId}/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceCatalogService serviceCatalogService;

    /** サービス登録（権限判定はサービス層で実施） */
    @PostMapping
    public ResponseEntity<ServiceResponse> createService(
            @PathVariable Long storeId,
            @Valid @RequestBody ServiceCreateRequest request,
            Authentication authentication) {
        ServiceResponse response =
                serviceCatalogService.createService(storeId, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** サービス一覧 */
    @GetMapping
    public ResponseEntity<List<ServiceResponse>> getServices(@PathVariable Long storeId) {
        return ResponseEntity.ok(serviceCatalogService.getServicesByStore(storeId));
    }
}
