package fr.shom.ping.service;

import fr.shom.ping.model.enums.WarningStatus;
import fr.shom.ping.repository.WarningRepository;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class WarningInternalService {

    private final WarningRepository repository;

    public String getConcatenatedTelexForAllPublishedWarningsBySerieId(UUID serieId) {
        return repository.findByStatusAndSerieIdAndTelexIsNotNull(WarningStatus.PUBLISHED, serieId).stream()
            .collect(Collectors.joining("\n\n"));
    }
}
