package com.irerin.travelan.board.support;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

public final class HtmlSanitizer {

    private static final Safelist SAFELIST = Safelist.none()
        .addTags("p", "br",
                 "strong", "b", "em", "i", "u",
                 "ul", "ol", "li",
                 "blockquote",
                 "h1", "h2", "h3",
                 "a")
        .addAttributes("a", "href", "title")
        .addProtocols("a", "href", "http", "https", "mailto")
        .addEnforcedAttribute("a", "rel", "nofollow noopener noreferrer")
        .addEnforcedAttribute("a", "target", "_blank");

    private HtmlSanitizer() {
    }

    public static String sanitize(String html) {
        if (html == null) {
            return null;
        }
        return Jsoup.clean(html, SAFELIST);
    }
}
