package com.neurogate.tenant;

public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_ORG = new ThreadLocal<>();
    public static final String DEFAULT_ORG_ID = "default-org";

    private TenantContext() {
    }

    public static void setCurrentOrgId(String orgId) {
        CURRENT_ORG.set(orgId);
    }

    public static String getCurrentOrgId() {
        return CURRENT_ORG.get();
    }

    public static String getCurrentOrgIdOrDefault() {
        String orgId = CURRENT_ORG.get();
        return (orgId == null || orgId.isBlank()) ? DEFAULT_ORG_ID : orgId;
    }

    public static void clear() {
        CURRENT_ORG.remove();
    }
}
