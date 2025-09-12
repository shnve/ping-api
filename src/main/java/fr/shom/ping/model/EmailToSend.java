package fr.shom.ping.model;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailToSend implements Serializable {

    private String recipients;
    private String subject;
    private String body;
}
