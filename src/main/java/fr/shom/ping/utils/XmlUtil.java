package fr.shom.ping.utils;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.xmlbeans.XmlException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public final class XmlUtil {
    private XmlUtil() {
    }

    public static String prettyFormat(String input, int indent) throws XmlException {
        try {
            final Source xmlInput = new StreamSource(new StringReader(input));
            final StringWriter stringWriter = new StringWriter();
            final StreamResult xmlOutput = new StreamResult(stringWriter);
            final TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            transformerFactory.setAttribute("indent-number", indent);
            final Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(xmlInput, xmlOutput);
            return xmlOutput.getWriter().toString();
        } catch (TransformerException e) {
            throw new XmlException("Error occurs when pretty-format xml:\n" + input, e);
        }
    }

    public static String prettyPrintByTransformer(String xmlString, int indent, boolean ignoreDeclaration) throws XmlException {
        try {
            InputSource src = new InputSource(new StringReader(xmlString));
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(src);
            document.setXmlStandalone(true);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            transformerFactory.setAttribute("indent-number", indent);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, ignoreDeclaration ? "yes" : "no");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            Writer out = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(out));
            return out.toString();
        } catch (ParserConfigurationException | TransformerException | SAXException | IOException e) {
            throw new XmlException("Error occurs when pretty-printing xml:\n" + xmlString, e);
        }
    }
}
