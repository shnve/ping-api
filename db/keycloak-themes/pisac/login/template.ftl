<#--
  PISAC (F5.3.6): plantilla común de las páginas de inicio de sesión de Keycloak para los realms
  ping-public y ping-pro. Basada en theme/base/login/template.ftl de Keycloak 26.6.4.

  Cambios respecto a la original:
  - Cabecera institucional (logos SHN / OCHINA, República y nombre de PISAC) igual que la del portal, con el
    selector de idioma y el enlace «Volver al portal» a la derecha.
  - Panel lateral con el tipo de acceso según el realm (público o profesional).
  - Pie institucional.
  - El resto (scripts, mensajes, bloques anidados header / form / info / socialProviders, «probar otra forma»)
    se conserva sin cambios para que sigan funcionando todas las páginas del tema padre.

  Al actualizar Keycloak, comparar con la template.ftl del nuevo tema base y trasladar los cambios.
-->
<#import "footer.ftl" as loginFooter>
<#macro registrationLayout bodyClass="" displayInfo=false displayMessage=true displayRequiredFields=false>
<#assign isPro = (realm.name!'') == "ping-pro">
<#assign portalUrl = (client.baseUrl)!"">
<#if !portalUrl?has_content><#assign portalUrl = properties.pisacPortalUrl!""></#if>
<#if portalUrl?has_content && !portalUrl?ends_with("/")><#assign portalUrl = portalUrl + "/"></#if>
<!DOCTYPE html>
<html class="${properties.kcHtmlClass!}" lang="${lang}"<#if realm.internationalizationEnabled> dir="${(locale.rtl)?then('rtl','ltr')}"</#if>>

<head>
    <meta charset="utf-8">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />

    <#if properties.meta?has_content>
        <#list properties.meta?split(' ') as meta>
            <meta name="${meta?split('==')[0]}" content="${meta?split('==')[1]}"/>
        </#list>
    </#if>
    <title>${title!} · PISAC</title>
    <link rel="icon" href="${url.resourcesPath}/img/favicon.ico" />
    <#if properties.stylesCommon?has_content>
        <#list properties.stylesCommon?split(' ') as style>
            <link href="${url.resourcesCommonPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
    <#if properties.styles?has_content>
        <#list properties.styles?split(' ') as style>
            <link href="${url.resourcesPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
    <#if properties.scripts?has_content>
        <#list properties.scripts?split(' ') as script>
            <script src="${url.resourcesPath}/${script}" type="text/javascript"></script>
        </#list>
    </#if>
    <script type="importmap">
        {
            "imports": {
                "rfc4648": "${url.resourcesCommonPath}/vendor/rfc4648/rfc4648.js"
            }
        }
    </script>
    <script src="${url.resourcesPath}/js/menu-button-links.js" type="module"></script>
    <#if scripts??>
        <#list scripts as script>
            <script src="${script}" type="text/javascript"></script>
        </#list>
    </#if>
    <script type="module">
        <#outputformat "JavaScript">
        import { startSessionPolling } from ${(url.resourcesPath + "/js/authChecker.js")?c};

        startSessionPolling(
            ${url.ssoLoginInOtherTabsUrl?c}
        );
        </#outputformat>
    </script>
    <script type="module">
        document.addEventListener("click", (event) => {
            const link = event.target.closest("a[data-once-link]");

            if (!link) {
                return;
            }

            if (link.getAttribute("aria-disabled") === "true") {
                event.preventDefault();
                return;
            }

            const { disabledClass } = link.dataset;

            if (disabledClass) {
                link.classList.add(...disabledClass.trim().split(/\s+/));
            }

            link.setAttribute("role", "link");
            link.setAttribute("aria-disabled", "true");
        });
    </script>
    <#if authenticationSession??>
        <script type="module">
            <#outputformat "JavaScript">
            import { checkAuthSession } from ${(url.resourcesPath + "/js/authChecker.js")?c};

            checkAuthSession(
                ${authenticationSession.authSessionIdHash?c}
            );
            </#outputformat>
        </script>
    </#if>
</head>

