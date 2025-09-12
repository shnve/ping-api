package fr.shom.ping.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

import static fr.shom.ping.utils.SchemaUtil.WARNING_HAZARD_TYPE_DETAILS;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = WARNING_HAZARD_TYPE_DETAILS)
public class WarningHazardTypeDetailsEntity extends AbstractWarningHazardType {
    @Serial
    private static final long serialVersionUID = 1933214495775928518L;
}
