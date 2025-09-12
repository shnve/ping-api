package fr.shom.ping.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "warning_public_comment")
public class WarningPublicComment implements Serializable {
    @Serial
    private static final long serialVersionUID = 7951508773005951252L;

    @Id
    private UUID id;

    @OneToOne
    private PublicUser publicUser;

    private String comment;

}
