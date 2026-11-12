<#import "template.ftl" as layout>
<#assign linkWithLocale = link + (link?contains("?")?then("&kc_locale=es", "?kc_locale=es"))>
<@layout.emailLayout title="Verifique su correo electrónico" preheader="Active su cuenta en PISAC verificando su dirección de correo.">
    <p style="margin:0 0 12px 0;">Estimado usuario:</p>
    <p style="margin:0 0 12px 0;">Gracias por registrarse en la <strong>Plataforma de Informaci&oacute;n de Seguridad Acu&aacute;tica (PISAC)</strong>.
        Para activar su cuenta, confirme que esta direcci&oacute;n de correo le pertenece.</p>
    <@layout.button href=linkWithLocale label="Verificar correo electrónico"/>
    <@layout.note>Este enlace es de uso &uacute;nico y expira en ${linkExpirationFormatter(linkExpiration)}.</@layout.note>
    <@layout.linkFallback href=linkWithLocale/>
    <@layout.note>Si usted no cre&oacute; una cuenta en PISAC, ignore este mensaje: la cuenta no se activar&aacute;.</@layout.note>
</@layout.emailLayout>