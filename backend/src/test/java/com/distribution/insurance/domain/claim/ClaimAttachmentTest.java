package com.distribution.insurance.domain.claim;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ClaimAttachmentTest {

    @Test
    void 첨부메타는_파일명_타입_크기_저장경로를_보존한다() {
        ClaimAttachment a = new ClaimAttachment("receipt.pdf", "application/pdf", 2048L, "/up/1/uuid_receipt.pdf");
        assertThat(a.getFilename()).isEqualTo("receipt.pdf");
        assertThat(a.getContentType()).isEqualTo("application/pdf");
        assertThat(a.getSizeBytes()).isEqualTo(2048L);
        assertThat(a.getStoredPath()).isEqualTo("/up/1/uuid_receipt.pdf");
    }
}
