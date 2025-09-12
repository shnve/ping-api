package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.Serial;
import java.io.Serializable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@ToString
@EqualsAndHashCode
@Builder
@JsonInclude(NON_NULL)
public class S100PointProperty implements Serializable {
    @Serial
    private static final long serialVersionUID = 9145445998553807018L;

    @JsonRawValue
    @JacksonXmlProperty(localName = "S100:Point")
    private String point;
}
