package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.shom.ping.model.enums.GeoregulationActivity;
import fr.shom.ping.model.enums.GeoregulationStatus;
import fr.shom.ping.model.enums.GeoregulationType;
import fr.shom.ping.utils.GeoregulationUtil;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import fr.shom.ping.validation.NoHtml;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.hateoas.server.core.Relation;

import jakarta.validation.Valid;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.time.format.DateTimeFormatter.ofPattern;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Relation(collectionRelation = "items")
@JsonInclude(NON_NULL)
public class GeoregulationAdminTableDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 6190882234922140782L;
    private static final DateTimeFormatter NUMBER_DATE_PATTERN = ofPattern("uuuuMMdd");

    private UUID id;

    private GeoregulationStatus status;

    @JsonIgnore
    private @Valid GeoregulationAdminTableNumberDto mainNumber;

    private GeoregulationType type;

    @Default
    private Set<GeoregulationActivity> activities = new HashSet<>();

    @NoHtml
    private String title;

    @NoHtml
    private String internationalTitle;

    @Default
    private Set<@Valid GeoregulationAdminTableMaritimeEntityDto> entities = new HashSet<>();

    @NoHtml
    private String internalComment;

    private Instant lastUpdate;

    private Instant publicationDate;

    private Instant repealDate;

    @JsonProperty("isPublic")
    private boolean isPublic;

    @JsonProperty("isNew")
    private boolean isNew;

    @JsonProperty("isExpired")
    private boolean isExpired;

    public String getNumber() {
        return GeoregulationUtil.getNumber(mainNumber.getYear(), mainNumber.getText(), mainNumber.getAuthorityCodes(), mainNumber.getDate());
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(NON_NULL)
    public static class GeoregulationAdminTableNumberDto implements Serializable {
        @Serial
        private static final long serialVersionUID = 1917140798977893677L;

        private Integer year;

        @NoHtml
        private String text;

        @Default
        @NoHtml
        private Set<String> authorityCodes = new HashSet<>();

        private LocalDate date;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(NON_NULL)
    public static class GeoregulationAdminTableMaritimeEntityDto implements Serializable {
        @Serial
        private static final long serialVersionUID = -361021113264278421L;

        private UUID id;

        @NoHtml
        private String name;

        @NoHtml
        private String internationalName;
    }
}
