package fr.shom.ping.model.converter;


import fr.shom.ping.model.enums.DiffusionNavtexType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.stream.Stream;

@Converter(autoApply = true)
public class DiffusionNavtexTypeConverter implements AttributeConverter<DiffusionNavtexType, String> {

    @Override
    public String convertToDatabaseColumn(DiffusionNavtexType diffusionNavtexType) {
        if (diffusionNavtexType == null) {
            return null;
        }
        return diffusionNavtexType.getSymbol();
    }

    @Override
    public DiffusionNavtexType convertToEntityAttribute(String symbol) {
        if (symbol == null) {
            return null;
        }

        return Stream.of(DiffusionNavtexType.values())
            .filter(t -> t.getSymbol().equals(symbol))
            .findFirst()
            .orElseThrow(IllegalArgumentException::new);
    }
}

