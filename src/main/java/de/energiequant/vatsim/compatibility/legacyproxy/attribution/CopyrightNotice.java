package de.energiequant.vatsim.compatibility.legacyproxy.attribution;

import java.util.HashMap;
import java.util.Map;

import de.energiequant.apputils.misc.attribution.CopyrightNoticeProvider;
import de.energiequant.apputils.misc.attribution.Project;

public class CopyrightNotice implements CopyrightNoticeProvider {
    // TODO: extract information to a file/files
    // TODO: is there any way to automatically collect this information?

    private static final Map<String, String> NOTICES_BY_MAVEN_ID = new HashMap<>();

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

    static {
        add("commons-cli", "commons-cli", "1.5.0",
            "Apache Commons CLI\n"
                + "Copyright 2002-2021 The Apache Software Foundation\n"
                + "\n"
                + "This product includes software developed at\n"
                + "The Apache Software Foundation (https://www.apache.org/)."
        );
        add("commons-codec", "commons-codec", "1.17.1",
            "Apache Commons Codec\n"
                + "Copyright 2002-2024 The Apache Software Foundation\n"
                + "\n"
                + "This product includes software developed at\n"
                + "The Apache Software Foundation (https://www.apache.org/)."
        );
        add("org.apache.commons", "commons-compress", "1.27.1",
            "Apache Commons Compress\n"
                + "Copyright 2002-2024 The Apache Software Foundation\n"
                + "\n"
                + "This product includes software developed at\n"
                + "The Apache Software Foundation (https://www.apache.org/)."
        );
        add("commons-io", "commons-io", "2.19.0",
            "Apache Commons IO\n"
                + "Copyright 2002-2025 The Apache Software Foundation\n"
                + "\n"
                + "This product includes software developed at\n"
                + "The Apache Software Foundation (https://www.apache.org/)."
        );
        add("org.apache.commons", "commons-lang3", "3.12.0",
            "Apache Commons Lang\n"
                + "Copyright 2001-2021 The Apache Software Foundation\n"
                + "\n"
                + "This product includes software developed at\n"
                + "The Apache Software Foundation (https://www.apache.org/)."
        );
        add("org.apache.httpcomponents.client5", "httpclient5", "5.4.3",
            "Apache HttpClient\n"
                + "Copyright 1999-2021 The Apache Software Foundation\n"
                + "\n"
                + "This product includes software developed at\n"
                + "The Apache Software Foundation (http://www.apache.org/)."
        );
        add("org.apache.httpcomponents.core5", "httpcore5", "5.3.4",
            "Apache HttpComponents Core HTTP/1.1\n"
                + "Copyright 2005-2021 The Apache Software Foundation\n"
                + "\n"
                + "This product includes software developed at\n"
                + "The Apache Software Foundation (http://www.apache.org/)."
        );
        add("org.apache.httpcomponents.core5", "httpcore5-h2", "5.3.4",
            "Apache HttpComponents Core HTTP/2\n"
                + "Copyright 2005-2021 The Apache Software Foundation\n"
                + "\n"
                + "This product includes software developed at\n"
                + "The Apache Software Foundation (http://www.apache.org/)."
        );
        add("org.apache.logging.log4j", "log4j-api", "2.20.0",
            "Apache Log4j API\n"
                + "Copyright 1999-2023 The Apache Software Foundation\n"
                + "\n"
                + "This product includes software developed at\n"
                + "The Apache Software Foundation (http://www.apache.org/)."
        );
        add("org.apache.logging.log4j", "log4j-core", "2.20.0",
            "Apache Log4j Core\n"
                + "Copyright 1999-2012 Apache Software Foundation\n"
                + "\n"
                + "This product includes software developed at\n"
                + "The Apache Software Foundation (http://www.apache.org/).\n"
                + "\n"
                + "ResolverUtil.java\n"
                + "Copyright 2005-2006 Tim Fennell"
        );
        add("org.apache.logging.log4j", "log4j-slf4j-impl", "2.20.0",
            "Apache Log4j SLF4J Binding\n"
                + "Copyright 1999-2023 The Apache Software Foundation\n"
                + "\n"
                + "This product includes software developed at\n"
                + "The Apache Software Foundation (http://www.apache.org/)."
        );
        add("com.github.cliftonlabs", "json-simple", "4.0.1",
            /*
             * This is based on the Apache License file, release date and the homepage of
             * json-simple as there is no NOTICE file or similar.
             */
            "Copyright 2016-2022 Clifton Labs\n"
                + "\n"
                + "Davin Loegering designed and developed the 2.*, 3.*, 4.* versions.\n"
                + "Fang Yidong architected and developed the 1.* versions.\n"
                + "Chris Nokleberg contributed to the 1.* versions.\n"
                + "Dave Hughes contributed to the 1.* versions.\n"
                + "Chris (cbojar on github) provided fixes released in the 3.0.2 version.\n"
                + "Barry Lagerweij for the convenience put methods introduced in the 3.1.1 version."
        );
        add("org.slf4j", "slf4j-api", "1.7.36",
            // grabbed from MIT license file (note there are two, on root folder and in
            // slf4j-api)
            "Copyright (c) 2004-2022 QOS.ch Sarl (Switzerland)\n"
                + "All rights reserved."
        );
        add("org.vatplanner", "dataformats-vatsim-public", "0.1",
            "Copyright (c) 2016-2023 Daniel Neugebauer"
        );
        add("org.vatplanner.commons", "vatplanner-commons-base", "1.0",
            "Copyright (c) 2021-2025 Daniel Neugebauer"
        );
        add("de.energiequant.common", "app-utils-misc", "0.1-SNAPSHOT",
            "Copyright (c) 2021-2025 Daniel Neugebauer"
        );
        add("de.energiequant.common", "webdataretrieval", "0.2.6",
            "Copyright (c) 2016-2021 Daniel Neugebauer"
        );
        add("_inofficial.com.github.vatsimnetwork", "vatspy-data-project", "a88517cece1e81cd1d18552e7c630e47ddd7739e",
            // based on README file
            // also needs to be incorporated to AppConstants.SERVER_VAT_SPY_INTERNAL_HEADER
            "Copyright (c) 2019-2024 Niels Voogd, Adrian Bjerke and contributors\n"
                + "VAT-Spy is being developed by Ross Carlson\n"
                + "Previous data project lead: Néstor Pérez\n"
                + "\n"
                + "Please visit the linked website to view the full list of contributors."
        );
    }

    private static String buildKey(String groupId, String artifactId, String version) {
        return groupId + ":" + artifactId + ":" + version;
    }

    private static void add(String groupId, String artifactId, String version, String notice) {
        String previous = NOTICES_BY_MAVEN_ID.put(buildKey(groupId, artifactId, version), notice);
        if (previous != null) {
            throw new DuplicateKey(groupId, artifactId, version);
        }
    }

    @Override
    public String getNotice(Project project) {
        return getNotice(project.getGroupId(), project.getArtifactId(), project.getVersion());
    }

    private static String getNotice(String groupId, String artifactId, String version) {
        String notice = NOTICES_BY_MAVEN_ID.get(buildKey(groupId, artifactId, version));

        if (notice == null) {
            throw new MissingNotice(groupId, artifactId, version);
        }

        return notice;
    }
}
