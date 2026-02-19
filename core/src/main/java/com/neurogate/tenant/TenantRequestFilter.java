package com.neurogate.tenant;

import com.neurogate.auth.SecurityUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TenantRequestFilter extends OncePerRequestFilter {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String orgId = SecurityUtils.getAuthentication()
                .flatMap(SecurityUtils::resolveOrgId)
                .orElseGet(() -> request.getHeader("X-Org-Id"));

        Session session = null;
        Filter tenantFilter = null;

        if (StringUtils.hasText(orgId)) {
            TenantContext.setCurrentOrgId(orgId);
            try {
                session = entityManager.unwrap(Session.class);
                tenantFilter = session.enableFilter("tenantFilter");
                tenantFilter.setParameter("orgId", orgId);
            } catch (RuntimeException ignored) {
                // No Hibernate session bound for this request path.
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            try {
                if (session != null && tenantFilter != null) {
                    session.disableFilter("tenantFilter");
                }
            } catch (RuntimeException ignored) {
                // Ignore cleanup failures on session lifecycle edges.
            }
            TenantContext.clear();
        }
    }
}
