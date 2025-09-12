package fr.shom.ping.model.dto;

import fr.shom.ping.model.Attachment;
import fr.shom.ping.model.FixedDateRange;
import fr.shom.ping.model.Georegulation;
import fr.shom.ping.model.GeoregulationPart;
import fr.shom.ping.model.MaritimeEntity;
import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.SeasonalPeriod;
import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.enums.GeoregulationActivity;
import fr.shom.ping.model.enums.GeoregulationStatus;
import fr.shom.ping.model.enums.GeoregulationType;
import fr.shom.ping.validation.NoHtml;
import fr.shom.ping.validation.ValidTimezone;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static java.util.stream.Collectors.toCollection;

@Data
@SuperBuilder
@NoArgsConstructor
@Schema(name = "Georegulation Create Dto Model")
public class GeoregulationCreateDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -4152878990745425893L;

    @NotNull(message = "validation.georegulation.service-id.not-null")
    protected UUID serviceId;

    @NotNull(message = "validation.georegulation.type.not-null")
    protected GeoregulationType type;

    @NotNull(message = "validation.georegulation.main-number.not-null")
    protected @Valid GeoregulationNumberDto mainNumber;

    @Default
    protected List<@Valid GeoregulationNumberDto> additionalNumbers = new ArrayList<>();

    @Default
    protected List<GeoregulationActivity> activities = new ArrayList<>();

    @NoHtml
    protected String title;

    @NoHtml
    protected String internationalTitle;

    @Default
    protected List<UUID> maritimeEntityIds = new ArrayList<>();

    @NoHtml
    protected String summary;

    @NoHtml
    protected String internationalSummary;

    @NotNull(message = "validation.georegulation.is-temporary.not-null")
    protected Boolean isTemporary;

    @Default
    protected List<@Valid FixedDateRange> calendarPeriods = new ArrayList<>();

    @Default
    protected List<@Valid SeasonalPeriod> seasonalPeriods = new ArrayList<>();

    protected boolean hasCoastlineLimit;

    @NotEmpty(message = "validation.georegulation.parts.not-empty")
    @Default
    protected List<@Valid GeoregulationPartCreateDto> parts = new ArrayList<>();

    @Default
    protected List<String> links = new ArrayList<>();

    @Default
    protected List<UUID> attachmentIds = new ArrayList<>();

    @NotNull(message = "validation.georegulation.is-public.not-null")
    protected Boolean isPublic;

    protected int markAsNewInMonths;

    @NoHtml
    protected String internalComment;

    @NotEmpty(message = "validation.georegulation.timezone.not-empty")
    @ValidTimezone(message = "validation.georegulation.timezone.invalid")
    @NoHtml
    protected String timezone;

    @Default
    protected GeoregulationStatus status = GeoregulationStatus.DRAFT;

    @NotNull(message = "validation.georegulation.all-territorial-sea.not-null")
    protected Boolean isAllTerritorialSea;

    @NotNull(message = "validation.georegulation.announced-by-avurnav-avirade.not-null")
    protected Boolean isAnnouncedByAvurnavAvirade;

    public Georegulation toGeoregulation(ServiceUser serviceUser, MaritimeService issuingService, List<MaritimeEntity> entities, List<Attachment> attachments) {
        AtomicInteger partPosition = new AtomicInteger(1);
        TreeSet<GeoregulationPart> parts = this.parts.stream()
            .map(part -> part.toGeoregulationPart(partPosition.getAndIncrement(), isTemporary))
            .collect(toCollection(TreeSet::new));

        if (isTemporary) {
            updateCalendarPeriods(calendarPeriods);
        } else {
            // When a saved temporary GR becomes permanent, we need to remove his potentials unuses calendar periods
            setCalendarPeriods(new ArrayList<>());
        }

        AtomicInteger seasonalPeriodPosition = new AtomicInteger(1);
        seasonalPeriods.forEach(seasonalPeriod -> seasonalPeriod.setPosition(seasonalPeriodPosition.getAndIncrement()));

        return Georegulation.builder()
            .issuingService(issuingService)
            .lastUpdateUser(serviceUser)
            .type(type)
            .activities(activities)
            .title(title)
            .internationalTitle(internationalTitle)
            .entities(entities)
            .summary(summary)
            .internationalSummary(internationalSummary)
            .isTemporary(isTemporary)
            .isAllTerritorialSea(isAllTerritorialSea)
            .isAnnouncedByAvurnavAvirade(isAnnouncedByAvurnavAvirade)
            .calendarPeriods(new TreeSet<>(calendarPeriods))
            .seasonalPeriods(new TreeSet<>(seasonalPeriods))
            .hasCoastlineLimit(hasCoastlineLimit)
            .parts(parts)
            .links(links)
            .attachments(attachments)
            .isPublic(isPublic)
            .markAsNewInMonths(markAsNewInMonths)
            .internalComment(internalComment)
            .timezone(timezone)
            .isNew(markAsNewInMonths != 0)
            .status(status)
            .build();
    }

    /**
     * Only update the position of the calendar periods from dto if the georegulation is temporary
     *
     * @param calendarPeriods List<FixedDateRange>
     */
    public static void updateCalendarPeriods(List<FixedDateRange> calendarPeriods) {
        AtomicInteger calendarPeriodPosition = new AtomicInteger(1);
        calendarPeriods.forEach(calendarPeriod -> calendarPeriod.setPosition(calendarPeriodPosition.getAndIncrement()));
    }
}
