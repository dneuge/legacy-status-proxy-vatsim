package de.energiequant.vatsim.compatibility.legacyproxy.attribution;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.energiequant.apputils.misc.attribution.CopyrightNoticeProvider;
import de.energiequant.apputils.misc.attribution.Project;

public class CopyrightNotice implements CopyrightNoticeProvider {
    // TODO: is there any way to automatically collect this information?

    private static final Pattern PATTERN_SURROUNDING_EMPTINESS = Pattern.compile("^\\R*(.*?)\\R*$", Pattern.DOTALL);
    private static final int PATTERN_SURROUNDING_EMPTINESS_NON_EMPTY = 1;

    private static class XmlParserHandler extends DefaultHandler {
        private static final String ROOT_NODE_NAME = "copyrightNotices";
        private static final String NOTICE_NODE_NAME = "notice";

        private static final String GROUP_ID_ATTR_NAME = "groupId";
        private static final String ARTIFACT_ID_ATTR_NAME = "artifactId";
        private static final String VERSION_ATTR_NAME = "version";

        private StringBuilder cdata = new StringBuilder();

        private String groupId;
        private String artifactId;
        private String version;

        private boolean encounteredRootNode;

        private final CopyrightNotice collector = new CopyrightNotice();

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (ROOT_NODE_NAME.equals(qName)) {
                if (encounteredRootNode) {
                    throw new IllegalArgumentException("unexpected duplicate use of root node " + qName);
                }

                encounteredRootNode = true;
                return;
            }

            cdata = new StringBuilder();

            if (groupId != null || artifactId != null || version != null) {
                throw new IllegalArgumentException("invalid element within " + NOTICE_NODE_NAME + ": " + qName);
            }

            if (!NOTICE_NODE_NAME.equals(qName)) {
                throw new IllegalArgumentException("unexpected element: " + qName);
            }

            groupId = requireTrimmedNonEmptyAttribute(attributes, GROUP_ID_ATTR_NAME);
            artifactId = requireTrimmedNonEmptyAttribute(attributes, ARTIFACT_ID_ATTR_NAME);
            version = requireTrimmedNonEmptyAttribute(attributes, VERSION_ATTR_NAME);
        }

        private String requireTrimmedNonEmptyAttribute(Attributes attributes, String name) {
            String value = attributes.getValue(name);

            if (value == null || !value.trim().equals(value) || value.isEmpty()) {
                throw new IllegalArgumentException("invalid " + name + " (must be non-empty and trimmed): " + value);
            }

            return value;
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            cdata.append(ch, start, length);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (ROOT_NODE_NAME.equals(qName)) {
                return;
            }

            if (!NOTICE_NODE_NAME.equals(qName)) {
                throw new IllegalArgumentException("unexpected element: " + qName);
            }

            String notice = normalize(cdata.toString());
            if (notice.trim().isEmpty()) {
                throw new IllegalArgumentException("empty copyright notice for " + groupId + ":" + artifactId + ":" + version);
            }

            collector.add(groupId, artifactId, version, notice);

            groupId = null;
            artifactId = null;
            version = null;
        }
    }

    private final Map<String, String> noticesByMavenId = new HashMap<>();

    private static class MissingNotice extends RuntimeException {
        public MissingNotice(String groupId, String artifactId, String version) {
            super("Missing copyright notice for group ID " + groupId + ", artifact ID " + artifactId + ", version "
                      + version);
        }
    }

    private static class DuplicateKey extends RuntimeException {
        public DuplicateKey(String groupId, String artifactId, String version) {
            super("Duplicate key for group ID \"" + groupId + "\", artifact ID \"" + artifactId + "\", version \""
                      + version + "\"");
        }
    }

    private static String buildKey(String groupId, String artifactId, String version) {
        return groupId + ":" + artifactId + ":" + version;
    }

    private void add(String groupId, String artifactId, String version, String notice) {
        String previous = noticesByMavenId.put(buildKey(groupId, artifactId, version), notice);
        if (previous != null) {
            throw new DuplicateKey(groupId, artifactId, version);
        }
    }

    @Override
    public String getNotice(Project project) {
        return getNotice(project.getGroupId(), project.getArtifactId(), project.getVersion());
    }

    private String getNotice(String groupId, String artifactId, String version) {
        String notice = noticesByMavenId.get(buildKey(groupId, artifactId, version));

        if (notice == null) {
            throw new MissingNotice(groupId, artifactId, version);
        }

        return notice;
    }

    public static CopyrightNotice loadXML(Class<?> attributionRelativeClass) {
        XmlParserHandler handler = new XmlParserHandler();

        String filePath = attributionRelativeClass.getPackage().getName().replace('.', '/') + "/copyright-notices.xml";
        InputStream is = attributionRelativeClass.getClassLoader().getResourceAsStream(filePath);

        try {
            SAXParserFactory.newInstance().newSAXParser().parse(is, handler);
        } catch (SAXException | IOException | ParserConfigurationException ex) {
            throw new IllegalArgumentException("Failed to parse copyright notice file, copyrights are unavailable", ex);
        }

        return handler.collector;
    }

    private static String normalize(String s) {
        s = expandLeadingTabsToSpaces(s);
        s = unindent(s);
        s = removeSurroundingEmptyLines(s);
        return s;
    }

    private static String expandLeadingTabsToSpaces(String s) {
        List<String> out = new ArrayList<>();

        String[] lines = s.split("\\R");
        for (String line : lines) {
            StringBuilder outLine = new StringBuilder();
            char[] chars = line.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                char ch = chars[i];
                if (ch == '\t') {
                    outLine.append("    ");
                } else if (ch == ' ') {
                    outLine.append(ch);
                } else {
                    outLine.append(chars, i, chars.length - i);
                    break;
                }
            }

            out.add(outLine.toString());
        }

        return String.join("\n", out);
    }

    private static String unindent(String s) {
        int minIndent = Integer.MAX_VALUE;

        String[] lines = s.split("\\R");
        for (String line : lines) {
            // ignore empty lines
            if (line.trim().isEmpty()) {
                continue;
            }

            char[] chars = line.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                char ch = chars[i];
                if (ch == '\t') {
                    throw new IllegalArgumentException("tabs are not handled; convert to spaces first");
                }

                if (ch != ' ') {
                    if (minIndent > i) {
                        minIndent = i;
                    }
                    break;
                }
            }
        }

        List<String> out = new ArrayList<>();
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                out.add("");
                continue;
            }

            out.add(line.substring(minIndent));
        }

        return String.join("\n", out);
    }

    private static String removeSurroundingEmptyLines(String s) {
        Matcher matcher = PATTERN_SURROUNDING_EMPTINESS.matcher(s);
        if (!matcher.matches()) {
            return s;
        }

        return matcher.group(PATTERN_SURROUNDING_EMPTINESS_NON_EMPTY);
    }
}
