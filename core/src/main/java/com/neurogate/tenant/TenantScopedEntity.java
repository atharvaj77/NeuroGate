package com.neurogate.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(TenantEntityListener.class)
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "orgId", type = String.class))
@Filter(name = "tenantFilter", condition = "org_id = :orgId")
public abstract class TenantScopedEntity {

    @Column(name = "org_id", nullable = false, length = 64)
    private String orgId;
}
