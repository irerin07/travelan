package com.irerin.travelan.board.support;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

public final class HtmlSanitizer {

    private static final Safelist SAFELIST = Safelist.relaxed();

    private HtmlSanitizer() {
    }

    public static String sanitize(String html) {
        if (html == null) {
            return null;
        }
        return Jsoup.clean(html, SAFELIST);
    }
}
