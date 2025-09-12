package fr.shom.ping.model.enums;

import java.time.LocalDate;
import java.time.Period;

public enum FolderReviewStatus {
    TO_REVIEW,
    OUTDATED,
    ACTIVE;

    public static FolderReviewStatus getByReviewDate(LocalDate reviewDate) {
        FolderReviewStatus status = FolderReviewStatus.ACTIVE;
        if(reviewDate == null) {
            return status;
        }

        LocalDate day = LocalDate.now();
        if(Period.between(day, reviewDate).isZero()) {
            status = FolderReviewStatus.TO_REVIEW;
        }
        if(Period.between(day, reviewDate).isNegative()) {
            status = FolderReviewStatus.OUTDATED;
        }
        return status;
    }
}
