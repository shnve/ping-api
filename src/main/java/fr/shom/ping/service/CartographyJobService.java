package fr.shom.ping.service;

import fr.shom.ping.model.dto.CartographyJobDto;
import fr.shom.ping.model.dto.CartographyUploadResponse;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Procesa las cartas en segundo plano para que la petición HTTP no dependa de la duración de la conversión
 * (una carta grande puede tardar más que el tiempo de espera de Nginx, lo que daba un 504 falso).
 *
 * <ul>
 *   <li>Las cartas se procesan de a una, en orden de llegada (un solo hilo): evita conversiones GDAL simultáneas
 *       que compitan por CPU/memoria y carreras sobre el mismo mosaico.</li>
 *   <li>Si llega otra vez la misma carta (misma banda e identificador) mientras sigue en cola o en proceso,
 *       se devuelve el trabajo existente en lugar de convertirla de nuevo.</li>
 *   <li>Los trabajos terminados se conservan en memoria unas horas para que la pantalla pueda consultar el resultado.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CartographyJobService {

    private static final Duration FINISHED_RETENTION = Duration.ofHours(6);

    private final CartographyService cartographyService;

    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "cartography-worker");
        t.setDaemon(true);
        return t;
    });

    private final Map<String, CartographyJobDto> jobs = new ConcurrentHashMap<>();
    private final Map<String, String> activeByChart = new ConcurrentHashMap<>();

    /**
     * Recibe la carta, la guarda en disco y la encola. Devuelve el trabajo (nuevo o el ya existente para la misma
     * carta) o un trabajo en estado ERROR si la carta no es válida.
     */
    public CartographyJobDto submit(MultipartFile file, String band, boolean reloadGeoserver, String requestedBy) {
        purgeOldJobs();
        CartographyService.PreparedUpload prepared = new CartographyService.PreparedUpload();
        CartographyUploadResponse error = cartographyService.prepareUpload(file, band, prepared);
        if (error != null) {
            CartographyJobDto failed = newJob(error.getFileName(), error.getBand(), requestedBy);
            finish(failed, error);
            return failed;
        }

        String chartKey = prepared.getChartKey();
        synchronized (activeByChart) {
            String existingId = activeByChart.get(chartKey);
            CartographyJobDto existing = existingId != null ? jobs.get(existingId) : null;
            if (existing != null && !existing.isFinished()) {
                log.info("La carta {} ya está en proceso (trabajo {}); se descarta el reintento", chartKey, existingId);
                cartographyService.discardPreparedUpload(prepared);
                return existing;
            }
            CartographyJobDto job = newJob(prepared.getOriginalFilename(), prepared.getBand(), requestedBy);
            activeByChart.put(chartKey, job.getJobId());
            worker.submit(() -> run(job, prepared, chartKey, reloadGeoserver));
            log.info("Carta {} encolada en el trabajo {} (banda {})", prepared.getOriginalFilename(), job.getJobId(), prepared.getBand());
            return job;
        }
    }

    public Optional<CartographyJobDto> get(String jobId) {
        return Optional.ofNullable(jobs.get(jobId));
    }

    private void run(CartographyJobDto job, CartographyService.PreparedUpload prepared, String chartKey, boolean reload) {
        job.setStartedAt(Instant.now().toString());
        CartographyUploadResponse result;
        try {
            result = cartographyService.processPreparedUpload(prepared, reload, job::setStatus);
        } catch (Exception e) {
            log.error("Error inesperado en el trabajo de cartografía {}: {}", job.getJobId(), e.getMessage(), e);
            result = CartographyUploadResponse.builder()
                    .success(false)
                    .fileName(prepared.getOriginalFilename())
                    .band(prepared.getBand())
                    .message("Error inesperado procesando la carta: " + e.getMessage())
                    .error(e.getMessage())
                    .build();
        } finally {
            activeByChart.remove(chartKey, job.getJobId());
        }
        finish(job, result);
        log.info("Trabajo {} ({}) terminado: {}", job.getJobId(), prepared.getOriginalFilename(), job.getStatus());
    }

    private CartographyJobDto newJob(String fileName, String band, String requestedBy) {
        CartographyJobDto job = CartographyJobDto.builder()
                .jobId(UUID.randomUUID().toString())
                .fileName(fileName)
                .band(band)
                .status("QUEUED")
                .requestedBy(requestedBy)
                .createdAt(Instant.now().toString())
                .build();
        jobs.put(job.getJobId(), job);
        return job;
    }

    private void finish(CartographyJobDto job, CartographyUploadResponse result) {
        job.setResult(result);
        job.setFinishedAt(Instant.now().toString());
        job.setStatus(result != null && result.isSuccess() ? "SUCCESS" : "ERROR");
    }

    private void purgeOldJobs() {
        Instant limit = Instant.now().minus(FINISHED_RETENTION);
        jobs.values().removeIf(j -> j.isFinished() && j.getFinishedAt() != null
                && Instant.parse(j.getFinishedAt()).isBefore(limit));
    }

    @PreDestroy
    void shutdown() {
        worker.shutdownNow();
    }
}
