package org.fyp.tmssep490be.config;

import org.fyp.tmssep490be.entities.enums.UserStatus;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Factory to create SecurityContext with UserPrincipal for controller tests.
 */
public class WithMockUserPrincipalSecurityContextFactory
        implements WithSecurityContextFactory<WithMockUserPrincipal> {

    @Override
    public SecurityContext createSecurityContext(WithMockUserPrincipal annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        // Create UserPrincipal with specified ID using constructor
        UserPrincipal principal = new UserPrincipal(
                annotation.id(),
                annotation.username() + "@example.com", // email
                "password", // passwordHash - not used in tests
                annotation.username(), // fullName
                UserStatus.ACTIVE,
                Arrays.stream(annotation.roles())
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList())
        );

        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal,
                principal.getPassword(),
                principal.getAuthorities()
        );

        context.setAuthentication(auth);
        return context;
    }
}
