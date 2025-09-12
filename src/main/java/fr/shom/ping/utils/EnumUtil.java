package fr.shom.ping.utils;

import fr.shom.ping.model.AbstractEnumTypes;
import fr.shom.ping.model.dto.EnumTypeWithTranslationDto;
import java.util.List;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;

public final class EnumUtil {
    public static final ModelMapper MODEL_MAPPER = new ModelMapper();

    private EnumUtil() {
    }

    public static List<EnumTypeWithTranslationDto> convertToEnumTypeWithTranslationDto(List<? extends AbstractEnumTypes> customEnumTypeList) {
        return customEnumTypeList.stream()
            .map(enumType -> MODEL_MAPPER.map(enumType, EnumTypeWithTranslationDto.class))
            .collect(Collectors.toList());
    }
}
