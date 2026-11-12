<#import "template.ftl" as layout>
<#assign linkWithLocale = link + (link?contains("?")?then("&kc_locale=es", "?kc_locale=es"))>
<@layout.emailLayout title="Restablecer contraseña" preheader="Solicitud para restablecer su contraseña de PISAC.">
    <p style="margin:0 0 12px 0;">Estimado usuario:</p>
    <p style="margin:0 0 12px 0;">Hemos recibido una solicitud para restablecer la contrase&ntilde;a de su cuenta en la
        <strong>Plataforma de Informaci&oacute;n de Seguridad Acu&aacute;tica (PISAC)</strong>.</p>
    <@layout.button href=linkWithLocale label="Restablecer contraseña"/>
    <@layout.note>Este enlace es de uso &uacute;nico y expira en ${linkExpirationFormatter(linkExpiration)}.</@layout.note>
    <@layout.linkFallback href=linkWithLocale/>
    <@layout.note>Si usted no solicit&oacute; este cambio, ignore este mensaje: su contrase&ntilde;a actual sigue siendo v&aacute;lida.</@layout.note>
</@layout.emailLayout>