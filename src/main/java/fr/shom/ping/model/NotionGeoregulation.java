package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@SuperBuilder
@Entity
@DiscriminatorValue("GEOREGULATION")
public class NotionGeoregulation extends Notion {
    @Serial
    private static final long serialVersionUID = 5938172238308776913L;

    @OneToOne
    @JoinColumn(name = "georegulation_id")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Schema(description = "Georegulation linked to a folder", required = true)
    private Georegulation georegulation;
}

