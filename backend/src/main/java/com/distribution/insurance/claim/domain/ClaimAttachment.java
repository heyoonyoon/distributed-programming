package com.distribution.insurance.claim.domain;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Claim 증빙 1건의 메타(바이너리는 디스크, 엔티티는 메타만). */
@Embeddable
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ClaimAttachment {

    private String filename;
    private String contentType;
    private long sizeBytes;
    private String storedPath;

    public ClaimAttachment(String filename, String contentType, long sizeBytes, String storedPath) {
        this.filename = filename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.storedPath = storedPath;
    }
}
