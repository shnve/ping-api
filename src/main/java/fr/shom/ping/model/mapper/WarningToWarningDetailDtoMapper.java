package fr.shom.ping.model.mapper;

import fr.shom.ping.model.Warning;
import fr.shom.ping.model.dto.AbstractWarningDetailDto;
import fr.shom.ping.model.dto.WarningDetailDto;
import java.util.function.Function;

public class WarningToWarningDetailDtoMapper implements Function<Warning, WarningDetailDto> {

    @Override
    public WarningDetailDto apply(Warning warning) {
        WarningDetailDto warningDetailDto = new WarningDetailDto();
        AbstractWarningDetailDto.fromWarning(warningDetailDto, warning);
        return warningDetailDto;
    }
}
