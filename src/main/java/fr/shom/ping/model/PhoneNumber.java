package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.shom.ping.model.dto.PhoneNumberDto;
import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static java.util.UUID.randomUUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class PhoneNumber {

    @Id
    private UUID id;

    private String description;

    private String number;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ProUser user;

    public PhoneNumber(PhoneNumberDto dto) {
        this.id = dto.getId();
        this.description = dto.getDescription();
        this.number = dto.getNumber();
    }

    @PrePersist
    public void prePersist() {
        id = randomUUID();
    }
}
