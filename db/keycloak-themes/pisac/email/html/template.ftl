<#--
  PISAC (F5.3.7): plantilla común de los correos de Keycloak (realms ping-public y ping-pro).
  Mismo diseño que los correos del backend (fr.shom.ping.service.email.PisacEmailTemplate): cabecera
  institucional, franja marina con acento naranja, contenido en tarjeta y pie con firma y avisos legales.

  Seguridad: Keycloak no permite adjuntar imágenes en línea (cid:) en sus correos, y una imagen remota
  (URL del servidor) permitiría rastrear la apertura y expondría la dirección del servidor; data: lo
  bloquean Gmail y Outlook. Por eso la cabecera de estos correos es solo texto (sin logo). Todas las
  variables pasan por el escape HTML automático de FreeMarker.

  Macros:
  - emailLayout title preheader: documento completo; el contenido va en <#nested>.
  - button href label: botón principal (tabla, compatible con Outlook).
  - linkFallback href: enlace en texto para copiar y pegar.
-->
<#macro emailLayout title="" preheader="">
<!DOCTYPE html>
<html lang="${locale.language}" dir="${(ltr)?then('ltr','rtl')}">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="x-apple-disable-message-reformatting">
    <title>${title}</title>
</head>
<body style="margin:0;padding:0;background-color:#F1F5F9;">
<#if preheader?has_content>
    <div style="display:none;max-height:0;overflow:hidden;mso-hide:all;font-size:1px;line-height:1px;color:#F1F5F9;">${preheader}</div>
