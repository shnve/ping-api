<#macro emailLayout>
<!DOCTYPE html>
<html lang="${locale.language}" dir="${(ltr)?then('ltr','rtl')}">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>
<body style="font-family: 'Segoe UI', Arial, Helvetica, sans-serif; color: #212529; line-height: 1.6; margin: 0; padding: 24px; background-color: #f1f5f9;">
    <table width="100%" cellpadding="0" cellspacing="0" border="0" style="max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.06); border: 1px solid #e2e8f0;">
        <!-- Encabezado Institucional -->
        <tr>
            <td style="background-color: #0b3152; padding: 22px 28px; text-align: center; border-bottom: 3px solid #0056b3;">
                <div style="color: #ffffff; font-size: 20px; font-weight: 700; letter-spacing: 1px; line-height: 1.2;">PISAC</div>
                <div style="color: #cbd5e1; font-size: 12px; margin-top: 4px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; line-height: 1.3;">Plataforma de Informaci&oacute;n de Seguridad Acu&aacute;tica</div>
            </td>
        </tr>
        <!-- Contenido Principal -->
        <tr>
            <td style="padding: 32px 28px 24px 28px; font-size: 14px; color: #334155;">
                <#nested>
            </td>
        </tr>
        <!-- Firma Institucional (Cuenta) -->
        <tr>
            <td style="padding: 24px 28px; background-color: #f8fafc; border-top: 2px solid #e2e8f0; font-size: 12px; color: #475569; line-height: 1.5;">
                <p style="margin: 0 0 12px 0; font-size: 13px; font-weight: 700; color: #0b3152;">
                    Atentamente,<br/>
                    PISAC - Plataforma de Informaci&oacute;n de Seguridad Acu&aacute;tica<br/>
                    <span style="font-size: 12px; font-weight: 400; color: #334155;">Servicio de Hidrograf&iacute;a, Oceanograf&iacute;a, Meteorolog&iacute;a y Cartografiado N&aacute;utico (SHN - Venezuela)</span>
                </p>
                <p style="margin: 0 0 10px 0; font-size: 11px; color: #64748b; line-height: 1.45;">
                    La plataforma PISAC est&aacute; implementada &uacute;nicamente para las aguas jurisdiccionales de la Rep&uacute;blica Bolivariana de Venezuela.
                </p>
                <p style="margin: 0 0 10px 0; font-size: 11px; color: #64748b; line-height: 1.45;">
                    La disponibilidad de las advertencias de navegaci&oacute;n en esta plataforma no exime a los capitanes de la obligaci&oacute;n de recibir en el mar las advertencias de navegaci&oacute;n a trav&eacute;s de los sistemas de difusi&oacute;n oficiales, de conformidad con las disposiciones del Convenio SOLAS.
                </p>
                <p style="margin: 0 0 14px 0; font-size: 11px; color: #64748b; line-height: 1.45;">
                    La plataforma PISAC presenta una selecci&oacute;n de decretos mar&iacute;timos. Para mayor informaci&oacute;n consultar las publicaciones de las autoridades competentes.
                </p>
                <div style="margin-top: 12px; padding-top: 10px; border-top: 1px dashed #cbd5e1; font-size: 11px; color: #94a3b8; line-height: 1.4;">
                    No responda o reenv&iacute;e correos a esta cuenta. Esta es una cuenta no monitoreada.<br/>
                    Para cualquier consulta adicional, por favor enviar un correo electr&oacute;nico a <a href="mailto:shnser@shn.gob.ve" style="color: #0066cc; text-decoration: none; font-weight: 600;">shnser@shn.gob.ve</a>
                </div>
            </td>
        </tr>
    </table>
</body>
</html>
</#macro>