<body class="${properties.kcBodyClass!} pisac-body ${isPro?then('pisac-realm-pro','pisac-realm-public')}" data-page-id="login-${pageId}">
<div class="pisac-shell">
    <#-- Cabecera institucional -->
    <header class="pisac-topbar">
        <div class="pisac-topbar__brand">
            <img class="pisac-topbar__logo" src="${url.resourcesPath}/img/logo-shn-ochina.png" alt="SHN · OCHINA" width="96" height="55" />
            <span class="pisac-topbar__divider" aria-hidden="true"></span>
            <div class="pisac-topbar__titles">
                <span class="pisac-topbar__republic">${msg("pisacRepublic")}</span>
                <span class="pisac-topbar__platform">${msg("pisacPlatform")}</span>
            </div>
        </div>
        <div class="pisac-topbar__actions">
            <#if realm.internationalizationEnabled  && locale.supported?size gt 1>
                <div class="${properties.kcLocaleMainClass!}" id="kc-locale">
                    <div id="kc-locale-wrapper" class="${properties.kcLocaleWrapperClass!}">
                        <div id="kc-locale-dropdown" class="menu-button-links ${properties.kcLocaleDropDownClass!}">
                            <button tabindex="1" id="kc-current-locale-link" aria-label="${msg("languages")}" aria-haspopup="true" aria-expanded="false" aria-controls="language-switch1">${locale.current}</button>
                            <ul role="menu" tabindex="-1" aria-labelledby="kc-current-locale-link" aria-activedescendant="" id="language-switch1" class="${properties.kcLocaleListClass!}">
                                <#assign i = 1>
                                <#list locale.supported as l>
                                    <li class="${properties.kcLocaleListItemClass!}" role="none">
                                        <a role="menuitem" id="language-${i}" class="${properties.kcLocaleItemClass!}" href="${l.url}">${l.label}</a>
                                    </li>
                                    <#assign i++>
                                </#list>
                            </ul>
                        </div>
                    </div>
                </div>
            </#if>
            <#if portalUrl?has_content>
                <a class="pisac-topbar__back" href="${portalUrl}">
                    <span aria-hidden="true">&larr;</span> ${msg("pisacBackToPortal")}
                </a>
            </#if>
        </div>
    </header>
    <div class="pisac-navband" aria-hidden="true"></div>

    <main class="pisac-main">
        <div class="pisac-layout">
            <#-- Panel lateral: tipo de acceso según el realm -->
            <aside class="pisac-aside" aria-label="${msg(isPro?then('pisacProBadge','pisacPublicBadge'))}">
                <span class="pisac-aside__badge">
                    <span class="pisac-aside__badge-dot" aria-hidden="true"></span>${msg(isPro?then('pisacProBadge','pisacPublicBadge'))}
                </span>
                <h2 class="pisac-aside__title">${msg(isPro?then('pisacProHeading','pisacPublicHeading'))}</h2>
                <p class="pisac-aside__text">${msg(isPro?then('pisacProText','pisacPublicText'))}</p>
                <ul class="pisac-aside__list">
                    <#list 1..3 as n>
                        <li>${msg(isPro?then('pisacProItem','pisacPublicItem') + n)}</li>
                    </#list>
                </ul>
                <#if !isPro && portalUrl?has_content>
                    <p class="pisac-aside__register">
                        ${msg("pisacNoAccount")}
                        <a href="${portalUrl}account-creation">${msg("pisacRegister")}</a>
                    </p>
                </#if>
            </aside>

            <div class="${properties.kcLoginClass!} pisac-login">
                <div class="${properties.kcFormCardClass!}">
                    <header class="${properties.kcFormHeaderClass!}">
                    <#if !(auth?has_content && auth.showUsername() && !auth.showResetCredentials())>
                        <#if displayRequiredFields>
                            <div class="${properties.kcContentWrapperClass!}">
                                <div class="${properties.kcLabelWrapperClass!} subtitle">
                                    <span class="subtitle"><span class="required">*</span> ${msg("requiredFields")}</span>
                                </div>
                                <div class="col-md-10">
                                    <h1 id="kc-page-title"><#nested "header"></h1>
                                </div>
                            </div>
                        <#else>
                            <h1 id="kc-page-title"><#nested "header"></h1>
                        </#if>
                    <#else>
                        <#if displayRequiredFields>
                            <div class="${properties.kcContentWrapperClass!}">
                                <div class="${properties.kcLabelWrapperClass!} subtitle">
                                    <span class="subtitle"><span class="required">*</span> ${msg("requiredFields")}</span>
                                </div>
                                <div class="col-md-10">
                                    <#nested "show-username">
                                    <div id="kc-username" class="${properties.kcFormGroupClass!}">
                                        <label id="kc-attempted-username">${auth.attemptedUsername}</label>
                                        <a id="reset-login" href="${url.loginRestartFlowUrl}" aria-label="${msg("restartLoginTooltip")}">
                                            <div class="kc-login-tooltip">
                                                <i class="${properties.kcResetFlowIcon!}"></i>
                                                <span class="kc-tooltip-text">${msg("restartLoginTooltip")}</span>
                                            </div>
                                        </a>
                                    </div>
                                </div>
                            </div>
                        <#else>
                            <#nested "show-username">
                            <div id="kc-username" class="${properties.kcFormGroupClass!}">
                                <label id="kc-attempted-username">${auth.attemptedUsername}</label>
                                <a id="reset-login" href="${url.loginRestartFlowUrl}" aria-label="${msg("restartLoginTooltip")}">
                                    <div class="kc-login-tooltip">
                                        <i class="${properties.kcResetFlowIcon!}"></i>
                                        <span class="kc-tooltip-text">${msg("restartLoginTooltip")}</span>
                                    </div>
                                </a>
                            </div>
                        </#if>
                    </#if>
                    </header>
                    <div id="kc-content">
                        <div id="kc-content-wrapper">

                        <#-- App-initiated actions should not see warning messages about the need to complete the action -->
                        <#-- during login.                                                                               -->
                        <#if displayMessage && message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
                            <div class="alert-${message.type} ${properties.kcAlertClass!} pf-m-<#if message.type = 'error'>danger<#else>${message.type}</#if>">
                                <div class="pf-c-alert__icon">
                                    <#if message.type = 'success'><span class="${properties.kcFeedbackSuccessIcon!}"></span></#if>
                                    <#if message.type = 'warning'><span class="${properties.kcFeedbackWarningIcon!}"></span></#if>
                                    <#if message.type = 'error'><span class="${properties.kcFeedbackErrorIcon!}"></span></#if>
                                    <#if message.type = 'info'><span class="${properties.kcFeedbackInfoIcon!}"></span></#if>
                                </div>
                                    <span class="${properties.kcAlertTitleClass!}">${kcSanitize(message.summary)?no_esc}</span>
                            </div>
                        </#if>

                        <#nested "form">

                        <#if auth?has_content && auth.showTryAnotherWayLink()>
                            <form id="kc-select-try-another-way-form" action="${url.loginAction}" method="post">
                                <div class="${properties.kcFormGroupClass!}">
                                    <input type="hidden" name="tryAnotherWay" value="on"/>
                                    <a href="#" id="try-another-way"
                                       onclick="document.forms['kc-select-try-another-way-form'].requestSubmit();return false;">${msg("doTryAnotherWay")}</a>
                                </div>
                            </form>
                        </#if>

                        <#nested "socialProviders">

                        <#if displayInfo>
                            <div id="kc-info" class="${properties.kcSignUpClass!}">
                                <div id="kc-info-wrapper" class="${properties.kcInfoAreaWrapperClass!}">
                                    <#nested "info">
                                </div>
                            </div>
                        </#if>
                        </div>
                    </div>

                    <@loginFooter.content/>
                </div>
            </div>
        </div>
    </main>

    <#-- Pie institucional -->
    <footer class="pisac-footer">
        <span>${msg("pisacPlatform")}</span>
        <span class="pisac-footer__sep" aria-hidden="true">·</span>
        <span>${msg("pisacFooterOrg")}</span>
    </footer>
</div>
</body>
</html>
</#macro>
