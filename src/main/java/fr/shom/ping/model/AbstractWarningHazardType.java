package fr.shom.ping.model;

import java.io.Serial;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@MappedSuperclass
@SuperBuilder
public abstract class AbstractWarningHazardType extends AbstractEnumTypes {
    @Serial
    private static final long serialVersionUID = -4208502523597911936L;
}
