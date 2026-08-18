package com.phraseforge.phraseforge_api.favorite;

import com.phraseforge.phraseforge_api.common.PagedResponse;
import com.phraseforge.phraseforge_api.phrase.dto.PhraseSummaryResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/favorites")
@SecurityRequirement(name = "bearerAuth")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public PagedResponse<PhraseSummaryResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            Pageable pageable) {
        return favoriteService.list(userId(jwt), pageable);
    }

    @PutMapping("/{phraseId}")
    public ResponseEntity<Void> add(@AuthenticationPrincipal Jwt jwt, @PathVariable Long phraseId) {
        favoriteService.add(userId(jwt), phraseId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{phraseId}")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal Jwt jwt, @PathVariable Long phraseId) {
        favoriteService.remove(userId(jwt), phraseId);
        return ResponseEntity.noContent().build();
    }

    private Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
