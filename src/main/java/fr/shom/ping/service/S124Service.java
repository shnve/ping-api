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
import java.util.Comparator;
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

    private static final Comparator<Warning> WARNING_ORDER = Comparator
        .comparing(Warning::getNameOfSeries, Comparator.nullsLast(String::compareToIgnoreCase))
        .thenComparing(Warning::getYear, Comparator.nullsLast(Integer::compareTo))
        .thenComparing(Warning::getWarningNumber, Comparator.nullsLast(Integer::compareTo));

    public AbstractS124ExchangeSetBaseDto findByNameOfSeries(String version, String nameOfSeries, String lang) {
        final List<Warning> warnings;
        if (!hasText(nameOfSeries)) {
            warnings = getPublishedWarningsByLanguage(lang);
        } else {
            Serie serie = getSerie(nameOfSeries, lang);
            warnings = repository.findAll(
                warning.serie.eq(serie)
                    .and(warning.status.eq(PUBLISHED))
            ).stream().sorted(WARNING_ORDER).toList();
        }

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
        return getInForceNwMessageIdentifiers(version, nameOfSeries, S124Util.DEFAULT_LANGUAGE);
    }

    public AbstractS124ExchangeSetBaseDto getInForceNwMessageIdentifiers(String version, String nameOfSeries, String lang) {
        validateLanguageWhenNoSerieSpecified(nameOfSeries, lang);
        final List<Warning> warnings = getPublishedWarnings(nameOfSeries, lang);

        if (VERSION_1.equals(version)) {
            return S124Util.buildS124ExchangeSetDtoFromInForceWarnings(warnings);
        } else {
            return s124v2Util.buildS124ExchangeSetDtoFromInForceWarnings(warnings, lang);
        }
    }

    public Serie getSerie(String nameOfSeries, String lang) {
        if (!hasText(nameOfSeries)) {
            throw new S124Exception("nameOfSeries is missing");
        }

        Optional<Serie> optionalSerie = findSerieByName(nameOfSeries);
        if (optionalSerie.isEmpty()) {
            throw new S124Exception("nameOfSeries is unknown");
        }

        Serie serie = optionalSerie.get();
        if (!isLanguageSupportedBySerie(serie, lang)) {
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
        return getPublishedWarnings(nameOfSeries, null);
    }

    public List<Warning> getPublishedWarnings(String nameOfSeries, String lang) {
        if (!hasText(nameOfSeries)) {
            return repository.findByStatus(PUBLISHED).stream()
                .filter(w -> !hasText(lang) || (w.getSerie() != null && isLanguageSupportedBySerie(w.getSerie(), lang)))
                .sorted(WARNING_ORDER)
                .toList();
        }

        final Optional<Serie> optionalSerie = findSerieByName(nameOfSeries);
        if (optionalSerie.isEmpty()) {
            throw new S124Exception("nameOfSeries is unknown");
        }

        final Serie serie = optionalSerie.get();
        if (hasText(lang) && !isLanguageSupportedBySerie(serie, lang)) {
            throw new S124Exception("lang is not available for this nameOfSeries");
        }
        return repository.findAll(
            warning.serie.eq(serie)
                .and(warning.status.eq(PUBLISHED))
        ).stream().sorted(WARNING_ORDER).toList();
    }

    private List<Warning> getPublishedWarningsByLanguage(String lang) {
        if (!hasText(lang)) {
            throw new S124Exception("lang is not available");
        }
        final List<Serie> allSeries = serieRepository.findAll();
        if (!allSeries.isEmpty() && allSeries.stream().noneMatch(s -> isLanguageSupportedBySerie(s, lang))) {
            throw new S124Exception("lang is not available");
        } else if (allSeries.isEmpty() && !S124Util.DEFAULT_LANGUAGE.equals(lang) && !S124Util.INTERNATIONAL_LANGUAGE.equals(lang)) {
            throw new S124Exception("lang is not available");
        }
        return repository.findByStatus(PUBLISHED).stream()
            .filter(w -> w.getSerie() != null && isLanguageSupportedBySerie(w.getSerie(), lang))
            .sorted(WARNING_ORDER)
            .toList();
    }

    private void validateLanguageWhenNoSerieSpecified(String nameOfSeries, String lang) {
        if (!hasText(nameOfSeries) && hasText(lang)) {
            final List<Serie> allSeries = serieRepository.findAll();
            if (!allSeries.isEmpty() && allSeries.stream().noneMatch(s -> isLanguageSupportedBySerie(s, lang))) {
                throw new S124Exception("lang is not available");
            } else if (allSeries.isEmpty() && !S124Util.DEFAULT_LANGUAGE.equals(lang) && !S124Util.INTERNATIONAL_LANGUAGE.equals(lang)) {
                throw new S124Exception("lang is not available");
            }
        }
    }

    private Optional<Serie> findSerieByName(String nameOfSeries) {
        final String trimmed = nameOfSeries.trim();
        return serieRepository.findOneByNameIgnoreCase(trimmed)
            .or(() -> serieRepository.findOneByInternationalNameIgnoreCase(trimmed));
    }

    private static boolean isLanguageSupportedBySerie(Serie serie, String lang) {
        return Stream.of(serie.getMandatoryLanguages(), serie.getOptionalLanguages())
            .filter(java.util.Objects::nonNull)
            .flatMap(Collection::stream)
            .anyMatch(serieLanguage -> serieLanguage.equalsIgnoreCase(lang));
    }
}
