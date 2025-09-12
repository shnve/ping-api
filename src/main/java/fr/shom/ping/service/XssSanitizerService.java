package fr.shom.ping.service;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Service;

@Service
public class XssSanitizerService {
    public String sanitize(String value) {
        final PolicyFactory policyFactory = new HtmlPolicyBuilder()
            .allowStandardUrlProtocols()
            .allowStyling()
            .allowCommonBlockElements()
            .allowCommonInlineFormattingElements()
            .allowElements("a")
            .allowAttributes("href", "target")
            .onElements("a")
            .requireRelsOnLinks("noopener", "noreferrer")
            .requireRelNofollowOnLinks()
            .allowElements("img")
            .allowUrlProtocols("data") // Specifically allow "data"
            .allowAttributes("src")
            .onElements("img")
            .allowElements("details", "summary")
            .disallowElements("script")
            .toFactory();
        return policyFactory.sanitize(value);
    }
}
