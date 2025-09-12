package fr.shom.ping.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DiffusionNavtexType {
    A("A"), // "Avertissements aux navigateurs"
    J("J"), // "Messages concernant les satellites de navigation"
    K("K"); // "Messages concernant d'autres aides électroniques à la navigation"

    private String symbol;
}
