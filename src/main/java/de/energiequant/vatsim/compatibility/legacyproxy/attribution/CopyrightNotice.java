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
        add("commons-cli", "commons-cli", "1.5.0",
            "Apache Commons CLI\n"
                + "Copyright 2002-2021 The Apache Software Foundation\n"
                + "\n"
                + "This product includes software developed at\n"
                + "The Apache Software Foundation (https://www.apache.org/)."
        );
        add("commons-codec", "commons-codec", "1.15",
            "Apache Commons Codec\n"
                + "Copyright 2002-2020 The Apache Software Foundation\n"
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
                + "Copyright (c) 2008 Alexander Beider & Stephen P. Morse."
        );
        add("org.apache.commons", "commons-compress", "1.21",
            "Apache Commons Compress\n"
                + "Copyright 2002-2021 The Apache Software Foundation\n"
                + "\n"
                + "This product includes software developed at\n"
                + "The Apache Software Foundation (https://www.apache.org/).\n"
                + "\n"
                + "---\n"
                + "\n"
                + "The files in the package org.apache.commons.compress.archivers.sevenz\n"
                + "were derived from the LZMA SDK, version 9.20 (C/ and CPP/7zip/),\n"
                + "which has been placed in the public domain:\n"
                + "\n"
                + "\"LZMA SDK is placed in the public domain.\" (http://www.7-zip.org/sdk.html)\n"
                + "\n"
                + "---\n"
                + "\n"
                + "The test file lbzip2_32767.bz2 has been copied from libbzip2's source\n"
                + "repository:\n"
                + "\n"
                + "This program, \"bzip2\", the associated library \"libbzip2\", and all\n"
                + "documentation, are copyright (C) 1996-2019 Julian R Seward.  All\n"
                + "rights reserved.\n"
                + "\n"
                + "Redistribution and use in source and binary forms, with or without\n"
                + "modification, are permitted provided that the following conditions\n"
                + "are met:\n"
                + "\n"
                + "1. Redistributions of source code must retain the above copyright\n"
                + "   notice, this list of conditions and the following disclaimer.\n"
                + "\n"
                + "2. The origin of this software must not be misrepresented; you must \n"
                + "   not claim that you wrote the original software.  If you use this \n"
                + "   software in a product, an acknowledgment in the product \n"
                + "   documentation would be appreciated but is not required.\n"
                + "\n"
                + "3. Altered source versions must be plainly marked as such, and must\n"
                + "   not be misrepresented as being the original software.\n"
                + "\n"
                + "4. The name of the author may not be used to endorse or promote \n"
                + "   products derived from this software without specific prior written \n"
                + "   permission.\n"
                + "\n"
                + "THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS\n"
                + "OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED\n"
                + "WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE\n"
                + "ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY\n"
                + "DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL\n"
                + "DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE\n"
                + "GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS\n"
                + "INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,\n"
                + "WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING\n"
                + "NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS\n"
                + "SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.\n"
                + "\n"
                + "Julian Seward, jseward@acm.org"
        );
        add("commons-io", "commons-io", "2.11.0",
            "Apache Commons IO\n"
                + "Copyright 2002-2021 The Apache Software Foundation\n"
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
        add("org.apache.httpcomponents.client5", "httpclient5", "5.1.4",
            "Apache HttpClient\n"
                + "Copyright 1999-2020 The Apache Software Foundation\n"
                + "\n"
                + "This product includes software developed at\n"
                + "The Apache Software Foundation (http://www.apache.org/)."
        );
        add("org.apache.httpcomponents.core5", "httpcore5", "5.1.5",
            "Apache HttpComponents Core HTTP/1.1\n"
                + "Copyright 2005-2020 The Apache Software Foundation\n"
                + "\n"
                + "This product includes software developed at\n"
                + "The Apache Software Foundation (http://www.apache.org/)."
        );
        add("org.apache.httpcomponents.core5", "httpcore5-h2", "5.1.5",
            "Apache HttpComponents Core HTTP/2\n"
                + "Copyright 2005-2020 The Apache Software Foundation\n"
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
        add("org.vatplanner", "dataformats-vatsim-public", "0.1-pre230626",
            "Copyright (c) 2016-2023 Daniel Neugebauer"
        );
        add("de.energiequant.common", "webdataretrieval", "0.2.5",
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
