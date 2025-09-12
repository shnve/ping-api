package fr.shom.ping.validation;

import fr.shom.ping.model.dto.GeoregulationPartCreateDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidGeoregulationPartRestrictionValidator implements ConstraintValidator<ValidGeoregulationPartRestriction, GeoregulationPartCreateDto> {
    protected static final String FIELD_ERROR_MESSAGE = "validation.invalid-georegulation-part-restriction";

    @Override
    public boolean isValid(GeoregulationPartCreateDto georegulationPartCreateDto, ConstraintValidatorContext constraintValidatorContext) {
        return this.isRestrictionLengthValid(georegulationPartCreateDto) && this.isRestrictionTonnageValid(georegulationPartCreateDto);
    }

    private boolean isRestrictionLengthValid(GeoregulationPartCreateDto georegulationPartCreateDto) {
        boolean allRestrictionLengthNull = georegulationPartCreateDto.getLengthVessel() == null && georegulationPartCreateDto.getChoiceLengthVessel() == null;
        boolean allRestrictionLengthNotNull =
            georegulationPartCreateDto.getLengthVessel() != null && georegulationPartCreateDto.getChoiceLengthVessel() != null;
        return allRestrictionLengthNotNull || allRestrictionLengthNull;
    }

    private boolean isRestrictionTonnageValid(GeoregulationPartCreateDto georegulationPartCreateDto) {
        boolean allRestrictionTonnageNull = georegulationPartCreateDto.getGrossTonnageVessel() == null
            && georegulationPartCreateDto.getChoiceGrossTonnageVessel() == null
            && georegulationPartCreateDto.getUnitsGrossTonnageVessel() == null;
        boolean allRestrictionTonnageNotNull = georegulationPartCreateDto.getGrossTonnageVessel() != null
            && georegulationPartCreateDto.getChoiceGrossTonnageVessel() != null
            && georegulationPartCreateDto.getUnitsGrossTonnageVessel() != null;
        return allRestrictionTonnageNotNull || allRestrictionTonnageNull;
    }
}
