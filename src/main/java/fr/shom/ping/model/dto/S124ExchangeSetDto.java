package fr.shom.ping.model.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@JacksonXmlRootElement(localName = "S124:ExchangeSet")
public class S124ExchangeSetDto extends AbstractS124ExchangeSetBaseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -2954680605592536424L;

    @Default
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "S124:DataSet")
    private List<S124DataSetDto> dataSet = new ArrayList<>();
}
