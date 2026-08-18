package com.phraseforge.phraseforge_api.user;

import com.phraseforge.phraseforge_api.auth.dto.UserResponse;
import com.phraseforge.phraseforge_api.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole());
    }
}
