package com.cundong.izhihu.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Converts the untrusted article body into safe, reader-ready markup.
 *
 * <p>This class deliberately has no Android dependencies so the security and
 * table-of-contents behavior can be protected by ordinary JVM tests.</p>
 */
public final class ArticleHtmlRenderer {

    private static final int MIN_TEXT_ZOOM = 100;
    private static final int MAX_TEXT_ZOOM = 200;
    private static final int MAX_SECTION_TITLE_LENGTH = 80;

    private ArticleHtmlRenderer() {
    }

    public static RenderedArticle render(String articleBody) {
        Document document = Jsoup.parseBodyFragment(articleBody == null ? "" : articleBody);
        document.select("script, iframe, object, embed, form, input, button, select, textarea")
                .remove();

        for (Element element : document.getAllElements()) {
            // Work from a copy because attributes are removed while iterating.
            List<Attribute> attributes = new ArrayList<Attribute>(
                    element.attributes().asList());
            for (Attribute attribute : attributes) {
                String key = attribute.getKey().toLowerCase(Locale.US);
                String value = attribute.getValue();
                if (key.startsWith("on") || "srcdoc".equals(key)
                        || (isUrlAttribute(key) && isUnsafeUrl(value))
                        || ("style".equals(key) && isUnsafeStyle(value))) {
                    element.removeAttr(attribute.getKey());
                }
            }

            if ("a".equals(element.tagName()) && element.hasAttr("href")) {
                element.attr("rel", "noopener noreferrer");
                element.removeAttr("target");
            }
        }

        ArrayList<Section> sections = new ArrayList<Section>();
        Elements headings = document.select("h1, h2, h3, .question-title");
        int sectionNumber = 1;
        for (Element heading : headings) {
            String title = normalizeSectionTitle(heading.text());
            if (title.length() == 0) {
                continue;
            }
            String id = "reader-section-" + sectionNumber++;
            heading.attr("id", id);
            heading.addClass("reader-section");
            sections.add(new Section(id, title));
        }

        return new RenderedArticle(document.body().html(), sections);
    }

    public static int textZoomForFontScale(float fontScale) {
        int zoom = Math.round(fontScale * 100f);
        return Math.max(MIN_TEXT_ZOOM, Math.min(MAX_TEXT_ZOOM, zoom));
    }

    private static boolean isUrlAttribute(String key) {
        return "href".equals(key) || "src".equals(key) || "action".equals(key)
                || "poster".equals(key) || "xlink:href".equals(key);
    }

    private static boolean isUnsafeUrl(String value) {
        String normalized = value == null ? "" : value
                .replaceAll("[\\p{Cntrl}\\s]+", "")
                .toLowerCase(Locale.US);
        return normalized.startsWith("javascript:")
                || normalized.startsWith("vbscript:")
                || normalized.startsWith("data:")
                || normalized.startsWith("file:")
                || normalized.startsWith("content:")
                || normalized.startsWith("intent:");
    }

    private static boolean isUnsafeStyle(String value) {
        String normalized = value == null ? "" : value.toLowerCase(Locale.US);
        return normalized.contains("expression(") || normalized.contains("javascript:")
                || normalized.contains("vbscript:");
    }

    private static String normalizeSectionTitle(String value) {
        String title = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (title.length() > MAX_SECTION_TITLE_LENGTH) {
            return title.substring(0, MAX_SECTION_TITLE_LENGTH - 1) + "…";
        }
        return title;
    }

    public static final class RenderedArticle {
        private final String bodyHtml;
        private final List<Section> sections;

        private RenderedArticle(String bodyHtml, List<Section> sections) {
            this.bodyHtml = bodyHtml;
            this.sections = Collections.unmodifiableList(new ArrayList<Section>(sections));
        }

        public String getBodyHtml() {
            return bodyHtml;
        }

        public List<Section> getSections() {
            return sections;
        }
    }

    public static final class Section {
        private final String id;
        private final String title;

        private Section(String id, String title) {
            this.id = id;
            this.title = title;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }
    }
}
