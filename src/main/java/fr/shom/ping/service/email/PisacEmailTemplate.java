package fr.shom.ping.service.email;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * PISAC (F5.3.7): plantilla común de los correos que envía el backend.
 *
 * <p>Estructura (tablas e estilos en línea, compatible con Outlook, Gmail y clientes móviles):
 * cabecera blanca con el logo institucional y el nombre de PISAC, franja marina con acento naranja,
 * contenido en tarjeta blanca, firma y avisos legales, y franja inferior marina.</p>
 *
 * <p>Seguridad:</p>
 * <ul>
 *   <li>El logo va <b>embebido como adjunto en línea</b> ({@code cid:}), no como imagen remota: el correo no
 *   hace peticiones a ningún servidor al abrirse (sin rastreo de apertura ni exposición de la URL o IP del
 *   portal) y se ve aunque el cliente bloquee las imágenes externas. Tampoco se usa {@code data:} (Gmail y
 *   Outlook lo bloquean y los filtros antispam lo penalizan).</li>
 *   <li>El contenido variable (títulos de suscripción, textos de avisos, enlaces de regulaciones) se sanea con
 *   una política estricta para correo: solo texto, saltos de línea, párrafos, listas, énfasis y enlaces
 *   {@code http}, {@code https} o {@code mailto}. Sin imágenes, estilos, formularios ni scripts.</li>
 *   <li>La cabecera y el pie son fijos y se añaden <b>después</b> del saneado; ningún dato de usuario entra en
 *   ellos. El asunto se escapa antes de usarse en el título del documento.</li>
 *   <li>Se genera también la versión de texto plano (multipart/alternative).</li>
 * </ul>
 */
@Component
public class PisacEmailTemplate {

    /** Content-ID del logo adjunto en línea. */
    public static final String LOGO_CID = "pisac-logo";

    /** Logo institucional (192 × 111 px, se muestra a 96 px de ancho para pantallas de alta densidad). */
    public static final String LOGO_CLASSPATH = "email/pisac-logo.png";

    public static final String LOGO_CONTENT_TYPE = "image/png";

    private static final String NAVY = "#002B5C";
    private static final String NAVY_DEEP = "#001D3D";
    private static final String ORANGE = "#F47C20";
    private static final String DIVIDER = "#CBD5E1";
    private static final String INK = "#253746";
    private static final String MUTED = "#475569";
    private static final String SUBTLE = "#64748B";
    private static final String BORDER = "#E2E8F0";
    private static final String PAGE_BG = "#F1F5F9";
    private static final String LINK = "#1F7FA6";
    private static final String FONT = "'Source Sans 3','Segoe UI',Roboto,Arial,Helvetica,sans-serif";

    private static final String CONTACT_EMAIL = "shnser@shn.gob.ve";

    /** Política estricta para el contenido variable de los correos. */
    private static final PolicyFactory EMAIL_CONTENT_POLICY = new HtmlPolicyBuilder()
        .allowElements("p", "br", "div", "span", "strong", "b", "em", "i", "u", "ul", "ol", "li", "blockquote")
        .allowElements("a")
        .allowUrlProtocols("http", "https", "mailto")
        .allowAttributes("href").onElements("a")
        .requireRelsOnLinks("noopener", "noreferrer")
        .toFactory();

    private static final Pattern ANCHOR_OPEN = Pattern.compile("<a\\s", Pattern.CASE_INSENSITIVE);

    /** Marca que identifica un correo ya maquetado con esta plantilla. */
    static final String LAYOUT_MARKER = "<!-- pisac-email-layout -->";

    /**
     * Sanea el contenido variable con la política de correo y da estilo a los enlaces.
     */
    public String sanitizeContent(String html) {
        final String sanitized = EMAIL_CONTENT_POLICY.sanitize(StringUtils.defaultString(html));
        return ANCHOR_OPEN.matcher(sanitized)
            .replaceAll(Matcher.quoteReplacement("<a style=\"color:" + LINK + ";font-weight:600;text-decoration:underline;\" "));
    }

    public boolean isAlreadyRendered(String html) {
        return html != null && html.contains(LAYOUT_MARKER);
    }

    public Resource logo() {
        return new ClassPathResource(LOGO_CLASSPATH);
    }

