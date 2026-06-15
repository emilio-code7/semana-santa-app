package com.repertorio.common.tenant;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TenantContextTest {

    @Test
    void setAndGetReturnsSameValue() {
        TenantContext.set("hermandad-123");
        String result = TenantContext.get();
        assertThat(result).isEqualTo("hermandad-123");
        TenantContext.clear();
    }

    @Test
    void clearResetsToNull() {
        TenantContext.set("hermandad-456");
        TenantContext.clear();
        String result = TenantContext.get();
        assertThat(result).isNull();
    }

    @Test
    void valuesDoNotLeakAcrossThreads() throws InterruptedException {
        TenantContext.set("main-thread-value");

        AtomicReference<String> otherThreadValue = new AtomicReference<>("NOT_NULL");

        Thread otherThread = new Thread(() -> {
            otherThreadValue.set(TenantContext.get());
        });
        otherThread.start();
        otherThread.join();

        assertThat(otherThreadValue.get()).isNull();
        TenantContext.clear();
    }
}
