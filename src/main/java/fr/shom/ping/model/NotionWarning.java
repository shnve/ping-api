package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@SuperBuilder
@Entity
@DiscriminatorValue("WARNING")
public class NotionWarning extends Notion {
    @Serial
    private static final long serialVersionUID = 8378272702843116350L;

    @OneToOne
    @JoinColumn(name = "warning_id")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Schema(description = "Warning linked to a folder", required = true)
    private Warning warning;
}
