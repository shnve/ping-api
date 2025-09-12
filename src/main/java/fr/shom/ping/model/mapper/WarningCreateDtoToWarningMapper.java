package fr.shom.ping.model.mapper;

import fr.shom.ping.model.Warning;
import fr.shom.ping.model.dto.WarningCreateDto;
import java.time.Instant;
import java.util.function.Function;

import static java.time.temporal.ChronoUnit.MINUTES;

public class WarningCreateDtoToWarningMapper implements Function<WarningCreateDto, Warning> {
    @Override
    public Warning apply(WarningCreateDto warningCreateDto) {
        return Warning.builder()
            .status(warningCreateDto.getStatus())
            .warningNumber(warningCreateDto.getWarningNumber())
            .year(warningCreateDto.getYear())
            .warningHazardTypeGeneral(warningCreateDto.getTypeGeneral())
            .estimatedPublicationDate(warningCreateDto.getEstimatedPublicationDate())
            .reviewDate(warningCreateDto.getReviewDate())
            .cancellationDate(warningCreateDto.getCancellationTime() != null ? warningCreateDto.getCancellationTime().truncatedTo(MINUTES) : null)
            .addCancellationTimeToTelex(warningCreateDto.isAddCancellationTimeToTelex())
            .internalComment(warningCreateDto.getInternalComment())
            .lastUpdate(Instant.now())
            .telex(warningCreateDto.getTelex())
            .navtex(warningCreateDto.getNavtex())
            .navtexConfig(warningCreateDto.getNavtexConfig())
            .build();
    }
}
