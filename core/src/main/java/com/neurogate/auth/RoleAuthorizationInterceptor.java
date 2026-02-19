package com.neurogate.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RoleAuthorizationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequiresRole requiresRole = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(),
                RequiresRole.class);
        if (requiresRole == null) {
            requiresRole = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequiresRole.class);
        }
        if (requiresRole == null) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Authentication is required");
        }

        Role currentRole = SecurityUtils.resolveRole(authentication).orElse(Role.VIEWER);
        if (!currentRole.hasAtLeast(requiresRole.value())) {
            throw new AccessDeniedException("Required role: " + requiresRole.value().name());
        }

        return true;
    }
}
