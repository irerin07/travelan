package com.irerin.travelan.board.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HtmlSanitizerTest {

    @Test
    void removesScriptTag() {
        String dirty = "<p>hello</p><script>alert('x')</script>";
        assertThat(HtmlSanitizer.sanitize(dirty)).doesNotContain("<script>");
    }

    @Test
    void removesOnclickAttribute() {
        String dirty = "<a href=\"https://example.com\" onclick=\"hack()\">link</a>";
        String clean = HtmlSanitizer.sanitize(dirty);
        assertThat(clean).doesNotContain("onclick");
        assertThat(clean).contains("href=\"https://example.com\"");
    }

    @Test
    void removesIframe() {
        String dirty = "<iframe src=\"http://evil\"></iframe><p>ok</p>";
        assertThat(HtmlSanitizer.sanitize(dirty)).doesNotContain("<iframe");
    }

    @Test
    void preservesInlineFormatting() {
        String html = "<p><strong>bold</strong> <em>italic</em></p>";
        assertThat(HtmlSanitizer.sanitize(html)).contains("<strong>").contains("<em>");
    }

    @Test
    void returnsNullForNull() {
        assertThat(HtmlSanitizer.sanitize(null)).isNull();
    }

    @Test
    void removesInlineImageTag() {
        String dirty = "<p>before</p><img src=\"https://attacker.example/track.gif\"><p>after</p>";
        String clean = HtmlSanitizer.sanitize(dirty);
        assertThat(clean).doesNotContain("<img");
        assertThat(clean).contains("before").contains("after");
    }

    @Test
    void removesTableTag() {
        String dirty = "<table><tr><td>a</td></tr></table><p>ok</p>";
        String clean = HtmlSanitizer.sanitize(dirty);
        assertThat(clean).doesNotContain("<table").doesNotContain("<tr").doesNotContain("<td");
        assertThat(clean).contains("ok");
    }

    @Test
    void enforcesRelAndTargetOnAnchor() {
        String dirty = "<a href=\"https://example.com\">link</a>";
        String clean = HtmlSanitizer.sanitize(dirty);
        assertThat(clean).contains("rel=\"nofollow noopener noreferrer\"");
        assertThat(clean).contains("target=\"_blank\"");
    }

    @Test
    void overridesUserSuppliedRelAttribute() {
        String dirty = "<a href=\"https://example.com\" rel=\"dofollow\" target=\"_self\">link</a>";
        String clean = HtmlSanitizer.sanitize(dirty);
        assertThat(clean).contains("rel=\"nofollow noopener noreferrer\"");
        assertThat(clean).contains("target=\"_blank\"");
        assertThat(clean).doesNotContain("dofollow");
        assertThat(clean).doesNotContain("_self");
    }

    @Test
    void blocksJavascriptUriOnAnchor() {
        String dirty = "<a href=\"javascript:alert(1)\">x</a>";
        String clean = HtmlSanitizer.sanitize(dirty);
        assertThat(clean).doesNotContain("javascript:");
    }

    @Test
    void preservesAllowedHeadingsAndBlockElements() {
        String html = "<h1>t</h1><h2>s</h2><h3>ss</h3><blockquote>q</blockquote><ul><li>i</li></ul>";
        String clean = HtmlSanitizer.sanitize(html);
        assertThat(clean).contains("<h1>").contains("<h2>").contains("<h3>");
        assertThat(clean).contains("<blockquote>").contains("<ul>").contains("<li>");
    }
}
