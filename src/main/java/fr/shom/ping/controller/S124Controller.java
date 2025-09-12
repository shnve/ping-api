package fr.shom.ping.controller;

import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.model.dto.AbstractS124ExchangeSetBaseDto;
import fr.shom.ping.service.S124Service;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static fr.shom.ping.service.S124Service.VERSION_1;
import static fr.shom.ping.service.S124Service.VERSION_2;
import static fr.shom.ping.utils.S124Util.DEFAULT_LANGUAGE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.springframework.util.ObjectUtils.isEmpty;

@RestController
@RequestMapping("/nw")
@RequiredArgsConstructor
public class S124Controller {
    private final S124Service service;

    @GetMapping(value = "/{version}/Get_NW_Messages", produces = APPLICATION_XML_VALUE)
    public AbstractS124ExchangeSetBaseDto getNwMessages(
        @PathVariable String version,
        @RequestParam(required = false) String nameOfSeries,
        @RequestParam(required = false) List<String> warningNumbers,
        @RequestParam(defaultValue = DEFAULT_LANGUAGE) String lang) {
        checkIfversionIsSupported(version);

        if (isEmpty(warningNumbers)) {
            return service.findByNameOfSeries(version, nameOfSeries, lang);
        }
        return service.findByNameOfSeriesAndWarningNumberAndYearList(version, nameOfSeries, warningNumbers, lang);
    }

    @GetMapping(value = "{version}/Get_NW_Message", produces = APPLICATION_XML_VALUE)
    public AbstractS124ExchangeSetBaseDto getNwMessage(
        @PathVariable String version,
        @RequestParam(required = false) String nameOfSeries,
        @RequestParam(required = false) Integer warningNumber,
        @RequestParam(required = false) Integer year,
        @RequestParam(defaultValue = DEFAULT_LANGUAGE) String lang) {
        checkIfversionIsSupported(version);
        return service.findByNameOfSeriesAndWarningNumberAndYear(version, nameOfSeries, warningNumber, year, lang);
    }

    @GetMapping(value = "{version}/Get_in-force_NW_MessageIdentifiers", produces = APPLICATION_XML_VALUE)
    public AbstractS124ExchangeSetBaseDto getInForceNwMessageIdentifiers(
        @PathVariable String version,
        @RequestParam(required = false) String nameOfSeries) {
        checkIfversionIsSupported(version);
        return service.getInForceNwMessageIdentifiers(version, nameOfSeries);
    }

    public static void checkIfversionIsSupported(String version) {
        if (!List.of(VERSION_1, VERSION_2).contains(version)) {
            throw new NotFoundException("Unknown version : " + version);
        }
    }
}
