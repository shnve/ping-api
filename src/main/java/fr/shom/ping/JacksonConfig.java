package fr.shom.ping;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.n52.jackson.datatype.jts.JtsModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;

import static com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator.Feature.WRITE_XML_DECLARATION;
import static org.locationtech.jts.geom.PrecisionModel.FLOATING;

/**
 * Declare the JtsModule to serialize/deserialize JSON property of geometry type
 */
@Configuration
public class JacksonConfig {

    @Bean
    public JtsModule jtsModule() {
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(FLOATING), 3857);
        return new JtsModule(geometryFactory);
    }

    @Bean
    public MappingJackson2XmlHttpMessageConverter mappingJackson2XmlHttpMessageConverter(Jackson2ObjectMapperBuilder builder) {
        XmlMapper xmlMapper = builder.createXmlMapper(true).build();
        xmlMapper.configure(WRITE_XML_DECLARATION, true);
        return new MappingJackson2XmlHttpMessageConverter(xmlMapper);
    }
}
