package fr.shom.ping.service;

import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.Warning;
import fr.shom.ping.model.dto.AbstractSissyLinkedEntity;
import fr.shom.ping.model.dto.SissyFolderDto;
import fr.shom.ping.model.dto.SissyUpdateMessageDto;
import fr.shom.ping.model.dto.SissyWarningDto;
import fr.shom.ping.model.enums.FolderStatus;
import fr.shom.ping.model.enums.InfosourceNature;
import fr.shom.ping.model.enums.WarningStatus;
import fr.shom.ping.model.tuple.FolderByInfosourceTuple;
import java.time.Year;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
@Service
@Slf4j
public class SissyService {

    private final KafkaService kafkaService;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public void sendWarningPublishedOrCancelled(Warning warning) {
        sendMessages(warning.getInfosources().stream()
            .filter(infosource -> InfosourceNature.LIGHTHOUSE_AND_BEACON.equals(infosource.getNature()))
            .collect(toList()));
    }

    @Transactional(readOnly = true)
    public void sendFolderUpdate(List<Infosource> infosources) {
        sendMessages(infosources.stream()
            .filter(infosource -> infosource.getNature() == InfosourceNature.LIGHTHOUSE_AND_BEACON)
            .collect(toList()));
    }

    private void sendMessages(List<Infosource> infosourceList) {
        if (infosourceList.isEmpty()) {
            return;
        }

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("infosourceIds", infosourceList.stream().map(Infosource::getId).collect(toList()));
        List<FolderByInfosourceTuple> folderByInfosourceTuples = jdbcTemplate.query(
            "SELECT n.infosource_id, f.id as folderId, f.number as folderNumber, f.creation_date as folderCreationDate, f.last_update as folderLastUpdate, " +
                "f.status as folderStatus " +
                "FROM folder f " +
                "LEFT JOIN notion n ON n.folder_id = f.id " +
                "WHERE n.infosource_id IN (:infosourceIds) ",
            parameters,
            new BeanPropertyRowMapper<>(FolderByInfosourceTuple.class)
        );

        infosourceList.parallelStream()
            .map(infosource -> {
                List<SissyWarningDto> warnings = infosource.getLinkedWarnings().stream()
                    .filter(warning -> warning.getStatus() != WarningStatus.DRAFT)
                    .map(SissyWarningDto::fromWarning)
                    .collect(toList());

                List<SissyFolderDto> folders = folderByInfosourceTuples.stream()
                    .filter(tuple -> tuple.getInfosourceId().equals(infosource.getId()))
                    .map(tuple -> SissyFolderDto.builder()
                        .number(tuple.getFolderNumber() + "/" + Year.of(tuple.getFolderCreationDate().atZone(UTC).getYear()).format(ofPattern("uu")))
                        .status(tuple.getFolderStatus() == FolderStatus.CLOSED ? "CLOSED" : "IN_PROGRESS")
                        .url(AbstractSissyLinkedEntity.FOLDER_RELATIVE_PATH + tuple.getFolderId())
                        .statusDate(tuple.getFolderLastUpdate())
                        .build())
                    .collect(toList());

                return SissyUpdateMessageDto.builder()
                    .number(infosource.getNumber())
                    .linkedWarnings(warnings)
                    .linkedFolders(folders)
                    .build();
            })
            .forEach(kafkaService::sendSissyUpdateMessage);
    }
}
