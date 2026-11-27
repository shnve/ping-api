package fr.shom.ping;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexPageTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.ResourceTransformerChain;
import org.springframework.web.servlet.resource.TransformedResource;

/**
 * PISAC: inyecta una barra institucional con selector dinámico de idioma (ES | EN) en Swagger UI (/swagger-ui/index.html)
 * que conmuta sin recargar tanto la especificación OpenAPI (/v3/api-docs/datos vs /v3/api-docs/datos-en) como las etiquetas
 * y botones propios de Swagger UI.
 */
public class PisacSwaggerIndexTransformer extends SwaggerIndexPageTransformer {

    private static final String PISAC_STYLE = """
        <style id="pisac-swagger-i18n-style">
          .swagger-ui .topbar { display: none !important; }
          #pisac-swagger-bar {
            background: #0b3152;
            color: #ffffff;
            border-bottom: 3px solid #e07a1f;
            padding: 12px 24px;
            font-family: 'Segoe UI', system-ui, -apple-system, Arial, sans-serif;
            display: flex;
            align-items: center;
            justify-content: space-between;
            flex-wrap: wrap;
            gap: 12px;
            box-shadow: 0 2px 6px rgba(0, 0, 0, 0.14);
          }
          .pisac-sw-brand {
            display: flex;
            flex-direction: column;
            gap: 2px;
          }
          .pisac-sw-title {
            font-size: 16px;
            font-weight: 700;
            letter-spacing: 0.4px;
          }
          .pisac-sw-subtitle {
            font-size: 12px;
            color: #cbd5e1;
          }
          .pisac-sw-actions {
            display: flex;
            align-items: center;
            gap: 14px;
          }
          .pisac-sw-back {
            color: #e2e8f0;
            text-decoration: none;
            font-size: 13px;
            font-weight: 600;
            padding: 6px 10px;
            border-radius: 6px;
            border: 1px solid rgba(255, 255, 255, 0.22);
            transition: background-color 0.15s ease;
          }
          .pisac-sw-back:hover {
            background: rgba(255, 255, 255, 0.12);
            color: #ffffff;
          }
          .pisac-sw-lang-group {
            display: inline-flex;
            align-items: center;
            background: rgba(255, 255, 255, 0.1);
            border: 1px solid rgba(255, 255, 255, 0.25);
            border-radius: 999px;
            padding: 2px;
          }
          .pisac-sw-lang-btn {
            appearance: none;
            border: none;
            background: transparent;
            color: #e2e8f0;
            font-size: 12px;
            font-weight: 700;
            padding: 5px 12px;
            border-radius: 999px;
            cursor: pointer;
            transition: background-color 0.15s ease, color 0.15s ease;
          }
          .pisac-sw-lang-btn[aria-pressed="true"] {
            background: #ffffff;
            color: #0b3152;
          }
          .pisac-sw-lang-btn:focus-visible {
            outline: 2px solid #f59e0b;
            outline-offset: 2px;
          }
        </style>
        """;

