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
@DiscriminatorValue("INFOSOURCE")
public class NotionInfosource extends Notion {
    @Serial
    private static final long serialVersionUID = -5974813624931127891L;

    @OneToOne
    @JoinColumn(name = "infosource_id")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Schema(description = "Infosource linked to a folder", required = true)
    private Infosource infosource;

    public NotionInfosource(Infosource infosource) {
        super();
        this.infosource = infosource;
    }
}
