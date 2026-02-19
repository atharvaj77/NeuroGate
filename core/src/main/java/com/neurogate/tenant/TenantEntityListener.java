package com.neurogate.tenant;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

public class TenantEntityListener {

    @PrePersist
    @PreUpdate
    public void setOrgId(TenantScopedEntity entity) {
        if (entity.getOrgId() == null || entity.getOrgId().isBlank()) {
            entity.setOrgId(TenantContext.getCurrentOrgIdOrDefault());
        }
    }
}