    private static final String PISAC_BAR_AND_SCRIPT = """
        <div id="pisac-swagger-bar" role="banner">
          <div class="pisac-sw-brand">
            <span class="pisac-sw-title" id="pisac-sw-title">PISAC — Documentación de servicios de datos</span>
            <span class="pisac-sw-subtitle" id="pisac-sw-subtitle">Servicio de Hidrografía, Oceanografía, Meteorología y Cartografiado Náutico (SHN - Venezuela)</span>
          </div>
          <div class="pisac-sw-actions">
            <a class="pisac-sw-back" id="pisac-sw-back" href="/static/data-services">← Volver a Servicios de datos</a>
            <div class="pisac-sw-lang-group" role="group" aria-label="Selector de idioma / Language selector">
              <button type="button" class="pisac-sw-lang-btn" id="pisac-lang-es" data-lang="es" aria-pressed="true">ES · Español</button>
              <button type="button" class="pisac-sw-lang-btn" id="pisac-lang-en" data-lang="en" aria-pressed="false">EN · English</button>
            </div>
          </div>
        </div>
        <script id="pisac-swagger-i18n-script">
        (function () {
          var SPEC_URLS = {
            es: '/v3/api-docs/datos',
            en: '/v3/api-docs/datos-en'
          };
          var HEADER_TEXT = {
            es: {
              pageTitle: 'PISAC — Referencia de la API (S-124 / SECOM)',
              title: 'PISAC — Documentación de servicios de datos',
              subtitle: 'Servicio de Hidrografía, Oceanografía, Meteorología y Cartografiado Náutico (SHN - Venezuela)',
              back: '← Volver a Servicios de datos'
            },
            en: {
              pageTitle: 'PISAC — API Reference (S-124 / SECOM)',
              title: 'PISAC — Data Services API Documentation',
              subtitle: 'Hydrographic, Oceanographic, Meteorological and Nautical Charting Service (SHN - Venezuela)',
              back: '← Back to Data Services'
            }
          };
          var UI_MAP_ES = {
            'Parameters': 'Parámetros',
            'No parameters': 'Sin parámetros',
            'Try it out': 'Probar',
            'Cancel': 'Cancelar',
            'Execute': 'Ejecutar',
            'Clear': 'Limpiar',
            'Responses': 'Respuestas',
            'Server response': 'Respuesta del servidor',
            'Code': 'Código',
            'Description': 'Descripción',
            'Links': 'Enlaces',
            'No links': 'Sin enlaces',
            'Headers:': 'Cabeceras:',
            'Response body': 'Cuerpo de la respuesta',
            'Response headers': 'Cabeceras de la respuesta',
            'Request URL': 'URL de la solicitud',
            'Curl': 'Comando cURL',
            'Download': 'Descargar',
            'Schemas': 'Esquemas',
            'Schema': 'Esquema',
            'Model': 'Modelo',
            'Example Value': 'Valor de ejemplo',
            'Example': 'Ejemplo',
            'Examples': 'Ejemplos',
            'Media type': 'Tipo de contenido',
            'Controls Accept header.': 'Controla la cabecera Accept.',
            'Name': 'Nombre',
            'Value': 'Valor',
            'Loading...': 'Cargando...',
            'Send empty value': 'Enviar valor vacío',
            'Add string item': 'Añadir elemento',
            'Add item': 'Añadir elemento',
            'required': 'obligatorio',
            '* required': '* obligatorio',
            '(query)': '(consulta)',
            '(path)': '(ruta)',
            '(header)': '(cabecera)',
            'Available values': 'Valores permitidos',
            'Default value': 'Valor por defecto',
            'Authorize': 'Autorizar',
            'Close': 'Cerrar'
          };
          var ATTR_MAP_ES = {
            'Copy to clipboard': 'Copiar al portapapeles',
            'Expand operation': 'Expandir operación',
            'Collapse operation': 'Contraer operación'
          };
          var SKIP_TAGS = { CODE: 1, PRE: 1, SCRIPT: 1, STYLE: 1, TEXTAREA: 1, SVG: 1 };

          function detectInitialLang() {
            try {
              var params = new URLSearchParams(window.location.search);
              var q = (params.get('lang') || '').toLowerCase();
              if (q === 'es' || q === 'en') return q;
              var saved = (localStorage.getItem('pisac_swagger_lang') || localStorage.getItem('lang') || '').toLowerCase();
              if (saved === 'en' || saved === 'es') return saved;
            } catch (e) {}
            return 'es';
          }

          var currentLang = detectInitialLang();
          var currentSpecApplied = null;
          var isMutating = false;

          function shouldSkipNode(el) {
            while (el && el.nodeType === 1) {
              if (SKIP_TAGS[el.tagName]) return true;
              var cls = el.className;
              if (typeof cls === 'string' && (
                cls.indexOf('microlight') !== -1 ||
                cls.indexOf('highlight-code') !== -1 ||
                cls.indexOf('curl-command') !== -1 ||
                cls.indexOf('parameter__name') !== -1 ||
                cls.indexOf('opblock-summary-path') !== -1 ||
                cls.indexOf('response-col_status') !== -1
              )) {
                if (cls.indexOf('parameter__name') !== -1) {
                  return false;
                }
                return true;
              }
              if (el.id === 'pisac-swagger-bar') return true;
              el = el.parentElement;
            }
            return false;
          }

          function translateTextValue(raw, lang) {
            var trimmed = raw.trim();
            if (!trimmed) return null;
            if (lang === 'es') {
              if (Object.prototype.hasOwnProperty.call(UI_MAP_ES, trimmed)) {
                return raw.replace(trimmed, UI_MAP_ES[trimmed]);
              }
              if (trimmed.indexOf('Available values') === 0) {
                return raw.replace('Available values', 'Valores permitidos');
              }
              if (trimmed.indexOf('Default value') === 0) {
                return raw.replace('Default value', 'Valor por defecto');
              }
              if (trimmed.indexOf('Controls ') === 0 && trimmed.indexOf(' header.') !== -1) {
                return raw.replace('Controls ', 'Controla la cabecera ').replace(' header.', '.');
              }
            }
            return null;
          }

          function applyUiTranslations() {
            var root = document.getElementById('swagger-ui');
            if (!root || isMutating) return;
            isMutating = true;
            try {
              var walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, null);
              var node;
              while ((node = walker.nextNode())) {
                var parent = node.parentElement;
                if (!parent || shouldSkipNode(parent)) continue;
                if (parent.classList && parent.classList.contains('parameter__name') && node.nodeValue && node.nodeValue.trim() !== '* required' && node.nodeValue.trim() !== 'required') {
                  continue;
                }
                if (node.__pisacOrig === undefined) {
                  var candidate = translateTextValue(node.nodeValue, 'es');
                  if (candidate !== null) {
                    node.__pisacOrig = node.nodeValue;
                  }
                }
                if (node.__pisacOrig !== undefined) {
                  if (currentLang === 'en') {
                    if (node.nodeValue !== node.__pisacOrig) {
                      node.nodeValue = node.__pisacOrig;
                    }
                  } else {
                    var translated = translateTextValue(node.__pisacOrig, 'es');
                    if (translated !== null && node.nodeValue !== translated) {
                      node.nodeValue = translated;
                    }
                  }
                }
              }

              var elements = root.querySelectorAll('[title], [aria-label], [placeholder]');
              for (var i = 0; i < elements.length; i++) {
                var el = elements[i];
                ['title', 'aria-label', 'placeholder'].forEach(function (attr) {
                  var val = el.getAttribute(attr);
                  if (!val) return;
                  var storeKey = 'pisacOrig_' + attr;
                  if (!el.dataset[storeKey] && Object.prototype.hasOwnProperty.call(ATTR_MAP_ES, val)) {
                    el.dataset[storeKey] = val;
                  }
                  var orig = el.dataset[storeKey];
                  if (orig) {
                    var nextVal = currentLang === 'en' ? orig : (ATTR_MAP_ES[orig] || orig);
                    if (val !== nextVal) {
                      el.setAttribute(attr, nextVal);
                    }
                  }
                });
              }
            } finally {
              isMutating = false;
            }
          }

          function updateHeaderBar(lang) {
            var copy = HEADER_TEXT[lang] || HEADER_TEXT.es;
            document.documentElement.lang = lang;
            document.title = copy.pageTitle;
            var titleEl = document.getElementById('pisac-sw-title');
            var subEl = document.getElementById('pisac-sw-subtitle');
            var backEl = document.getElementById('pisac-sw-back');
            var btnEs = document.getElementById('pisac-lang-es');
            var btnEn = document.getElementById('pisac-lang-en');
            if (titleEl) titleEl.textContent = copy.title;
            if (subEl) subEl.textContent = copy.subtitle;
            if (backEl) backEl.textContent = copy.back;
            if (btnEs) btnEs.setAttribute('aria-pressed', lang === 'es' ? 'true' : 'false');
            if (btnEn) btnEn.setAttribute('aria-pressed', lang === 'en' ? 'true' : 'false');
          }

          function switchOpenApiSpec(lang, forceDownload) {
            var targetUrl = SPEC_URLS[lang] || SPEC_URLS.es;
            if (!window.ui || !window.ui.specActions) return false;
            var params = new URLSearchParams(window.location.search);
            if (!forceDownload && params.has('urls.primaryName')) {
              return true;
            }
            if (forceDownload || currentSpecApplied !== targetUrl) {
              currentSpecApplied = targetUrl;
              window.ui.specActions.updateUrl(targetUrl);
              window.ui.specActions.download(targetUrl);
            }
            return true;
          }

          function setLanguage(lang, userInitiated) {
            if (lang !== 'es' && lang !== 'en') return;
            currentLang = lang;
            try {
              localStorage.setItem('pisac_swagger_lang', lang);
              var url = new URL(window.location.href);
              url.searchParams.set('lang', lang);
              window.history.replaceState({}, '', url.toString());
            } catch (e) {}
            updateHeaderBar(lang);
            switchOpenApiSpec(lang, !!userInitiated);
            applyUiTranslations();
          }

          document.getElementById('pisac-lang-es').addEventListener('click', function () { setLanguage('es', true); });
          document.getElementById('pisac-lang-en').addEventListener('click', function () { setLanguage('en', true); });

          updateHeaderBar(currentLang);

          var attempts = 0;
          var bootTimer = setInterval(function () {
            attempts++;
            var root = document.getElementById('swagger-ui');
            if (root && !root.__pisacObserved) {
              root.__pisacObserved = true;
              var observer = new MutationObserver(function () {
                if (!isMutating) {
                  applyUiTranslations();
                }
              });
              observer.observe(root, { childList: true, subtree: true });
            }
            if (switchOpenApiSpec(currentLang, false) || attempts > 60) {
              applyUiTranslations();
              clearInterval(bootTimer);
            }
          }, 100);
        })();
        </script>
        """;

