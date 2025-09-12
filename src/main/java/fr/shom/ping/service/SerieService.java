package fr.shom.ping.service;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.Serie;
import fr.shom.ping.model.SubArea;
import fr.shom.ping.model.dto.SerieCreateDto;
import fr.shom.ping.model.dto.SerieDetailsDto;
import fr.shom.ping.model.dto.SerieDto;
import fr.shom.ping.model.dto.SubAreaDto;
import fr.shom.ping.model.predicate.SerieSearchCriteria;
import fr.shom.ping.model.tuple.SerieTuple;
import fr.shom.ping.repository.SerieRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static fr.shom.ping.model.QSerie.serie;
import static fr.shom.ping.model.QSubArea.subArea;
import static fr.shom.ping.model.enums.ServiceRole.SERVICE_ADMIN;
import static fr.shom.ping.model.enums.ServiceRole.SERVICE_OPERATOR;
import static java.util.Arrays.asList;
import static org.springframework.util.ObjectUtils.isEmpty;

@RequiredArgsConstructor
@Service
public class SerieService {
    private final EntityManager entityManager;
    private final SerieRepository repository;
    private final MaritimeServiceService maritimeServiceService;
    private final SubAreaService subAreaService;
    private final JdbcTemplate jdbcTemplate;

    public List<Serie> findAll() {
        return repository.findAll();
    }

    public List<SerieDto> findAllBySearchCriteria(SerieSearchCriteria searchCriteria) {
        JPAQuery<SerieTuple> query = new JPAQuery<Void>(entityManager)
            .select(Projections.constructor(
                SerieTuple.class,
                serie.id,
                serie.name,
                serie.internationalName,
                serie.type,
                subArea.id,
                subArea.name,
                subArea.internationalName))
            .from(serie)
            .leftJoin(serie.subAreas, subArea);

        if (!isEmpty(searchCriteria.getAreaId())) {
            query.where(serie.maritimeService.areas.any().id.eq(searchCriteria.getAreaId()));
        }

        Map<UUID, SerieDto> series = new HashMap<>();
        query.fetch().forEach(tuple -> {
            if (!series.containsKey(tuple.getId())) {
                SerieDto serieDto = SerieDto.builder()
                    .id(tuple.getId())
                    .name(tuple.getName())
                    .internationalName(tuple.getInternationalName())
                    .type(tuple.getType())
                    .build();

                serieDto.setMandatoryLanguages(findById(serieDto.getId()).getMandatoryLanguages());

                series.put(serieDto.getId(), serieDto);
            }

            if (tuple.getSubAreaId() != null) {
                SubAreaDto subAreaDto = SubAreaDto.builder()
                    .id(tuple.getSubAreaId())
                    .name(tuple.getSubAreaName())
                    .internationalName(tuple.getSubAreaInternationalName())
                    .build();

                series.get(tuple.getId()).getSubAreas().add(subAreaDto);
            }
        });

        return new ArrayList<>(series.values());
    }

    public Page<Serie> search(Pageable pageable) {
        return repository.findAll(PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("name")));
    }

    @Transactional
    public SerieDetailsDto create(SerieCreateDto dto) {
        List<SubArea> subAreaList = this.subAreaService.findAllById(new ArrayList<>(dto.getSubAreas()));
        MaritimeService maritimeService = this.maritimeServiceService.findById(dto.getMaritimeService());
        return this.repository.save(new Serie(dto, maritimeService, subAreaList)).asDto();
    }

    @Transactional
    public SerieDetailsDto update(UUID id, SerieCreateDto dto) {
        List<SubArea> subAreaList = this.subAreaService.findAllById(new ArrayList<>(dto.getSubAreas()));
        MaritimeService maritimeService = this.maritimeServiceService.findById(dto.getMaritimeService());
        Serie serie = new Serie(dto, maritimeService, subAreaList);
        serie.setId(id);
        return this.repository.save(serie).asDto();
    }

    public List<Serie> findAllByServiceIds(List<UUID> serviceIds) {
        return repository.findAllByMaritimeServiceIdIn(serviceIds);
    }

    public Serie findById(UUID id) {
        return repository.findById(id).orElseThrow(NotFoundException::new);
    }

    public List<SerieDto> findByUserWithAdminOrOperatorRole(String username) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("username", username);
        parameters.addValue("roles", asList(SERVICE_ADMIN.name(), SERVICE_OPERATOR.name()));

        return new NamedParameterJdbcTemplate(jdbcTemplate).query(
            "SELECT s.id as id, s.name as name, s.international_name " +
                "FROM serie as s " +
                "INNER JOIN maritime_service as ms ON ms.id = s.maritime_service_id " +
                "INNER JOIN service_user_role as r ON ms.id = r.service_id " +
                "INNER JOIN pro_user u ON r.service_user_id = u.id " +
                "WHERE u.email = :username " +
                "AND r.service_role IN (:roles) " +
                "ORDER BY s.name", parameters, new BeanPropertyRowMapper<>(SerieDto.class)
        );
    }

    public Serie findByIdAndUserWithAdminOrOperatorRole(UUID id, UUID serviceUserId) {
        return repository.findByIdAndUserWithAdminOrOperatorRole(id, serviceUserId);
    }
}
