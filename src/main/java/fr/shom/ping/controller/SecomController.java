package fr.shom.ping.controller;

import fr.shom.ping.exception.BadRequestException;
import fr.shom.ping.exception.ForbiddenException;
import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.model.dto.GetResponseObjectDto;
import fr.shom.ping.model.predicate.SecomGetResponseObjectSearchCriteria;
import fr.shom.ping.properties.PingProperties;
import fr.shom.ping.service.SecomService;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.grad.secom.core.models.CapabilityResponseObject;
import org.grad.secom.core.models.enums.SECOM_DataProductType;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static fr.shom.ping.service.SecomService.PRODUCT_VERSION;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.util.Arrays.asList;
import static org.grad.secom.core.models.enums.ContainerTypeEnum.S100_ExchangeSet;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("/secom")
@RequiredArgsConstructor
@Tag(name = "Secom", description = "Secom API")
@Slf4j
public class SecomController {

    private final PingProperties pingProperties;

    private final SecomService service;

    @GetMapping("/{serviceInstanceId}/v1/ping")
    @Operation(summary = "Check if the service instance by serviceInstanceId is available", tags = "Secom")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "404", description = "Service instance {serviceInstanceId} not available", content = @Content())
    })
    @ResponseBody
    @ResponseStatus(OK)
    @Parameter(name = "serviceInstanceId", description = "ID of the service instance", example = "avurnav_local_brest_fr", required = true)
    public void checkIfServiceInstanceIsAvailable(@PathVariable @NoHtml String serviceInstanceId) {
        try {
            service.checkInstanceServiceExists(serviceInstanceId);
        } catch (NotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Service instance " + serviceInstanceId + " not available");
        }
    }

    @GetMapping(value = "/{serviceInstanceId}/v1/capability", produces = APPLICATION_JSON)
    @Operation(summary = "Get the service instance capabilities by serviceInstanceId", tags = "Secom")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200"),
        @ApiResponse(responseCode = "404", description = "Cannot get capability for serviceInstanceId: {serviceInstanceId}", content = @Content())
    })
    @Parameter(name = "serviceInstanceId", description = "ID of the service instance", example = "avurnav_local_brest_fr", required = true)
    public CapabilityResponseObject capability(@PathVariable @NoHtml String serviceInstanceId) {
        try {
            return service.getCapability(serviceInstanceId);
        } catch (NotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cannot get capability for serviceInstanceId: " + serviceInstanceId, e);
        }
    }

    // /secom/avurnav_local_brest_fr/v1/object?dataReference=98514269-7b05-4623-ad29-b2875ec049a3&containerType=1&dataProductType=S124&productVersion=1.0.0&page=0&pageSize=10
    // /secom/avurnav_local_brest_fr/v1/object?containerType=1&dataProductType=S124&productVersion=1.0.0&page=0&pageSize=10
    @GetMapping(value = "/{serviceInstanceId}/v1/object", produces = APPLICATION_JSON)
    @Operation(summary = "Obtain S-124 navigation warning data from service instance", tags = "Secom")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "totalItem indicates the number of navigational warnings in force that the service instance should " +
            "normally return. A responseText is used when navigational warning data is missing in the response."),
        @ApiResponse(responseCode = "400", description = "Incorrect request", content = @Content()),
        @ApiResponse(responseCode = "404", description = "Information not found", content = @Content()),
        @ApiResponse(responseCode = "403", description = "Not authorized to access requested information", content = @Content())
    })
    @Parameter(name = "serviceInstanceId", description = "ID of the service instance", example = "avurnav_local_brest_fr", required = true)
    @Parameter(name = "searchCriteria", description = "Search criteria are optional (use {} if no criterion). With regard to the criteria laid down in the " +
        "SECOM standard, this service instance only offers data with \"containerType\": 1, \"dataProductType\": \"S124\". It does not process information " +
        "input productVersion, geometry, unlocode, validFrom, validTo. If used, the dataReference of a navigational warning is the string " +
        "45c70257-318a-49e6-b789-3218e1316b59 in the example: https://portail.ping-info-nautique.fr/avurnav-notice/45c70257-318a-49e6-b789-3218e1316b59 where" +
        " the URL is the link in the PING portal to share the navigational warning.", required = false)
    public GetResponseObjectDto getResponseObject(@PathVariable @NoHtml String serviceInstanceId,
        @ModelAttribute("searchCriteria") @Validated SecomGetResponseObjectSearchCriteria searchCriteria,
        HttpServletRequest request) {

        checkParameters(searchCriteria, request);
        try {
            return service.getResponseObject(serviceInstanceId, searchCriteria);
        } catch (BadRequestException | NotFoundException | ForbiddenException e) {
            throw buildResponseStatusException("Cannot get response object for serviceInstanceId: " + serviceInstanceId, e);
        }
    }

    public static ResponseStatusException buildResponseStatusException(String preambleErrorMessage, Exception e) {
        String message = String.format("%s. Error: %s", preambleErrorMessage, e.getMessage());
        HttpStatus status = switch (e.getClass().getSimpleName()) {
            case "BadRequestException" -> HttpStatus.BAD_REQUEST;
            case "NotFoundException" -> HttpStatus.NOT_FOUND;
            case "ForbiddenException" -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };

        log.error(message, e);
        return new ResponseStatusException(status, message, e);
    }

    private void checkParameters(SecomGetResponseObjectSearchCriteria searchCriteria, HttpServletRequest request) {
        checkAllowedParams(request);

        // if containerType != 1 ==> S100_ExchangeSet then bad request
        if (searchCriteria.getContainerType() != null && searchCriteria.getContainerType() != S100_ExchangeSet.getValue()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Wrong required parameter value: containerType. Has: " + searchCriteria.getContainerType() + " instead of " + S100_ExchangeSet.getValue() +
                    ". Only S100_ExchangeSet is supported.");
        }

        if (searchCriteria.getDataProductType() != null && searchCriteria.getDataProductType() != SECOM_DataProductType.S124) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wrong required parameter value: dataProductType S124 only is supported.");
        }

        if (searchCriteria.getProductVersion() != null && !PRODUCT_VERSION.equals(searchCriteria.getProductVersion())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Wrong required parameter value: productVersion. Only %s is supported.".formatted(PRODUCT_VERSION));
        }

        // if dataReference is present, page should always be 1 because we only have one object
        if (searchCriteria.getDataReference() != null && searchCriteria.getPage() != null && searchCriteria.getPage() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wrong required parameter value: page. Should be 1 when dataReference is present.");
        }

        final int maxPageSize = pingProperties.getSecomPageSizeMax();

        if (searchCriteria.getPageSize() != null && (searchCriteria.getPageSize() < 1 || searchCriteria.getPageSize() > maxPageSize)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Wrong required parameter value: pageSize. Should be in closed range: [1, " + maxPageSize + "].");
        }
    }

    private static void checkAllowedParams(HttpServletRequest request) {
        // unlocode, validFrom, validTo, geometry are not used for now but set here to avoid validation errors. These parameters are just ignored.
        List<String> allowedParams = asList(
            "dataReference", "containerType", "dataProductType", "productVersion", "page", "pageSize", "geometry", "unlocode", "validFrom", "validTo"
        );

        Map<String, String[]> requestParams = request.getParameterMap();

        requestParams.keySet().stream()
            .filter(param -> !allowedParams.contains(param))
            .findFirst()
            .ifPresent(param -> { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid parameter: " + param); });
    }
}
