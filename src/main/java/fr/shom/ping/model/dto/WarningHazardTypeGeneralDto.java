package fr.shom.ping.model.dto;

import fr.shom.ping.model.AbstractWarningHazardType;
import fr.shom.ping.model.WarningHazardTypeGeneralEntity;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

@AllArgsConstructor
@SuperBuilder
public class WarningHazardTypeGeneralDto extends AbstractWarningHazardType {

    public static WarningHazardTypeGeneralDto from(WarningHazardTypeGeneralEntity entity) {

        return WarningHazardTypeGeneralDto.builder()
            .enumName(entity.getEnumName())
            .translation(entity.getTranslation())
            .internationalTranslation(entity.getInternationalTranslation())
            .build();

    }
}