    public PisacSwaggerIndexTransformer(
        SwaggerUiConfigProperties swaggerUiConfig,
        SwaggerUiOAuthProperties swaggerUiOAuthProperties,
        SwaggerWelcomeCommon swaggerWelcomeCommon,
        ObjectMapperProvider objectMapperProvider
    ) {
        super(swaggerUiConfig, swaggerUiOAuthProperties, swaggerWelcomeCommon, objectMapperProvider);
    }

    @Override
    public Resource transform(HttpServletRequest request, Resource resource, ResourceTransformerChain transformerChain) throws IOException {
        Resource transformed = super.transform(request, resource, transformerChain);
        String filename = resource.getFilename();
        if (!"index.html".equals(filename)) {
            return transformed;
        }
        try (InputStream is = transformed.getInputStream()) {
            String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            String enriched = injectLanguageSelector(html);
            return new TransformedResource(transformed, enriched.getBytes(StandardCharsets.UTF_8));
        }
    }

    static String injectLanguageSelector(String html) {
        if (html.contains("pisac-swagger-bar")) {
            return html;
        }
        String withStyle = html.contains("</head>")
            ? html.replace("</head>", PISAC_STYLE + "\n</head>")
            : PISAC_STYLE + html;
        if (withStyle.contains("<body>")) {
            return withStyle.replace("<body>", "<body>\n" + PISAC_BAR_AND_SCRIPT);
        }
        return withStyle + PISAC_BAR_AND_SCRIPT;
    }
}
