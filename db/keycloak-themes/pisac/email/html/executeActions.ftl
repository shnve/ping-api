<#import "template.ftl" as layout>
<#assign linkWithLocale = link + (link?contains("?")?then("&kc_locale=es", "?kc_locale=es"))>
<@layout.emailLayout title="Configure su acceso a PISAC" preheader="Defina su contraseña para activar su cuenta en PISAC.">
    <p style="margin:0 0 12px 0;">Estimado usuario:</p>
    <p style="margin:0 0 12px 0;">Se ha creado o actualizado su cuenta en la <strong>Plataforma de Informaci&oacute;n de Seguridad Acu&aacute;tica (PISAC)</strong>.
        Para activarla, defina su contrase&ntilde;a de acceso.</p>
    <@layout.button href=linkWithLocale label="Configurar contraseña"/>
    <@layout.note>Este enlace es de uso &uacute;nico y expira en ${linkExpirationFormatter(linkExpiration)}.</@layout.note>
    <@layout.linkFallback href=linkWithLocale/>
    <@layout.note>Si no esperaba este mensaje, no use el enlace y comun&iacute;quese con el SHN.</@layout.note>
</@layout.emailLayout>