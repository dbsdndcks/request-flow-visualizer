package io.github.wooongchan.requestflow.integration.fixture;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * healthy-plate의 JwtAuthenticationFilter(OncePerRequestFilter 상속)를 재현하는 픽스처.
 * GenericFilterBean#init()이 final이라 CGLIB 프록시가 이 메서드를 가로채지 못하고, 그 결과
 * 프록시 인스턴스의 필드가 초기화되지 않아 필터 시작 단계에서 NPE가 나는 실제 회귀가 있었다.
 */
@Component
public class FilterLikeComponent extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        chain.doFilter(request, response);
    }
}