</#if>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:#F1F5F9;">
<tr><td align="center" style="padding:24px 12px;">
<table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" style="width:100%;max-width:600px;background-color:#ffffff;border:1px solid #E2E8F0;border-radius:12px;border-collapse:separate;overflow:hidden;font-family:'Source Sans 3','Segoe UI',Roboto,Arial,Helvetica,sans-serif;">
    <#-- Cabecera institucional en dos filas, como el encabezado del portal: franja de gobierno (Ministerio del Poder
         Popular para la Defensa y Armada Bolivariana) y, debajo, el nombre de PISAC. Solo texto (ver nota inicial). -->
    <tr>
        <td style="padding:16px 28px 12px 28px;background-color:#ffffff;">
            <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
                <td valign="middle" style="padding-right:12px;font-family:'Source Sans 3','Segoe UI',Roboto,Arial,Helvetica,sans-serif;color:#002B5C;">
                    <div style="font-size:10px;line-height:1.15;font-weight:600;">Ministerio del Poder Popular<br>para la</div>
                    <div style="font-family:Montserrat,'Segoe UI',Arial,sans-serif;font-size:17px;line-height:1.05;font-weight:800;letter-spacing:0.3px;text-transform:uppercase;">Defensa</div>
                </td>
                <td valign="middle" style="padding:0 12px;border-left:1px solid #CBD5E1;font-family:'Source Sans 3','Segoe UI',Roboto,Arial,Helvetica,sans-serif;color:#002B5C;">
                    <div style="font-size:10px;line-height:1.15;font-weight:600;">Armada</div>
                    <div style="font-family:Montserrat,'Segoe UI',Arial,sans-serif;font-size:17px;line-height:1.05;font-weight:800;letter-spacing:0.3px;text-transform:uppercase;">Bolivariana</div>
                </td>
            </tr></table>
        </td>
    </tr>
    <tr>
        <td style="padding:12px 28px 18px 28px;background-color:#ffffff;border-top:1px solid #E2E8F0;">
            <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
                <td valign="middle" style="padding-right:14px;">
                    <div style="width:3px;height:40px;background-color:#F47C20;border-radius:2px;"></div>
                </td>
                <td valign="middle" style="font-family:'Source Sans 3','Segoe UI',Roboto,Arial,Helvetica,sans-serif;">
                    <div style="font-family:Montserrat,'Segoe UI',Arial,sans-serif;font-size:18px;line-height:1.25;font-weight:700;color:#002B5C;">PISAC</div>
                    <div style="font-size:13px;line-height:1.3;font-weight:600;color:#002B5C;">Plataforma de Informaci&oacute;n de Seguridad Acu&aacute;tica</div>
                </td>
            </tr></table>
        </td>
    </tr>
    <tr><td style="height:6px;line-height:6px;font-size:0;background-color:#002B5C;">&nbsp;</td></tr>
    <tr><td style="height:3px;line-height:3px;font-size:0;background-color:#F47C20;">&nbsp;</td></tr>

    <#-- Contenido -->
    <tr>
        <td style="padding:32px 32px 28px 32px;font-family:'Source Sans 3','Segoe UI',Roboto,Arial,Helvetica,sans-serif;font-size:15px;line-height:1.6;color:#253746;">
            <#if title?has_content>
                <h1 style="margin:0 0 16px 0;font-family:Montserrat,'Segoe UI',Arial,sans-serif;font-size:20px;line-height:1.3;font-weight:700;color:#002B5C;">${title}</h1>
            </#if>
            <#nested>
        </td>
    </tr>

    <#-- Firma y avisos -->
    <tr>
        <td style="padding:24px 32px;background-color:#F8FAFC;border-top:1px solid #E2E8F0;font-family:'Source Sans 3','Segoe UI',Roboto,Arial,Helvetica,sans-serif;font-size:12px;line-height:1.5;color:#475569;">
            <p style="margin:0 0 12px 0;font-size:13px;line-height:1.45;color:#002B5C;">
                <strong>Atentamente,<br>PISAC - Plataforma de Informaci&oacute;n de Seguridad Acu&aacute;tica</strong><br>
                <span style="color:#475569;">Servicio de Hidrograf&iacute;a, Oceanograf&iacute;a, Meteorolog&iacute;a y Cartografiado N&aacute;utico (SHN - Venezuela)</span>
            </p>
            <p style="margin:0 0 8px 0;font-size:11px;color:#64748B;">La plataforma PISAC est&aacute; implementada &uacute;nicamente para las aguas jurisdiccionales de la Rep&uacute;blica Bolivariana de Venezuela.</p>
            <p style="margin:0 0 8px 0;font-size:11px;color:#64748B;">La disponibilidad de las advertencias de navegaci&oacute;n en esta plataforma no exime a los capitanes de la obligaci&oacute;n de recibir en el mar las advertencias de navegaci&oacute;n a trav&eacute;s de los sistemas de difusi&oacute;n oficiales, de conformidad con las disposiciones del Convenio SOLAS.</p>
            <p style="margin:0 0 12px 0;font-size:11px;color:#64748B;">La plataforma PISAC presenta una selecci&oacute;n de decretos mar&iacute;timos. Para mayor informaci&oacute;n consultar las publicaciones de las organismos competentes.</p>
            <p style="margin:0;padding-top:10px;border-top:1px dashed #CBD5E1;font-size:11px;color:#64748B;">
                No responda ni reenv&iacute;e correos a esta cuenta: es una cuenta no monitoreada.
                Para cualquier consulta, escriba a <a href="mailto:shnser@shn.gob.ve" style="color:#1F7FA6;font-weight:600;text-decoration:none;">shnser@shn.gob.ve</a>.
            </p>
        </td>
    </tr>
    <tr>
        <td align="center" style="padding:10px 24px;background-color:#001D3D;font-family:'Source Sans 3','Segoe UI',Roboto,Arial,Helvetica,sans-serif;font-size:11px;line-height:1.4;color:#DCE5F0;">
            PISAC &middot; SHN Venezuela &middot; Correo autom&aacute;tico
        </td>
    </tr>
</table>
</td></tr>
</table>
</body>
</html>
</#macro>

<#macro button href label>
<table role="presentation" cellpadding="0" cellspacing="0" border="0" align="center" style="margin:28px auto;">
    <tr>
        <td align="center" bgcolor="#002B5C" style="border-radius:6px;background-color:#002B5C;">
            <a href="${href}" target="_blank" rel="noopener noreferrer" style="display:inline-block;padding:13px 28px;font-family:'Source Sans 3','Segoe UI',Roboto,Arial,Helvetica,sans-serif;font-size:15px;font-weight:700;line-height:1.2;color:#ffffff;text-decoration:none;border-radius:6px;">${label}</a>
        </td>
    </tr>
</table>
</#macro>

<#macro linkFallback href>
<p style="margin:0 0 12px 0;font-size:12px;line-height:1.5;color:#64748B;word-break:break-all;">
    Si el bot&oacute;n no funciona, copie y pegue el siguiente enlace en su navegador:<br>
    <a href="${href}" target="_blank" rel="noopener noreferrer" style="color:#1F7FA6;">${href}</a>
</p>
</#macro>

<#macro note>
<p style="margin:0 0 12px 0;font-size:13px;line-height:1.5;color:#64748B;"><#nested></p>
</#macro>
