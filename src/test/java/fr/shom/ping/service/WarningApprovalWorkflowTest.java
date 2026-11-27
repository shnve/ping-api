package fr.shom.ping.service;

import fr.shom.ping.exception.BadRequestException;
import fr.shom.ping.exception.ForbiddenException;
import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.FeaturePart;
import fr.shom.ping.model.GeneralArea;
import fr.shom.ping.model.LocalizedText;
import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.Serie;
import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.ServiceUserRole;
import fr.shom.ping.model.SubArea;
import fr.shom.ping.model.Warning;
import fr.shom.ping.model.enums.SerieType;
import fr.shom.ping.model.enums.ServiceRole;
import fr.shom.ping.model.enums.UserStatus;
import fr.shom.ping.model.enums.WarningHazardTypeGeneral;
import fr.shom.ping.model.enums.WarningStatus;
import fr.shom.ping.model.predicate.WarningAdminSearchCriteria;
import fr.shom.ping.properties.PingProperties;
import fr.shom.ping.repository.AttachmentRepository;
import fr.shom.ping.repository.GeoregulationRepository;
import fr.shom.ping.repository.MaritimeEntityReferenceRepository;
import fr.shom.ping.repository.MaritimeEntityRepository;
import fr.shom.ping.repository.MaritimeServiceRepository;
import fr.shom.ping.repository.ServiceUserRepository;
import fr.shom.ping.repository.SubAreaRepository;
import fr.shom.ping.repository.WarningLocalityNamesRepository;
import fr.shom.ping.repository.WarningRepository;
import fr.shom.ping.scheduling.SchedulerJobService;
import fr.shom.ping.utils.NavtexUtil;
import fr.shom.ping.utils.TelexUtil;
import fr.shom.ping.utils.search.WarningAdminTableUtil;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarningApprovalWorkflowTest {

    @Mock private EntityManager entityManager;
    @Mock private WarningRepository warningRepository;
    @Mock private SerieService serieService;
    @Mock private ProUserService proUserService;
    @Mock private SubAreaRepository subAreaRepository;
    @Mock private WarningLocalityNamesRepository localityNamesRepository;
    @Mock private AttachmentService attachmentService;
    @Mock private ServiceUserRepository serviceUserRepository;
    @Mock private MaritimeServiceRepository maritimeServiceRepository;
    @Mock private SchedulerJobService schedulerJobService;
    @Mock private TelexUtil telexUtil;
    @Mock private NavtexUtil navtexUtil;
    @Mock private InfosourceAdminService infosourceAdminService;
    @Mock private SissyService sissyService;
    @Mock private MaritimeEntityRepository maritimeEntityRepository;
    @Mock private MaritimeEntityReferenceRepository maritimeEntityReferenceRepository;
    @Mock private GeoregulationRepository georegulationRepository;
    @Mock private FolderService folderService;
    @Mock private SseNotificationService sseNotificationService;
    @Mock private SecomService secomService;
    @Mock private S124ExchangeSetLocator s124ExchangeSetLocator;
    @Mock private AttachmentRepository attachmentRepository;

    private WarningAdminService warningAdminService;
    private UUID serviceId;
    private Serie serie;
    private ServiceUser operatorUser;
    private ServiceUser approverUser;

    @BeforeEach
    void setUp() {
        PingProperties pingProperties = new PingProperties();
        warningAdminService = new WarningAdminService(
            entityManager,
            warningRepository,
            serieService,
            proUserService,
            subAreaRepository,
            localityNamesRepository,
            attachmentService,
            serviceUserRepository,
            maritimeServiceRepository,
            schedulerJobService,
            telexUtil,
            navtexUtil,
            infosourceAdminService,
            sissyService,
            maritimeEntityRepository,
            maritimeEntityReferenceRepository,
            georegulationRepository,
            folderService,
            sseNotificationService,
            secomService,
            s124ExchangeSetLocator,
            attachmentRepository,
            pingProperties
        );

        serviceId = UUID.randomUUID();
        MaritimeService maritimeService = MaritimeService.builder()
            .id(serviceId)
            .name("SHN Venezuela")
            .maritimeNoticeProducer(true)
            .s124DataProducer(false)
            .build();

        serie = Serie.builder()
            .id(UUID.randomUUID())
            .name("ADVERTENCIA LOCAL VE")
            .type(SerieType.LOCAL_NAVIGATIONAL_WARNING)
            .maritimeService(maritimeService)
            .mandatoryLanguages(Set.of("es"))
            .ranges(java.util.Map.of(1, 500))
            .build();

        operatorUser = ServiceUser.builder()
            .id(UUID.randomUUID())
            .email("operador@shn.gob.ve")
            .firstname("Ana")
            .lastname("Redactora")
            .status(UserStatus.ENABLED)
            .service(maritimeService)
            .roles(Set.of(ServiceUserRole.builder().serviceId(serviceId).serviceRole(ServiceRole.SERVICE_OPERATOR).build()))
            .build();

        approverUser = ServiceUser.builder()
            .id(UUID.randomUUID())
            .email("aprobador@shn.gob.ve")
            .firstname("Carlos")
            .lastname("Aprobador")
            .status(UserStatus.ENABLED)
            .service(maritimeService)
            .roles(Set.of(ServiceUserRole.builder().serviceId(serviceId).serviceRole(ServiceRole.SERVICE_APPROVER).build()))
            .build();
    }

    private Warning buildValidWarning(WarningStatus status) {
        SubArea subArea = SubArea.builder().id(UUID.randomUUID()).name("Costa Central").build();
        GeneralArea generalArea = GeneralArea.builder().id(subArea.getId()).name("Costa Central").position(1).subArea(subArea).build();
        FeaturePart part = FeaturePart.builder()
            .position(1)
            .information(Set.of(LocalizedText.builder().language("es").text("Boya apagada en posición.").build()))
            .build();
        Warning warning = Warning.builder()
            .id(UUID.randomUUID())
            .serie(serie)
            .nameOfSeries(serie.getName())
            .productionAgency("SHN")
            .status(status)
            .warningHazardTypeGeneral(WarningHazardTypeGeneral.AIDS_TO_NAVIGATION)
            .creationDate(Instant.now())
            .generalAreas(new TreeSet<>(Set.of(generalArea)))
            .featureParts(new TreeSet<>(Set.of(part)))
            .build();
        return warning;
    }

    @Test
    void publicationWorkflowRequiresApprovalAndNotifiesRequester() throws ServiceException {
        Warning draft = buildValidWarning(WarningStatus.DRAFT);
        when(warningRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
        when(warningRepository.save(any(Warning.class))).thenAnswer(inv -> inv.getArgument(0));
        when(proUserService.findByEmail("operador@shn.gob.ve")).thenReturn(operatorUser);
        when(proUserService.findByEmail("aprobador@shn.gob.ve")).thenReturn(approverUser);
        when(warningRepository.findReservedWarningNumbersBySerieAndYear(eq(serie), any())).thenReturn(List.of());
        when(telexUtil.generateTelex(any(Warning.class))).thenReturn("TELEX");

        // Operator cannot bypass approval by calling publish directly
        assertThatThrownBy(() -> warningAdminService.publish(draft.getId(), "operador@shn.gob.ve"))
            .isInstanceOf(ForbiddenException.class);

        // Step 1: Operator requests approval
        warningAdminService.requestApproval(draft.getId(), "operador@shn.gob.ve");
        assertThat(draft.getStatus()).isEqualTo(WarningStatus.PENDING_APPROVAL);
        assertThat(draft.getApprovalRequestedBy()).isEqualTo(operatorUser);
        verify(sseNotificationService).sendWarningApprovalRequestedNotification(draft, false, null);

        // Step 2: Approver returns draft for correction (mandatory comment)
        assertThatThrownBy(() -> warningAdminService.reject(draft.getId(), "   ", "aprobador@shn.gob.ve"))
            .isInstanceOf(BadRequestException.class);

        warningAdminService.reject(draft.getId(), "Ajustar coordenadas WGS-84", "aprobador@shn.gob.ve");
        assertThat(draft.getStatus()).isEqualTo(WarningStatus.DRAFT);
        assertThat(draft.getRejectionComment()).isEqualTo("Ajustar coordenadas WGS-84");
        verify(sseNotificationService).sendWarningApprovalDecisionNotification(draft, operatorUser, false, false, "Ajustar coordenadas WGS-84");

        // Step 3: Operator re-submits, Approver grants visto bueno -> immediately published
        warningAdminService.requestApproval(draft.getId(), "operador@shn.gob.ve");
        assertThat(draft.getRejectionComment()).isNull();

        warningAdminService.approve(draft.getId(), "aprobador@shn.gob.ve");
        assertThat(draft.getStatus()).isEqualTo(WarningStatus.PUBLISHED);
        assertThat(draft.getWarningNumber()).isEqualTo(1);
        assertThat(draft.getApprovedBy()).isEqualTo(approverUser);
        verify(sseNotificationService).sendWarningApprovalDecisionNotification(draft, operatorUser, true, false, null);
    }

    @Test
    void directCancellationKeepsWarningPublishedUntilApproverValidates() throws ServiceException {
        Warning published = buildValidWarning(WarningStatus.PUBLISHED);
        published.setWarningNumber(15);
        published.setYear(2026);
        when(warningRepository.findById(published.getId())).thenReturn(Optional.of(published));
        when(warningRepository.save(any(Warning.class))).thenAnswer(inv -> inv.getArgument(0));
        when(proUserService.findByEmail("operador@shn.gob.ve")).thenReturn(operatorUser);
        when(proUserService.findByEmail("aprobador@shn.gob.ve")).thenReturn(approverUser);

        // Operator requests direct cancellation: stays PUBLISHED until approved
        warningAdminService.cancel(published.getId(), "operador@shn.gob.ve");
        assertThat(published.getStatus()).isEqualTo(WarningStatus.PUBLISHED);
        assertThat(published.getCancellationRequested()).isTrue();
        verify(sissyService, never()).sendWarningPublishedOrCancelled(any());
        verify(sseNotificationService).sendWarningApprovalRequestedNotification(published, true, null);

        // Approver approves cancellation -> transitions to CANCELLED and notifies requester
        warningAdminService.approve(published.getId(), "aprobador@shn.gob.ve");
        assertThat(published.getStatus()).isEqualTo(WarningStatus.CANCELLED);
        assertThat(published.getCancellationRequested()).isFalse();
        verify(sissyService).sendWarningPublishedOrCancelled(published);
        verify(sseNotificationService).sendWarningApprovalDecisionNotification(published, operatorUser, true, true, null);
    }

    @Test
    void pendingApprovalTableQueryIncludesBothDraftPublicationsAndPublishedCancellationRequests() {
        StringBuilder sql = new StringBuilder();
        WarningAdminSearchCriteria criteria = new WarningAdminSearchCriteria();
        criteria.setStatus(WarningStatus.PENDING_APPROVAL);

        WarningAdminTableUtil.addCriteriaSelectionToSql(sql, criteria);

        assertThat(sql.toString())
            .contains("AND (w.status = :status OR (w.status = 'PUBLISHED' AND w.cancellation_requested = TRUE))");
    }
}
