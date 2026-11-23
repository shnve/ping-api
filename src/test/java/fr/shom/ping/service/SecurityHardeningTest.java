package fr.shom.ping.service;

import static fr.shom.ping.model.enums.ProUserRole.FUNCTIONAL_ADMIN;
import static fr.shom.ping.model.enums.ProUserRole.SERVICE_USER;
import static fr.shom.ping.model.enums.ServiceRole.SERVICE_ADMIN;
import static fr.shom.ping.model.enums.ServiceRole.SERVICE_VISITOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fr.shom.ping.exception.ForbiddenException;
import fr.shom.ping.exception.KeycloakUserCreationException;
import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.FunctionalAdmin;
import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.ServiceUserRole;
import fr.shom.ping.model.dto.CreateFunctionalAdminDto;
import fr.shom.ping.model.dto.CreatePublicUserDto;
import fr.shom.ping.model.dto.CreateServiceUserDto;
import fr.shom.ping.model.dto.CartographyUploadResponse;
import fr.shom.ping.model.enums.UserCreationErrorMessage;
import fr.shom.ping.properties.ClamAvProperties;
import fr.shom.ping.repository.AttachmentRepository;
import fr.shom.ping.repository.ProUserRepository;
import fr.shom.ping.repository.UserRepository;
import fr.shom.ping.utils.XmlUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.apache.xmlbeans.XmlException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.validation.BindingResult;
import xyz.capybara.clamav.ClamavClient;

/** PISAC (18-nov-2026): pruebas unitarias de los parches de seguridad S-1, S-2, S-8 y S-10. */
class SecurityHardeningTest {

