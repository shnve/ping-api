package fr.shom.ping.service;

import fr.shom.ping.exception.S124Exception;
import fr.shom.ping.model.Serie;
import fr.shom.ping.model.Warning;
import fr.shom.ping.model.dto.AbstractS124ExchangeSetBaseDto;
import fr.shom.ping.repository.SerieRepository;
import fr.shom.ping.repository.WarningRepository;
import fr.shom.ping.utils.S124Util;
import fr.shom.ping.utils.S124v2Util;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static fr.shom.ping.model.QWarning.warning;
import static fr.shom.ping.model.enums.WarningStatus.PUBLISHED;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
@RequiredArgsConstructor
@Service
public class S124Service {

    private final WarningRepository repository;
    private final SerieRepository serieRepository;

    /***
     * La secom et S124 v2 a été mis en place en se basant sur la version 1.5.0 de la S-124.
     */
    private final S124v2Util s124v2Util;

    public static final String VERSION_1 = "v1";
    public static final String VERSION_2 = "v2";

    public AbstractS124ExchangeSetBaseDto findByNameOfSeries(String version, String nameOfSeries, String lang) {
        Serie serie = getSerie(nameOfSeries, lang);

        List<Warning> warnings = repository.findAll(
            warning.serie.eq(serie)
                .and(warning.status.eq(PUBLISHED))
        );

        if (VERSION_1.equals(version)) {
            return S124Util.buildS124ExchangeSetDtoFromWarnings(warnings, lang);
        } else {
            return s124v2Util.buildS124ExchangeSetDtoFromWarnings(warnings, lang);
        }
    }

    public AbstractS124ExchangeSetBaseDto findByNameOfSeriesAndWarningNumberAndYear(String version, String nameOfSeries,
        Integer warningNumber, Integer year, String lang) {
        Warning warning = getWarning(nameOfSeries, warningNumber, year, lang);

        if (VERSION_1.equals(version)) {
            return S124Util.buildS124ExchangeSetDtoFromWarnings(Collections.singletonList(warning), lang);
        } else {
            return s124v2Util.buildS124ExchangeSetDtoFromWarnings(Collections.singletonList(warning), lang);
        }
    }

    public AbstractS124ExchangeSetBaseDto findByNameOfSeriesAndWarningNumberAndYearList(String version, String nameOfSeries,
        List<String> warningNumbers, String lang) {
        final List<Warning> warnings = getWarnings(nameOfSeries, warningNumbers, lang);

        if (VERSION_1.equals(version)) {
            return S124Util.buildS124ExchangeSetDtoFromWarnings(warnings, lang);
        } else {
            return s124v2Util.buildS124ExchangeSetDtoFromWarnings(warnings, lang);
        }
    }

    public AbstractS124ExchangeSetBaseDto getInForceNwMessageIdentifiers(String version, String nameOfSeries) {
        final List<Warning> warnings = getPublishedWarnings(nameOfSeries);

        if (VERSION_1.equals(version)) {
            return S124Util.buildS124ExchangeSetDtoFromInForceWarnings(warnings);
        } else {
            return s124v2Util.buildS124ExchangeSetDtoFromInForceWarnings(warnings);
        }
    }

    public Serie getSerie(String nameOfSeries, String lang) {
        if (!hasText(nameOfSeries)) {
            throw new S124Exception("nameOfSeries is missing");
        }

        Optional<Serie> optionalSerie = serieRepository.findOneByNameIgnoreCase(nameOfSeries);
        if (optionalSerie.isEmpty()) {
            throw new S124Exception("nameOfSeries is unknown");
        }

        Serie serie = optionalSerie.get();
        if (Stream.of(serie.getMandatoryLanguages(), serie.getOptionalLanguages())
            .flatMap(Collection::stream)
            .noneMatch(serieLanguage -> serieLanguage.equals(lang))) {
            throw new S124Exception("lang is not available for this nameOfSeries");
        }
        return serie;
    }

    public Warning getWarning(String nameOfSeries, Integer warningNumber, Integer year, String lang) {
        if (!hasText(nameOfSeries)) {
            throw new S124Exception("nameOfSeries is missing");
        }

        if (warningNumber == null) {
            throw new S124Exception("warningNumber is missing");
        }

        if (year == null) {
            throw new S124Exception("year is missing");
        }

        Optional<Warning> optionalWarning = repository.findOneByNameOfSeriesIgnoreCaseAndWarningNumberAndYearAndStatusIsPublished(
            nameOfSeries, warningNumber, year
        );
        if (optionalWarning.isEmpty()) {
            throw new S124Exception("warning with this nameOfSeries, this warningNumber and this year is unknown or is not published");
        }

        Warning warning = optionalWarning.get();
        if (Stream.of(warning.getSerie().getMandatoryLanguages(), warning.getSerie().getOptionalLanguages())
            .flatMap(Collection::stream)
            .noneMatch(serieLanguage -> serieLanguage.equals(lang))) {
            throw new S124Exception("lang is not available for this nameOfSeries");
        }
        return warning;
    }

    public List<Warning> getWarnings(String nameOfSeries, List<String> warningNumbers, String lang) {
        if (!hasText(nameOfSeries)) {
            throw new S124Exception("nameOfSeries is missing");
        }

        if (warningNumbers == null || warningNumbers.isEmpty()) {
            throw new S124Exception("warningNumbers is missing");
        }

        final List<Warning> warnings = repository.findAllByNameOfSeriesIgnoreCaseAndWarningNumberAndYearListAndStatusIsPublished(nameOfSeries, warningNumbers);

        warnings.forEach(warning -> {
            if (Stream.of(warning.getSerie().getMandatoryLanguages(), warning.getSerie().getOptionalLanguages())
                .flatMap(Collection::stream)
                .noneMatch(serieLanguage -> serieLanguage.equals(lang))) {
                throw new S124Exception("lang is not available for this nameOfSeries");
            }
        });
        return warnings;
    }

    public List<Warning> getPublishedWarnings(String nameOfSeries) {
        if (!hasText(nameOfSeries)) {
            throw new S124Exception("nameOfSeries is missing");
        }

        final Optional<Serie> optionalSerie = serieRepository.findOneByNameIgnoreCase(nameOfSeries);
        if (optionalSerie.isEmpty()) {
            throw new S124Exception("nameOfSeries is unknown");
        }

        final Serie serie = optionalSerie.get();
        return repository.findAll(
            warning.serie.eq(serie)
                .and(warning.status.eq(PUBLISHED))
        );
    }
}
