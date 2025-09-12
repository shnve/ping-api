package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.exception.BadRequestException;
import fr.shom.ping.model.dto.MaritimeServiceCreateDto;
import fr.shom.ping.model.dto.MaritimeServiceDto;
import fr.shom.ping.model.enums.MaritimeServiceType;
import fr.shom.ping.model.enums.ServiceNetwork;
import fr.shom.ping.validation.ValidCountryCode;
import fr.shom.ping.validation.ValidURN;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.COUNTRY_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.EMPTY_LIST_EXAMPLE;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.INFORMATION_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.INTERNATIONAL_NAME_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.LOCALIZED_INFORMATION_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.NAME_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.URN_DESCRIPTION;
import static fr.shom.ping.model.enums.MaritimeServiceType.AUTHORITY;
import static fr.shom.ping.model.enums.ServiceNetwork.GOVERNMENT_SERVICE;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.util.ObjectUtils.isEmpty;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = NON_NULL)
@Schema(name = "Maritime Service Model")
public class MaritimeService implements Serializable {
    @Serial
    private static final long serialVersionUID = -7180315901403592992L;

    @Id
    @Schema(description = ID_DESCRIPTION, requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID id;

    @ValidURN(message = "validation.maritime-service.urn.invalid")
    @Schema(description = URN_DESCRIPTION)
    private String urn;

    @NotEmpty(message = "validation.maritime-service.name.not-empty")
    @Column(nullable = false)
    @Schema(description = NAME_DESCRIPTION, requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = INTERNATIONAL_NAME_DESCRIPTION)
    private String internationalName;

    @Schema(description = "Production Agency")
    private String productionAgency;

    @Schema(description = INFORMATION_DESCRIPTION)
    private String information;

    @Schema(description = LOCALIZED_INFORMATION_DESCRIPTION)
    private String localizedInformation;

    @ValidCountryCode(message = "validation.maritime-service.country-code.invalid")
    @Schema(description = COUNTRY_DESCRIPTION)
    private String country;

    @NotNull(message = "validation.maritime-service.type.not-null")
    @Column(nullable = false)
    @Enumerated(STRING)
    @Schema(description = "Maritime Service Type", requiredMode = Schema.RequiredMode.REQUIRED)
    private MaritimeServiceType type;

    @NotNull(message = "validation.maritime-service.service-network.not-null")
    @Column(nullable = false)
    @Enumerated(STRING)
    @Schema(description = "Service Network", requiredMode = Schema.RequiredMode.REQUIRED)
    @Builder.Default
    private ServiceNetwork serviceNetwork = GOVERNMENT_SERVICE;

    @Schema(description = "PING partner")
    private boolean pingPartner;

    @Schema(description = "Maritime Notice Producer")
    private boolean maritimeNoticeProducer;

    @Schema(description = "Folder manager")
    private boolean folderManager;

    @Schema(description = "Survey owner")
    private boolean surveyOwner;

    @OneToMany
    @JoinTable(
        name = "maritime_service_areas",
        joinColumns = @JoinColumn(name = "maritime_service_id"),
        inverseJoinColumns = @JoinColumn(name = "areas_id"))
    @Schema(description = "List of linked areas", example = EMPTY_LIST_EXAMPLE)
    @ToString.Exclude
    private List<Area> areas;

    @OneToMany(mappedBy = "maritimeService", fetch = LAZY)
    @Schema(description = "List of linked maritime entities", example = EMPTY_LIST_EXAMPLE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private Set<MaritimeEntity> maritimeEntities;

    @ManyToMany(mappedBy = "additionalServices", fetch = LAZY)
    @Schema(description = "List of observed maritime entities", example = EMPTY_LIST_EXAMPLE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private Set<MaritimeEntity> observedMaritimeEntities;

    @Column(unique = true)
    private String sissyId;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    @JsonIgnore
    @OneToMany(mappedBy = "maritimeService", fetch = LAZY)
    private Set<Tag> tags = new HashSet<>();

    @Column(unique = true)
    private String authorityCode;

    @PrePersist
    public void prePersist() {
        id = randomUUID();
    }

    @Schema(description = "SHOM service")
    private boolean shomService;

    @Schema(description = "OHI producer code")
    @Column(unique = true, name = "ohi_producer_code")
    private String ihoProducerCode;

    @Schema(description = "S124 Data Producer")
    @Column(nullable = false, name = "s124_data_producer")
    private boolean s124DataProducer;

    public MaritimeService(MaritimeServiceCreateDto dto, final List<Area> areas) {
        this.localizedInformation = dto.getLocalizedInformation();
        this.information = dto.getInformation();
        this.internationalName = dto.getInternationalName();
        this.name = dto.getName();
        this.urn = dto.getUrn();
        this.country = dto.getCountry();
        this.maritimeNoticeProducer = dto.isMaritimeNoticeProducer();
        this.pingPartner = dto.isPingPartner();
        this.folderManager = dto.isFolderManager();
        this.surveyOwner = dto.isSurveyOwner();
        if (this.maritimeNoticeProducer && isEmpty(dto.getProductionAgency())) {
            throw new BadRequestException("validation.maritime-service.production-agency.not-empty");
        }
        this.productionAgency = dto.getProductionAgency();
        this.serviceNetwork = dto.getServiceNetwork();
        this.type = dto.getType();
        if (type == AUTHORITY && isBlank(dto.getAuthorityCode())) {
            throw new BadRequestException("validation.maritime-service.authority-code.not-blank");
        }
        this.authorityCode = dto.getAuthorityCode();
        this.areas = areas;
        this.sissyId = dto.getSissyId();
        this.shomService = dto.isShomService();
        this.s124DataProducer = dto.isS124DataProducer();
        if (this.maritimeNoticeProducer && this.s124DataProducer && isEmpty(dto.getIhoProducerCode())) {
            throw new BadRequestException("validation.maritime-service.iho-producer-code.not-empty");
        }
        this.ihoProducerCode = dto.getIhoProducerCode();
    }

    public MaritimeServiceDto asDto() {
        return new MaritimeServiceDto(this);
    }

}
