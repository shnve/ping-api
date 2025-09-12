package fr.shom.ping.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DiffusionNavtexEmergencyLevel {
    ROUTINE('R'), // "communication NAVTEX de routine à diffuser au cours de l'émission normale suivante"
    IMPORTANT('I'), // "communication NAVTEX importante à diffuser au cours de la prochaine période de libération de la fréquence NAVTEX"
    VITAL('V'); // "communication NAVTEX vitale à diffuser immédiatement"

    private char letter;
}
