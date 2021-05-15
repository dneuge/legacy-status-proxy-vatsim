package de.energiequant.vatsim.compatibility.legacyproxy.attribution;

import java.util.HashMap;
import java.util.Map;

public class CopyrightNotice {
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
        add("commons-cli", "commons-cli", "1.4", //
            "Apache Commons CLI\n"
                + "Copyright 2001-2017 The Apache Software Foundation\n"
                + "\n"
                + "This product includes software developed at\n"
                + "The Apache Software Foundation (http://www.apache.org/)." //
        );
        add("commons-codec", "commons-codec", "1.13", //
            "Apache Commons Codec\n"
                + "Copyright 2002-2019 The Apache Software Foundation\n"
                + "\n"
                + "This product includes software developed at\n"
                + "The Apache Software Foundation (https://www.apache.org/).\n"
                + "\n"
                + "src/test/org/apache/commons/codec/language/DoubleMetaphoneTest.java\n"
                + "contains test data from http://aspell.net/test/orig/batch0.tab.\n"
                + "Copyright (C) 2002 Kevin Atkinson (kevina@gnu.org)\n"
                + "\n"
                + "===============================================================================\n"
                + "\n"
                + "The content of package org.apache.commons.codec.language.bm has been translated\n"
                + "from the original php source code available at http://stevemorse.org/phoneticinfo.htm\n"
                + "with permission from the original authors.\n"
                + "Original source copyright:\n"
                + "Copyright (c) 2008 Alexander Beider & Stephen P. Morse." //
        );
        add("org.apache.commons", "commons-compress", "1.19", //
            "Apache Commons Compress\n"
                + "Copyright 2002-2019 The Apache Software Foundation\n"
                + "\n"
                + "This product includes software developed at\n"
                + "The Apache Software Foundation (https://www.apache.org/).\n"
                + "\n"
                + "The files in the package org.apache.commons.compress.archivers.sevenz\n"
                + "were derived from the LZMA SDK, version 9.20 (C/ and CPP/7zip/),\n"
                + "which has been placed in the public domain:\n"
                + "\n"
                + "\"LZMA SDK is placed in the public domain.\" (http://www.7-zip.org/sdk.html)" //
        );
        add("commons-io", "commons-io", "2.8.0", //
            "Apache Commons IO\n"
                + "Copyright 2002-2020 The Apache Software Foundation\n"
                + "\n"
                + "This product includes software developed at\n"
                + "The Apache Software Foundation (https://www.apache.org/)." //
        );
        add("org.apache.commons", "commons-lang3", "3.12.0", //
            "Apache Commons Lang\n"
                + "Copyright 2001-2021 The Apache Software Foundation\n"
                + "\n"
                + "This product includes software developed at\n"
                + "The Apache Software Foundation (https://www.apache.org/)." //
        );
        add("org.apache.httpcomponents.client5", "httpclient5", "5.0.3", //
            "Apache HttpClient\n"
                + "Copyright 1999-2020 The Apache Software Foundation\n"
                + "\n"
                + "This product includes software developed at\n"
                + "The Apache Software Foundation (http://www.apache.org/)." //
        );
        add("org.apache.httpcomponents.core5", "httpcore5", "5.0.3", //
            "Apache HttpComponents Core HTTP/1.1\n"
                + "Copyright 2005-2020 The Apache Software Foundation\n"
                + "\n"
                + "This product includes software developed at\n"
                + "The Apache Software Foundation (http://www.apache.org/)." //
        );
        add("org.apache.httpcomponents.core5", "httpcore5-h2", "5.0.2", //
            "Apache HttpComponents Core HTTP/2\n"
                + "Copyright 2005-2020 The Apache Software Foundation\n"
                + "\n"
                + "This product includes software developed at\n"
                + "The Apache Software Foundation (http://www.apache.org/)." //
        );
        add("org.apache.logging.log4j", "log4j-api", "2.13.3", //
            "Apache Log4j API\n"
                + "Copyright 1999-2020 The Apache Software Foundation\n"
                + "\n"
                + "This product includes software developed at\n"
                + "The Apache Software Foundation (http://www.apache.org/)." //
        );
        add("org.apache.logging.log4j", "log4j-core", "2.13.3", //
            "Apache Log4j Core\n"
                + "Copyright 1999-2012 Apache Software Foundation\n"
                + "\n"
                + "This product includes software developed at\n"
                + "The Apache Software Foundation (http://www.apache.org/).\n"
                + "\n"
                + "ResolverUtil.java\n"
                + "Copyright 2005-2006 Tim Fennell" //
        );
        add("org.apache.logging.log4j", "log4j-slf4j-impl", "2.13.3", //
            "Apache Log4j SLF4J Binding\n"
                + "Copyright 1999-2020 The Apache Software Foundation\n"
                + "\n"
                + "This product includes software developed at\n"
                + "The Apache Software Foundation (http://www.apache.org/)."//
        );
        add("com.github.cliftonlabs", "json-simple", "3.1.1", //
            /*
             * This is based on the Apache License file and the homepage of json-simple as
             * there is no NOTICE file or similar.
             */
            "Copyright 2016 Clifton Labs\n"
                + "\n"
                + "Davin Loegering designed and developed the 2.* and 3.* versions.\n"
                + "Fang Yidong architected and developed the 1.* versions.\n"
                + "Chris Nokleberg contributed to the 1.* versions.\n"
                + "Dave Hughes contributed to the 1.* versions.\n"
                + "Chris (cbojar on github) provided fixes released in the 3.0.2 version.\n"
                + "Barry Lagerweij for the convenience put methods introduced in the 3.1.1 version." //
        );
        add("org.slf4j", "slf4j-api", "1.7.30", //
            // grabbed from MIT license file
            "Copyright (c) 2004-2017 QOS.ch\n"
                + " All rights reserved." //
        );
        add("org.vatplanner", "dataformats-vatsim-public", "0.1-pre210419", //
            "Copyright (c) 2016 Daniel Neugebauer" //
        );
        add("de.energiequant.common", "webdataretrieval", "0.2.2", //
            "Copyright (c) 2016 Daniel Neugebauer" //
        );
        add("_inofficial.com.github.vatsimnetwork", "vatspy-data-project", "487beca0f11f1cee5cb32c001591cd6845241e6a", //
            // based on README file
            // also needs to be incorporated to AppConstants.SERVER_VAT_SPY_INTERNAL_HEADER
            "Copyright (c) 2019-2021 Néstor Pérez, Niels Voogd, Adrian Bjerke, Alex Long and contributors\n"
                + "VAT-Spy is being developed by Ross Carlson\n"
                + "\n"
                + "Please visit the linked website to view the full list of contributors." //
        );

        // FIXME: pin version before release
        add("org.vatplanner", "dataformats-vatsim-public", "0.1-SNAPSHOT", //
            "Copyright (c) 2016-2021 Daniel Neugebauer" //
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

    public static String getNotice(Project project) {
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
