package com.distribution.insurance.review.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "review")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "review_type")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public abstract class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime reviewedAt;

    @Enumerated(EnumType.STRING)
    private ReviewResult result;

    @Column(length = 500)
    private String comment;

    protected void recordResult(ReviewResult result, String comment) {
        this.result = result;
        this.comment = comment;
        this.reviewedAt = LocalDateTime.now();
    }
}