    /**
     * Documento HTML completo: cabecera + contenido (ya saneado) + pie institucional.
     *
     * @param subject     asunto del correo (se escapa; se usa como título y texto de vista previa)
     * @param safeContent contenido devuelto por {@link #sanitizeContent(String)}
     * @param withLogo    {@code true} si el mensaje lleva el logo adjunto en línea ({@link #LOGO_CID})
     */
    public String renderHtml(String subject, String safeContent, boolean withLogo) {
        final String title = StringEscapeUtils.escapeHtml4(StringUtils.defaultIfBlank(subject, "PISAC"));
        final StringBuilder html = new StringBuilder(8192);
        html.append("<!DOCTYPE html>").append(LAYOUT_MARKER)
            .append("<html lang=\"es\"><head><meta charset=\"utf-8\">")
            .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
            .append("<meta name=\"x-apple-disable-message-reformatting\">")
            .append("<title>").append(title).append("</title></head>")
            .append("<body style=\"margin:0;padding:0;background-color:").append(PAGE_BG).append(";\">")
            // Texto de vista previa (oculto)
            .append("<div style=\"display:none;max-height:0;overflow:hidden;mso-hide:all;font-size:1px;line-height:1px;color:")
            .append(PAGE_BG).append(";\">").append(title).append("</div>")
            .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"background-color:")
            .append(PAGE_BG).append(";\"><tr><td align=\"center\" style=\"padding:24px 12px;\">")
            .append("<table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"width:100%;max-width:600px;background-color:#ffffff;border:1px solid ")
            .append(BORDER).append(";border-radius:12px;border-collapse:separate;overflow:hidden;font-family:").append(FONT).append(";\">");

        appendHeader(html, withLogo);

        // Contenido
        html.append("<tr><td style=\"padding:32px 32px 28px 32px;font-family:").append(FONT)
            .append(";font-size:15px;line-height:1.6;color:").append(INK).append(";\">")
            .append("<h1 style=\"margin:0 0 16px 0;font-family:Montserrat,").append(FONT)
            .append(";font-size:20px;line-height:1.3;font-weight:700;color:").append(NAVY).append(";\">")
            .append(title).append("</h1>")
            .append(safeContent)
            .append("</td></tr>");

        appendFooter(html);

        html.append("</table></td></tr></table></body></html>");
        return html.toString();
    }

    /**
     * Versión de texto plano del mismo mensaje (para clientes sin HTML y filtros antispam).
     */
    public String renderText(String subject, String safeContent) {
        final Document doc = Jsoup.parseBodyFragment(StringUtils.defaultString(safeContent));
        final StringBuilder text = new StringBuilder();
        text.append(StringUtils.defaultIfBlank(subject, "PISAC")).append("\n\n");
        appendText(doc.body(), text);
        text.append("\n\n--\n")
            .append("PISAC - Plataforma de Información de Seguridad Acuática\n")
            .append("Servicio de Hidrografía, Oceanografía, Meteorología y Cartografiado Náutico (SHN - Venezuela)\n\n")
            .append("La plataforma PISAC está implementada únicamente para las aguas jurisdiccionales de la República Bolivariana de Venezuela.\n")
            .append("La disponibilidad de las advertencias de navegación en esta plataforma no exime a los capitanes de la obligación de recibir en el mar ")
            .append("las advertencias de navegación a través de los sistemas de difusión oficiales, de conformidad con las disposiciones del Convenio SOLAS.\n")
            .append("La plataforma PISAC presenta una selección de decretos marítimos. Para mayor información consultar las publicaciones de las organismos competentes.\n\n")
            .append("No responda ni reenvíe correos a esta cuenta: es una cuenta no monitoreada. Consultas: ").append(CONTACT_EMAIL).append('\n');
        return text.toString().replaceAll("\n{3,}", "\n\n");
    }

    private void appendText(Node node, StringBuilder out) {
        for (Node child : node.childNodes()) {
            if (child instanceof TextNode textNode) {
                out.append(textNode.text());
            } else if (child instanceof Element element) {
                final String tag = element.tagName();
                if ("br".equals(tag)) {
                    out.append('\n');
                    continue;
                }
                if ("li".equals(tag)) {
                    out.append("\n- ");
                }
                appendText(element, out);
                if ("a".equals(tag)) {
                    final String href = element.attr("href");
                    if (StringUtils.isNotBlank(href) && !href.equals(element.text())) {
                        out.append(" (").append(href).append(')');
                    }
                } else if ("p".equals(tag) || "div".equals(tag) || "ul".equals(tag) || "ol".equals(tag) || "blockquote".equals(tag)) {
                    out.append('\n');
                }
            }
        }
    }

