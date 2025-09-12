package fr.shom.ping.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.io.Serial;
import java.io.Serializable;


@Data
@NoArgsConstructor
@MappedSuperclass
@SuperBuilder
public abstract class AbstractLighthouseAndBeaconMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = 7799702989995550824L;

    @Id
    @Column(nullable = false)
    private String enumName;

    @Column(nullable = false)
    private String translation;

    @Column(nullable = false)
    private String internationalTranslation;
}
