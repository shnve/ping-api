package fr.shom.ping.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

import static fr.shom.ping.utils.SchemaUtil.WARNING_HAZARD_TYPE_GENERAL;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = WARNING_HAZARD_TYPE_GENERAL)
public class WarningHazardTypeGeneralEntity extends AbstractWarningHazardType {
    @Serial
    private static final long serialVersionUID = 8920524050987045525L;

    @Column(nullable = false)
    private Boolean bulletin;
}
