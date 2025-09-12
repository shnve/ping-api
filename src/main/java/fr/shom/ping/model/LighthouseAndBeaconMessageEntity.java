package fr.shom.ping.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import static fr.shom.ping.utils.SchemaUtil.LIGHTHOUSE_BEACON_MESSAGE;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = LIGHTHOUSE_BEACON_MESSAGE)
public class LighthouseAndBeaconMessageEntity extends AbstractLighthouseAndBeaconMessage {
}