    @Test
    void serviceUserCannotEditOtherUserOrEscalateRole() {
        ProUserRepository repo = mock(ProUserRepository.class);
        AreaService areaService = mock(AreaService.class);
        ProUserService service = new ProUserService(
            repo, null, null, null, areaService, null, null);

        UUID otherId = UUID.randomUUID();
        UUID myServiceId = UUID.randomUUID();
        MaritimeService ms = MaritimeService.builder().id(myServiceId).build();

        ServiceUser current = ServiceUser.builder()
            .id(UUID.randomUUID())
            .email("operador@shn.gob.ve")
            .role(SERVICE_USER)
            .service(ms)
            .roles(Set.of(ServiceUserRole.builder().serviceId(myServiceId).serviceRole(SERVICE_VISITOR).build()))
            .build();

        FunctionalAdmin other = FunctionalAdmin.builder()
            .id(otherId)
            .email("admin@shn.gob.ve")
            .role(FUNCTIONAL_ADMIN)
            .build();

        when(repo.findById(otherId)).thenReturn(Optional.of(other));
        when(repo.findOneByEmail("operador@shn.gob.ve")).thenReturn(Optional.of(current));

        CreateFunctionalAdminDto escalationDto = new CreateFunctionalAdminDto();
        escalationDto.setEmail("operador@shn.gob.ve");
        escalationDto.setRole(FUNCTIONAL_ADMIN);

        BindingResult br = mock(BindingResult.class);
        when(repo.findById(current.getId())).thenReturn(Optional.of(current));

        // S-1a: SERVICE_USER no puede editar a otro usuario
        assertThatThrownBy(() -> service.update(otherId, escalationDto, "operador@shn.gob.ve", br))
            .isInstanceOf(ForbiddenException.class);

        // S-1b: SERVICE_USER no puede promoverse a FUNCTIONAL_ADMIN editándose a sí mismo
        assertThatThrownBy(() -> service.update(current.getId(), escalationDto, "operador@shn.gob.ve", br))
            .isInstanceOf(ForbiddenException.class);

        // S-1c: SERVICE_USER no puede cambiar su rol dentro del organismo ni cambiar de organismo
        CreateServiceUserDto promoteServiceRoleDto = new CreateServiceUserDto();
        promoteServiceRoleDto.setEmail("operador@shn.gob.ve");
        promoteServiceRoleDto.setRole(SERVICE_USER);
        promoteServiceRoleDto.setServiceId(myServiceId);
        promoteServiceRoleDto.setServiceRole(SERVICE_ADMIN);
        assertThatThrownBy(() -> service.update(current.getId(), promoteServiceRoleDto, "operador@shn.gob.ve", br))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void cartographyUploadRejectsLeadingDashAndStripsTraversal() {
        CartographyService cartographyService = new CartographyService(null, null, null);

        MockMultipartFile flagName = new MockMultipartFile("file", "-evil.pdf", "application/pdf", new byte[]{1});
        CartographyUploadResponse res = cartographyService.prepareUpload(flagName, "banda1", new CartographyService.PreparedUpload());
        assertThat(res).isNotNull();
        assertThat(res.isSuccess()).isFalse();

        // FilenameUtils.getName('../../evil.exe') -> 'evil.exe' (rechazado por formato, no escribe fuera)
        MockMultipartFile traversal = new MockMultipartFile("file", "../../evil.exe", "application/octet-stream", new byte[]{1});
        CartographyUploadResponse res2 = cartographyService.prepareUpload(traversal, "banda1", new CartographyService.PreparedUpload());
        assertThat(res2).isNotNull();
        assertThat(res2.isSuccess()).isFalse();
        assertThat(res2.getFileName()).isEqualTo("evil.exe");
    }

    @Test
    void xmlPrettyPrintBlocksDtdEntities() {
        String xxe = "<?xml version=\"1.0\"?><!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]><root>&xxe;</root>";
        assertThatThrownBy(() -> XmlUtil.prettyPrintByTransformer(xxe, 2, false))
            .isInstanceOf(XmlException.class);
    }

    @Test
    void clamAvRespectsFailOpenSetting() throws ServiceException {
        ClamavClient client = mock(ClamavClient.class);
        Mockito.doThrow(new RuntimeException("connection refused")).when(client).ping();

        ClamAvProperties closedProps = ClamAvProperties.builder()
            .host("localhost").port(4310).failOpen(false).build();
        ClamAvService failClosedService = new ClamAvService(client, closedProps);
        assertThatThrownBy(() -> failClosedService.hasVirus("archivo.pdf"))
            .isInstanceOf(ServiceException.class)
            .hasMessage("clamav.detection.failure");

        ClamAvProperties openProps = ClamAvProperties.builder()
            .host("localhost").port(4310).failOpen(true).build();
        ClamAvService failOpenService = new ClamAvService(client, openProps);
        assertThat(failOpenService.hasVirus("archivo.pdf")).isFalse();
    }

    @Test
    void clamAvRejectsOversizedFileWhenFailClosed() throws IOException, ServiceException {
        Path tmp = Files.createTempFile("pisac-clamav-test-", ".bin");
        try {
            Files.write(tmp, new byte[2048]);
            ClamavClient client = mock(ClamavClient.class);

            ClamAvProperties closedProps = ClamAvProperties.builder()
                .host("localhost").port(4310).syncAnalysisThreshold(0)
                .clamavReplacedPath("").clamavReplacementPath("").failOpen(false).build();
            ClamAvService closedService = new ClamAvService(client, closedProps);
            assertThatThrownBy(() -> closedService.hasVirus(tmp.toString()))
                .isInstanceOf(ServiceException.class)
                .hasMessage("clamav.detection.failure");

            ClamAvProperties openProps = ClamAvProperties.builder()
                .host("localhost").port(4310).syncAnalysisThreshold(0)
                .clamavReplacedPath("").clamavReplacementPath("").failOpen(true).build();
            ClamAvService openService = new ClamAvService(client, openProps);
            assertThat(openService.hasVirus(tmp.toString())).isFalse();
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void attachmentHardeningBlocksActiveContentTypesAndRestrictsServiceUserDelete() {
        assertThat(AttachmentService.sanitizeDownloadContentType("image/svg+xml", "icono.svg"))
            .isEqualTo("application/octet-stream");
        assertThat(AttachmentService.sanitizeDownloadContentType("text/xml", "datos.xml"))
            .isEqualTo("application/octet-stream");
        assertThat(AttachmentService.sanitizeDownloadContentType("application/pdf", "aviso.pdf"))
            .isEqualTo("application/pdf");

        AttachmentRepository repo = mock(AttachmentRepository.class);
        AttachmentService service = new AttachmentService(repo, null, null, null, null, null, null);
        UUID attachmentId = UUID.randomUUID();
        Authentication serviceUserAuth = new TestingAuthenticationToken(
            "visitante@shn.gob.ve", "n/a", List.of(new SimpleGrantedAuthority("service-user")));

        // S-7: SERVICE_VISITOR (sin rol de escritura) no puede borrar adjuntos
        when(repo.isLinkedToStaticFile(attachmentId)).thenReturn(false);
        when(repo.hasServiceUserWriteRole("visitante@shn.gob.ve")).thenReturn(false);
        assertThatThrownBy(() -> service.deleteFromDiskAndDatabase(attachmentId, serviceUserAuth))
            .isInstanceOf(ForbiddenException.class);

        // S-7: service-user (incluso con rol de escritura) no puede borrar static_file
        when(repo.isLinkedToStaticFile(attachmentId)).thenReturn(true);
        when(repo.hasServiceUserWriteRole("visitante@shn.gob.ve")).thenReturn(true);
        assertThatThrownBy(() -> service.deleteFromDiskAndDatabase(attachmentId, serviceUserAuth))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void publicUserRegistrationReturnsNeutralSuccessOnDuplicateEmail() throws ServiceException {
        KeycloakService keycloakService = mock(KeycloakService.class);
        UserRepository userRepository = mock(UserRepository.class);
        AreaService areaService = mock(AreaService.class);
        UserService userService = new UserService(keycloakService, userRepository, areaService);

        CreatePublicUserDto dto = CreatePublicUserDto.builder()
            .email("existente@ejemplo.com")
            .password("ClaveFuerte#2026")
            .proAccount(false)
            .build();

        // Checklist 1.4: correo existente no lanza error ni crea registro duplicado
        when(keycloakService.createUser(dto))
            .thenThrow(new KeycloakUserCreationException(UserCreationErrorMessage.USER_ALREADY_EXISTS));
        BindingResult brDuplicate = new org.springframework.validation.BeanPropertyBindingResult(dto, "user");
        userService.create(dto, brDuplicate);
        assertThat(brDuplicate.hasErrors()).isFalse();
        Mockito.verify(userRepository, Mockito.never()).save(any());

        // Error de política de contraseña sí debe reportarse normalmente
        Mockito.doThrow(new KeycloakUserCreationException(UserCreationErrorMessage.PASSWORD_POLICY_NOT_MET))
            .when(keycloakService).createUser(dto);
        BindingResult brWeakPass = new org.springframework.validation.BeanPropertyBindingResult(dto, "user");
        assertThatThrownBy(() -> userService.create(dto, brWeakPass))
            .isInstanceOf(ServiceException.class);
        assertThat(brWeakPass.hasFieldErrors("password")).isTrue();
    }
}
