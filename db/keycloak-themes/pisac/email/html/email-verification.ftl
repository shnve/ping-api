<#import "template.ftl" as layout>
<@layout.emailLayout>
    <#assign linkWithLocale = link + (link?contains("?")?then("&kc_locale=es", "?kc_locale=es"))>
    <p style="font-size: 15px; font-weight: 600; color: #0b3152; margin-top: 0;">
        Estimado usuario,
    </p>
    <p style="font-size: 14px; color: #334155; line-height: 1.6;">
        Gracias por registrarse en la <strong>Plataforma de Informaci&oacute;n de Seguridad Acu&aacute;tica (PISAC)</strong>.
    </p>
    <p style="font-size: 14px; color: #334155; line-height: 1.6;">
        Para verificar su direcci&oacute;n de correo electr&oacute;nico y activar su cuenta, por favor haga clic en el siguiente bot&oacute;n:
    </p>
    <div style="text-align: center; margin: 30px 0;">
        <a href="${linkWithLocale}" style="background-color: #0b3152; color: #ffffff; padding: 12px 28px; text-decoration: none; border-radius: 6px; font-weight: 600; font-size: 14px; display: inline-block; letter-spacing: 0.5px;">
            Verificar Correo Electr&oacute;nico
        </a>
    </div>
    <p style="font-size: 12px; color: #64748b; line-height: 1.5;">
        Este enlace es de uso &uacute;nico y expira en ${linkExpirationFormatter(linkExpiration)}.
    </p>
    <p style="font-size: 12px; color: #64748b; line-height: 1.5; word-break: break-all;">
        Si el bot&oacute;n no funciona, copie y pegue el siguiente enlace en su navegador:<br/>
        <a href="${linkWithLocale}" style="color: #0066cc;">${linkWithLocale}</a>
    </p>
</@layout.emailLayout>
