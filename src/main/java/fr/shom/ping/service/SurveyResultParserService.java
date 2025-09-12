package fr.shom.ping.service;

import fr.shom.ping.exception.ConvertException;
import fr.shom.ping.exception.CsvParsingException;
import fr.shom.ping.model.SurveyDanger;
import fr.shom.ping.model.SurveyDetection;
import fr.shom.ping.model.SurveyEnc;
import fr.shom.ping.model.enums.SurveyDangerNature;
import fr.shom.ping.model.enums.SurveyDangerTypeOfValue;
import fr.shom.ping.utils.GeoUtil;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SurveyResultParserService {

    public static final String CSV_SEPARATOR = " ";

    public static final int CSV_NB_OF_COLUMNS = 5;

    public Set<SurveyEnc> parseEncXmlFile(String path) throws IOException, XMLStreamException {
        final Set<SurveyEnc> surveyEncSet = new HashSet<>();
        final XMLInputFactory xmlInputFactory = getXmlInputFactoryInstance();
        final FileInputStream fis = new FileInputStream(path);
        final XMLEventReader reader = xmlInputFactory.createXMLEventReader(fis);
        SurveyEnc surveyEnc = new SurveyEnc();
        while (reader.hasNext()) {
            XMLEvent nextEvent = reader.nextEvent();
            if (nextEvent.isStartElement()) {
                final StartElement startElement = nextEvent.asStartElement();
                switch (startElement.getName().getLocalPart()) {
                    case "ENC":
                        surveyEnc = new SurveyEnc();
                        break;
                    case "id":
                        nextEvent = reader.nextEvent();
                        surveyEnc.setName(nextEvent.asCharacters().getData());
                        break;
                    case "updateNumber":
                        nextEvent = reader.nextEvent();
                        surveyEnc.setUpdateNumber(Float.parseFloat(nextEvent.asCharacters().getData()));
                        break;
                    case "updateDate":
                        nextEvent = reader.nextEvent();
                        surveyEnc.setUpdateDate(convertDateToInstant(nextEvent.asCharacters().getData()));
                        break;
                    case "comments":
                        nextEvent = reader.nextEvent();
                        surveyEnc.setComments(nextEvent.asCharacters().getData());
                        break;
                }
            }

            if (nextEvent.isEndElement()) {
                final EndElement endElement = nextEvent.asEndElement();
                if ("ENC".equals(endElement.getName().getLocalPart())) {
                    surveyEncSet.add(surveyEnc);
                }
            }
        }

        // free resources
        reader.close();
        fis.close();

        return surveyEncSet;
    }

    public Set<SurveyDanger> parseDangerGmlFile(String path) throws IOException, XMLStreamException {
        final Set<SurveyDanger> surveyDangerSet = new HashSet<>();
        final XMLInputFactory xmlInputFactory = getXmlInputFactoryInstance();
        final FileInputStream fis = new FileInputStream(path);
        final XMLEventReader reader = xmlInputFactory.createXMLEventReader(fis);

        SurveyDanger surveyDanger = new SurveyDanger();
        String shape = "";
        while (reader.hasNext()) {
            XMLEvent nextEvent = reader.nextEvent();
            if (nextEvent.isStartElement()) {
                final StartElement startElement = nextEvent.asStartElement();
                switch (startElement.getName().getLocalPart()) {
                    case "LessWaterFeaturePart":
                        surveyDanger = new SurveyDanger();
                        break;
                    case "S100:Polygon":
                    case "S100:Point":
                        nextEvent = reader.nextEvent();
                        shape = startElement.getName().getLocalPart().replace("S100:", "");
                        break;
                    case "gml:pos":
                    case "gml:posList":
                        nextEvent = reader.nextEvent();
                        final String coordinates = nextEvent.asCharacters().getData();
                        try {
                            final Geometry geometry = buildGeometryInverted(shape, coordinates);
                            geometry.setSRID(3857);
                            surveyDanger.setGeometry(geometry);
                        } catch (ConvertException e) {
                            log.error("Failed to build geometry from shape {} with coordinates {}", shape, coordinates, e);
                        }
                        break;
                    case "typeOfvalue":
                        nextEvent = reader.nextEvent();
                        final String typeOfValue = nextEvent.asCharacters().getData();
                        surveyDanger.setTypeOfValue(SurveyDangerTypeOfValue.valueOf(typeOfValue.toUpperCase()));
                        break;
                    case "nature":
                        nextEvent = reader.nextEvent();
                        final String nature = nextEvent.asCharacters().getData();
                        surveyDanger.setNature(SurveyDangerNature.valueOf(nature.toUpperCase()));
                        break;
                    case "value":
                        nextEvent = reader.nextEvent();
                        final String value = nextEvent.asCharacters().getData();
                        surveyDanger.setValue(Float.parseFloat(value));
                        break;
                }
            }

            if (nextEvent.isEndElement()) {
                final EndElement endElement = nextEvent.asEndElement();
                if ("LessWaterFeaturePart".equals(endElement.getName().getLocalPart())) {
                    surveyDangerSet.add(surveyDanger);
                }
            }
        }

        // free resources
        reader.close();
        fis.close();

        return surveyDangerSet;
    }

    public Set<SurveyDetection> parseDetectionCsvFile(String path) throws IOException {
        final Set<SurveyDetection> surveyDetectionSet = new HashSet<>();
        try (Stream<String> lines = Files.lines(Path.of(path))) {
            lines.forEach(line -> {
                // lat,lon,depth,difference,color
                final String[] splitted = line.split(CSV_SEPARATOR);
                if (splitted.length != CSV_NB_OF_COLUMNS) {
                    throw new RuntimeException(new CsvParsingException(
                        "Wrong number of columns found in csv " + splitted.length + " instead of " + CSV_NB_OF_COLUMNS + " at line " + line
                    ));
                }
                final String coordinates = splitted[1] + " " + splitted[0];
                Geometry geometry;
                try {
                    geometry = buildGeometry(Point.TYPENAME_POINT, coordinates);
                } catch (ConvertException e) {
                    throw new RuntimeException(e);
                }
                geometry.setSRID(3857);
                surveyDetectionSet.add(
                    SurveyDetection.builder().geometry(geometry)
                        .depth(Float.parseFloat(splitted[2]))
                        .difference(Float.parseFloat(splitted[3]))
                        .color(Short.parseShort(splitted[4]))
                        .build()
                );
            });
        }
        return surveyDetectionSet;
    }

    private Geometry buildGeometry(String shape, String coordinates) throws ConvertException {
        // assume that coordinates from Cartonaut are always 4326
        return GeoUtil.convertCoordinatesFromEpsg4326To3857(shape, coordinates.split(" "));
    }

    private Geometry buildGeometryInverted(String shape, String coordinates) throws ConvertException {
        // assume that coordinates from Cartonaut are always 4326
        return GeoUtil.convertCoordinatesFromEpsg4326To3857(shape, true, coordinates.split(" "));
    }

    private static Instant convertDateToInstant(String date) {
        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        final LocalDate localDate = LocalDate.parse(date, dateTimeFormatter);
        return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    private static XMLInputFactory getXmlInputFactoryInstance() {
        final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
        return xmlInputFactory;
    }
}
