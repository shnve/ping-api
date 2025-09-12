package fr.shom.ping.model.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class S124DataSetDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -600398151228179627L;

    @Default
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<S124MemberDto> imember = new ArrayList<>();
}
