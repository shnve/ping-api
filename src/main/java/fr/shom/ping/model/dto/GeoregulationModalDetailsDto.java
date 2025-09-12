package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.shom.ping.model.FixedDateRange;
import fr.shom.ping.model.Georegulation;
import fr.shom.ping.model.GeoregulationNumber;
import fr.shom.ping.model.GeoregulationPart;
import fr.shom.ping.model.SeasonalPeriod;
import fr.shom.ping.model.enums.GeoregulationActivity;
import fr.shom.ping.model.enums.GeoregulationPartType;
import fr.shom.ping.model.enums.GeoregulationStatus;
import fr.shom.ping.model.enums.GeoregulationType;
import fr.shom.ping.validation.NoHtml;
import jakarta.validation.Valid;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
public class GeoregulationModalDetailsDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -7256603395147706230L;

    private UUID id;

    private GeoregulationNumberModalDetailsDto mainNumber;

    private List<GeoregulationNumberModalDetailsDto> additionalNumbers;

    private boolean isTemporary;

    @JsonProperty("isPublic")
    private boolean isPublic;

    private boolean isNew;

    @Default
    private SortedSet<FixedDateRange> calendarPeriods = new TreeSet<>();

    @Default
    private SortedSet<SeasonalPeriod> seasonalPeriods = new TreeSet<>();

    private GeoregulationType type;

    private boolean hasCoastlineLimit;

    @Default
    private List<GeoregulationActivity> activities = new ArrayList<>();

    private String title;

    private String internationalTitle;

    private String summary;

    private String internationalSummary;

    @Default
    private List<@Valid GeoregulationPartModalDetailsDto> parts = new ArrayList<>();

    @Default
    private List<@Valid AttachmentDownloadDto> attachments = new ArrayList<>();

    @Default
    @NoHtml
    private List<String> links = new ArrayList<>();

    private GeoregulationStatus status;

    private Instant publicationDate;

    private Instant repealDate;

    private Instant lastUpdate;

    private boolean canBeAddedToFolder;

    public GeoregulationModalDetailsDto(Georegulation georegulation, boolean canBeAddedToFolder) {
        id = georegulation.getId();
        mainNumber = new GeoregulationNumberModalDetailsDto(georegulation.getNumber());
        additionalNumbers = ofNullable(georegulation.getAdditionalNumbers())
            .map(numbers -> numbers.stream().map(GeoregulationNumberModalDetailsDto::new).collect(toList()))
            .orElse(null);
        isTemporary = georegulation.isTemporary();
        isPublic = georegulation.isPublic();
        calendarPeriods = georegulation.getCalendarPeriods();
        seasonalPeriods = georegulation.getSeasonalPeriods();
        type = georegulation.getType();
        hasCoastlineLimit = georegulation.isHasCoastlineLimit();
        activities = georegulation.getActivities();
        title = georegulation.getTitle();
        internationalTitle = georegulation.getInternationalTitle();
        summary = georegulation.getSummary();
        internationalSummary = georegulation.getInternationalSummary();
        parts = georegulation.getParts().stream().map(GeoregulationPartModalDetailsDto::new).collect(toList());
        attachments = georegulation.getAttachments().stream().map(AttachmentDownloadDto::new).collect(toList());
        links = georegulation.getLinks();
        status = georegulation.getStatus();
        publicationDate = georegulation.getPublicationDate();
        repealDate = georegulation.getRepealDate();
        lastUpdate = georegulation.getLastUpdate();
        isNew = georegulation.isNew();
        this.canBeAddedToFolder = canBeAddedToFolder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(NON_NULL)
    public static class GeoregulationNumberModalDetailsDto implements Serializable {
        @Serial
        private static final long serialVersionUID = -6533820948370857526L;

        private Integer year;

        @NoHtml
        private String text;

        @Default
        @NoHtml
        private Set<String> authorityCodes = new HashSet<>();

        @JsonFormat()
        private LocalDate date;

        public GeoregulationNumberModalDetailsDto(GeoregulationNumber number) {
            year = number.getYear();
            text = number.getText();
            authorityCodes = newHashSet(number.getAuthorityCodes());
            date = number.getDate();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(NON_NULL)
    public static class GeoregulationPartModalDetailsDto implements Serializable {
        @Serial
        private static final long serialVersionUID = -1834547439252900851L;

        @Default
        private List<FeatureDto> features = new ArrayList<>();

        @Default
        private List<GeoregulationPartType> types = new ArrayList<>();

        @Default
        private SortedSet<FixedDateRange> calendarPeriods = new TreeSet<>();

        @Default
        private SortedSet<SeasonalPeriod> seasonalPeriods = new TreeSet<>();

        @NoHtml
        private String information;

        @NoHtml
        private String internationalInformation;

        public GeoregulationPartModalDetailsDto(GeoregulationPart part) {
            features = part.getFeatures().stream().map(FeatureDto::new).collect(toList());
            types = part.getTypes();
            calendarPeriods = part.getCalendarPeriods();
            seasonalPeriods = part.getSeasonalPeriods();
            information = part.getInformation();
            internationalInformation = part.getInternationalInformation();
        }
    }
}