    /**
     * Cabecera institucional (igual que el encabezado del portal): logos SHN / OCHINA, línea fina gris y nombre de
     * PISAC. Sin franja de gobierno: la imagen del Ministerio y la Armada solo va en el pie del portal.
     */
    private void appendHeader(StringBuilder html, boolean withLogo) {
        html.append("<tr><td style=\"padding:20px 28px;background-color:#ffffff;\">")
            .append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr>");
        if (withLogo) {
            html.append("<td valign=\"middle\" style=\"padding-right:14px;\">")
                .append("<img src=\"cid:").append(LOGO_CID).append("\" width=\"96\" height=\"56\" alt=\"SHN · OCHINA\" ")
                .append("style=\"display:block;width:96px;height:auto;border:0;outline:none;text-decoration:none;\">")
                .append("</td>");
        }
        // Línea fina gris (antes barra naranja), igual a la del portal
        html.append("<td valign=\"middle\" style=\"padding-right:14px;\">")
            .append("<div style=\"width:1px;height:40px;font-size:0;line-height:0;background-color:").append(DIVIDER).append(";\"></div>")
            .append("</td>")
            .append("<td valign=\"middle\" style=\"font-family:").append(FONT).append(";\">")
            .append("<div style=\"font-family:Montserrat,").append(FONT).append(";font-size:18px;line-height:1.25;font-weight:700;color:")
            .append(NAVY).append(";\">PISAC</div>")
            .append("<div style=\"font-size:13px;line-height:1.3;font-weight:600;color:").append(NAVY)
            .append(";\">Plataforma de Informaci&oacute;n de Seguridad Acu&aacute;tica</div>")
            .append("</td></tr></table></td></tr>")
            // Franja marina con acento naranja (como la barra de menú del portal)
            .append("<tr><td style=\"height:6px;line-height:6px;font-size:0;background-color:").append(NAVY).append(";\">&nbsp;</td></tr>")
            .append("<tr><td style=\"height:3px;line-height:3px;font-size:0;background-color:").append(ORANGE).append(";\">&nbsp;</td></tr>");
    }

    private void appendFooter(StringBuilder html) {
        html.append("<tr><td style=\"padding:24px 32px;background-color:#F8FAFC;border-top:1px solid ").append(BORDER)
            .append(";font-family:").append(FONT).append(";font-size:12px;line-height:1.5;color:").append(MUTED).append(";\">")
            .append("<p style=\"margin:0 0 12px 0;font-size:13px;line-height:1.45;color:").append(NAVY).append(";\">")
            .append("<strong>Atentamente,<br>PISAC - Plataforma de Informaci&oacute;n de Seguridad Acu&aacute;tica</strong><br>")
            .append("<span style=\"color:").append(MUTED).append(";\">Servicio de Hidrograf&iacute;a, Oceanograf&iacute;a, Meteorolog&iacute;a y ")
            .append("Cartografiado N&aacute;utico (SHN - Venezuela)</span></p>")
            .append("<p style=\"margin:0 0 8px 0;font-size:11px;color:").append(SUBTLE).append(";\">")
            .append("La plataforma PISAC est&aacute; implementada &uacute;nicamente para las aguas jurisdiccionales de la Rep&uacute;blica Bolivariana de Venezuela.</p>")
            .append("<p style=\"margin:0 0 8px 0;font-size:11px;color:").append(SUBTLE).append(";\">")
            .append("La disponibilidad de las advertencias de navegaci&oacute;n en esta plataforma no exime a los capitanes de la obligaci&oacute;n de recibir ")
            .append("en el mar las advertencias de navegaci&oacute;n a trav&eacute;s de los sistemas de difusi&oacute;n oficiales, de conformidad con las ")
            .append("disposiciones del Convenio SOLAS.</p>")
            .append("<p style=\"margin:0 0 12px 0;font-size:11px;color:").append(SUBTLE).append(";\">")
            .append("La plataforma PISAC presenta una selecci&oacute;n de decretos mar&iacute;timos. Para mayor informaci&oacute;n consultar las publicaciones ")
            .append("de las organismos competentes.</p>")
            .append("<p style=\"margin:0;padding-top:10px;border-top:1px dashed #CBD5E1;font-size:11px;color:").append(SUBTLE).append(";\">")
            .append("No responda ni reenv&iacute;e correos a esta cuenta: es una cuenta no monitoreada. ")
            .append("Para cualquier consulta, escriba a <a href=\"mailto:").append(CONTACT_EMAIL).append("\" style=\"color:").append(LINK)
            .append(";font-weight:600;text-decoration:none;\">").append(CONTACT_EMAIL).append("</a>.</p>")
            .append("</td></tr>")
            .append("<tr><td align=\"center\" style=\"padding:10px 24px;background-color:").append(NAVY_DEEP)
            .append(";font-family:").append(FONT).append(";font-size:11px;line-height:1.4;color:#DCE5F0;\">")
            .append("PISAC &middot; SHN Venezuela &middot; Correo autom&aacute;tico")
            .append("</td></tr>");
    }
}
