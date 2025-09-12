package fr.shom.ping.model.mapper;

import fr.shom.ping.model.dto.WarningMapViewDto;
import fr.shom.ping.model.tuple.WarningMapViewAggregatedTuple;
import java.util.function.Function;

public class WarningMapViewTupleToWarningMapViewDto implements Function<WarningMapViewAggregatedTuple, WarningMapViewDto> {

    @Override
    public WarningMapViewDto apply(WarningMapViewAggregatedTuple warning) {
        WarningMapViewDto warningMapViewDto = new WarningMapViewDto();
        warningMapViewDto.setId(warning.getId());

        warningMapViewDto.setNameOfSeries(warning.getNameOfSeries());
        warningMapViewDto.setWarningNumber(warning.getWarningNumber());
        warningMapViewDto.setYear(warning.getYear());

        // If there is just one warningHazardTypeDetails, we use it as warning details
        if (warning.getWarningHazardTypeDetails().size() == 1) {
            warningMapViewDto.setWarningTypeDetails(warning.getWarningHazardTypeDetails().iterator().next().name());
        } else { // Otherwise, we use the general hazard type as warning details
            warningMapViewDto.setWarningTypeDetails(warning.getWarningHazardTypeGeneral().name());
        }

        warningMapViewDto.setGlobalWarningType(warning.getSerieType().getGlobalWarningType());
        warningMapViewDto.setSerieType(warning.getSerieType());
        warningMapViewDto.setPublicationTime(warning.getPublicationTime());
        warningMapViewDto.setGeometries(warning.getGeometries());
        warningMapViewDto.setAffectedAreas(warning.getAffectedAreas());

        return warningMapViewDto;
    }
}
