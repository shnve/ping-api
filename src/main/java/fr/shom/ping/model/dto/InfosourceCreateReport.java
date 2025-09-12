package fr.shom.ping.model.dto;

import fr.shom.ping.model.Attachment;
import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.MaritimeEntity;
import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.ServiceUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface InfosourceCreateReport {
    Set<@Valid FeatureDto> getLocalisation();

    UUID getServiceId();

    List<UUID> getConcernedMaritimeEntityIds();

    List<UUID> getAttachmentIds();

    Infosource toInfosource(List<Attachment> attachments, ServiceUser serviceUser, MaritimeService service, List<MaritimeEntity> entities);
}
