package org.fyp.tmssep490be.dtos.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private Long expiresIn;
    private Long userId;
    private String email;
    private String fullName;
    private String avatarUrl;
    private Set<String> roles;
    private List<BranchInfo> branches;
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BranchInfo {
        private Long id;
        private String name;
        private String code;
    }
}
