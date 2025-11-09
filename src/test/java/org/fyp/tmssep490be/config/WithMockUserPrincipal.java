package org.fyp.tmssep490be.config;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Custom annotation to mock UserPrincipal for controller tests.
 * Usage: @WithMockUserPrincipal(id = 1L, username = "academic", roles = {"ACADEMIC_AFFAIR"})
 */
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockUserPrincipalSecurityContextFactory.class)
public @interface WithMockUserPrincipal {
    long id() default 1L;
    String username() default "user";
    String[] roles() default {"USER"};
}
