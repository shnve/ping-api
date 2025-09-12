package fr.shom.ping.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.stream.Stream;
import org.grad.secom.core.models.enums.ContainerTypeEnum;

public class ContainerTypeEnumValidator implements ConstraintValidator<ValidContainerTypeEnum, Integer> {
    private List<Integer> acceptedValues;

    @Override
    public void initialize(ValidContainerTypeEnum annotation) {
        acceptedValues = Stream.of(ContainerTypeEnum.values())
            .map(ContainerTypeEnum::getValue)
            .toList();
    }

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        return value == null || acceptedValues.contains(value);
    }
}
