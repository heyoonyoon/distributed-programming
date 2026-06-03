package com.distribution.insurance.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class JwtAuthenticationFilterTest {

    JwtTokenProvider provider = new JwtTokenProvider(
            "test-secret-key-please-change-this-to-a-long-enough-value-32byte!!", 3600000L);
    JwtAuthenticationFilter filter = new JwtAuthenticationFilter(provider);

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void 유효한_토큰이면_SecurityContext에_인증이_설정된다() throws Exception {
        String token = provider.createToken(7L, "POLICYHOLDER");
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, new MockHttpServletResponse(), chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(7L);
        assertThat(auth.getAuthorities().toString()).contains("ROLE_POLICYHOLDER");
    }

    @Test
    void 토큰이_없으면_인증이_설정되지_않는다() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        filter.doFilter(req, new MockHttpServletResponse(), mock(FilterChain.class));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
