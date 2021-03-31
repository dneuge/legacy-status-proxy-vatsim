package de.energiequant.vatsim.compatibility.legacyproxy.attribution;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class AttributionParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(AttributionParser.class);

    private static class ParsingException extends RuntimeException {
        public ParsingException(String msg) {
            super(msg);
        }
    }

    private static class Handler extends DefaultHandler {
        private static class ElementNames {
            private static final String DEPENDENCY = "dependency";
            private static final String PROJECT_NAME = "name";
            private static final String PROJECT_VERSION = "version";
            private static final String PROJECT_URL = "projectUrl";
            private static final String LICENSES = "licenses";
            private static final String LICENSE_NAME = "name";

            private static class Ignored {
                private static final String ATTRIBUTION_REPORT = "attributionReport";
                private static final String DEPENDENCIES = "dependencies";
                private static final String ARTIFACT_GROUP_ID = "groupId";
                private static final String ARTIFACT_ID = "artifactId";
                private static final String ARTIFACT_TYPE = "type";
                private static final String ARTIFACT_DOWNLOAD_URLS = "downloadUrls";
                private static final String ARTIFACT_DOWNLOAD_URL_STRING = "string";
                private static final String LICENSE = "license";
                private static final String LICENSE_URL = "url";
            }
        }

        private final Collection<Project> projects = new ArrayList<>();

        private StringBuilder cdata;

        private String projectName;
        private String projectVersion;
        private String projectUrl;
        private Set<String> licenseNames;

        private boolean inLicenses;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            cdata = new StringBuilder();

            switch (qName) {
                case ElementNames.DEPENDENCY:
                    projectName = null;
                    projectVersion = null;
                    projectUrl = null;
                    licenseNames = new HashSet<>();
                    break;

                case ElementNames.LICENSES:
                    if (inLicenses) {
                        throw new ParsingException("Unexpected nesting of \"" + qName + "\" element");
                    }
                    inLicenses = true;
                    break;

                default:
                    // ignore all other elements
                    break;
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (inLicenses) {
                endLicenseElement(qName);
            } else {
                endProjectElement(qName);
            }
        }

        public void endProjectElement(String qName) {
            switch (qName) {
                case ElementNames.PROJECT_NAME:
                    projectName = getAndClearCData();
                    break;

                case ElementNames.PROJECT_VERSION:
                    projectVersion = getAndClearCData();
                    break;

                case ElementNames.PROJECT_URL:
                    projectUrl = getAndClearCData();
                    break;

                case ElementNames.DEPENDENCY:
                    Collection<License> licenses = new ArrayList<>();
                    for (String licenseName : licenseNames) {
                        licenses.add(License.byAlias(licenseName));
                    }
                    projects.add(new Project(projectName, projectVersion, projectUrl, licenses));
                    break;

                case ElementNames.Ignored.ATTRIBUTION_REPORT:
                case ElementNames.Ignored.DEPENDENCIES:
                case ElementNames.Ignored.ARTIFACT_DOWNLOAD_URLS:
                case ElementNames.Ignored.ARTIFACT_DOWNLOAD_URL_STRING:
                case ElementNames.Ignored.ARTIFACT_GROUP_ID:
                case ElementNames.Ignored.ARTIFACT_ID:
                case ElementNames.Ignored.ARTIFACT_TYPE:
                    // ignore expected unused elements
                    break;

                default:
                    // abort if unexpected elements are encountered
                    throw new ParsingException("Unhandled project element end: " + qName);
            }
        }

        public void endLicenseElement(String qName) {
            switch (qName) {
                case ElementNames.LICENSE_NAME:
                    licenseNames.add(getAndClearCData());
                    break;

                case ElementNames.LICENSES:
                    inLicenses = false;
                    break;

                case ElementNames.Ignored.LICENSE:
                case ElementNames.Ignored.LICENSE_URL:
                    // ignore expected unused elements
                    break;

                default:
                    // abort if unexpected elements are encountered
                    throw new ParsingException("Unhandled license element end: " + qName);
            }
        }

        private String getAndClearCData() {
            String s = cdata.toString();
            cdata = new StringBuilder();
            return s;
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            cdata.append(ch, start, length);
        }
    }

    public static Collection<Project> getProjects() {
        Handler handler = new Handler();

        String filePath = AttributionParser.class.getPackage().getName().replace('.', '/') + "/attribution.xml";
        InputStream is = AttributionParser.class.getClassLoader().getResourceAsStream(filePath);

        try {
            SAXParserFactory.newInstance().newSAXParser().parse(is, handler);
        } catch (SAXException | IOException | ParserConfigurationException ex) {
            LOGGER.error("Failed to parse attributions file, no license information is available", ex);
            return null;
        }

        return handler.projects;
    }

}
