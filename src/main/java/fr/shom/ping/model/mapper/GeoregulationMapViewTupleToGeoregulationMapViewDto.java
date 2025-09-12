package fr.shom.ping.model.mapper;

import fr.shom.ping.model.dto.GeoregulationMapViewDto;
import fr.shom.ping.model.tuple.GeoregulationMapViewAggregatedTuple;
import java.util.function.Function;

public class GeoregulationMapViewTupleToGeoregulationMapViewDto implements Function<GeoregulationMapViewAggregatedTuple, GeoregulationMapViewDto> {
    @Override
    public GeoregulationMapViewDto apply(GeoregulationMapViewAggregatedTuple georegulation) {
        return GeoregulationMapViewDto.builder()
            .id(georegulation.getId())
            .isNew(georegulation.isNew())
            .isTemporary(georegulation.isTemporary())
            .mainNumber(georegulation.getNumber())
            .features(georegulation.getFeatures())
            .publicationDate(georegulation.getPublicationDate())
            .build();
    }
}
