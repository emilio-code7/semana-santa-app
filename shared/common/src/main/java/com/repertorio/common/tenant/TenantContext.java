package com.repertorio.common.tenant;

public class TenantContext {

    private static final ThreadLocal<String> HERMANDAD_ID = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(String hermandadId) {
        HERMANDAD_ID.set(hermandadId);
    }

    public static String get() {
        return HERMANDAD_ID.get();
    }

    public static void clear() {
        HERMANDAD_ID.remove();
    }
}
