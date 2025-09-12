package fr.shom.ping.model.predicate;

import fr.shom.ping.validation.NoHtml;
import fr.shom.ping.validation.ValidContainerTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.grad.secom.core.models.enums.ContainerTypeEnum;
import org.grad.secom.core.models.enums.SECOM_DataProductType;

import static fr.shom.ping.service.SecomService.PRODUCT_VERSION;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Secom GetResponseObject search criteria")
public class SecomGetResponseObjectSearchCriteria {

    @Schema(description = "object ID", example = "b72eb02d-61cc-4577-90c4-cfd83432bf46", requiredMode = NOT_REQUIRED)
    private UUID dataReference;

    @Schema(description = "Type of container", requiredMode = NOT_REQUIRED, example = "1")
    @ValidContainerTypeEnum(enumClass = ContainerTypeEnum.class, message = "validation.get-response-object.search-criteria.containerType.not-in-enum")
    private Integer containerType;

    @Schema(description = "Data product type", example = "S124", requiredMode = NOT_REQUIRED)
    private SECOM_DataProductType dataProductType;

    @Schema(description = "Product version based on S-100", example = PRODUCT_VERSION, requiredMode = NOT_REQUIRED)
    @NoHtml
    private String productVersion;

    @Schema(description = "Pagination's page requested", requiredMode = NOT_REQUIRED, example = "1")
    @Min(0)
    private Integer page;

    @Schema(description = "Pagination's size requested", requiredMode = NOT_REQUIRED, example = "10")
    @Min(0)
    private Integer pageSize;
}
