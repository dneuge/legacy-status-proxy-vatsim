package de.energiequant.vatsim.compatibility.legacyproxy;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.energiequant.vatsim.compatibility.legacyproxy.attribution.AttributionParser;
import de.energiequant.vatsim.compatibility.legacyproxy.attribution.CopyrightNotice;
import de.energiequant.vatsim.compatibility.legacyproxy.attribution.License;
import de.energiequant.vatsim.compatibility.legacyproxy.attribution.Project;
import de.energiequant.vatsim.compatibility.legacyproxy.gui.MainWindow;
import de.energiequant.vatsim.compatibility.legacyproxy.logging.BufferAppender;
import de.energiequant.vatsim.compatibility.legacyproxy.server.Server;
import de.energiequant.vatsim.compatibility.legacyproxy.server.Server.State;
import de.energiequant.vatsim.compatibility.legacyproxy.utils.ResourceUtils;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final Collection<Project> DEPENDENCIES = AttributionParser.getProjects();

    private static final String DISCLAIMER = ResourceUtils
        .getResourceContentAsString(Main.class, "disclaimer.txt", StandardCharsets.UTF_8)
        .orElseThrow(DisclaimerNotFound::new);

    private static Configuration configuration;
    private static Server server;

    // TODO: set application meta data automatically during build
    private static final String APPLICATION_JAR_NAME = "legacy-status-proxy-vatsim.jar";
    private static final String APPLICATION_NAME = "Legacy status proxy for VATSIM";
    private static final String APPLICATION_VERSION = "0.80.2";
    private static final String APPLICATION_URL = "https://github.com/dneuge/legacy-status-proxy-vatsim";
    private static final String APPLICATION_COPYRIGHT = "Copyright (c) 2021 Daniel Neugebauer";

    private static final License EFFECTIVE_LICENSE = License.MIT;

    private static final String OPTION_NAME_HELP = "help";
    private static final String OPTION_NAME_SHOW_DISCLAIMER = "disclaimer";
    private static final String OPTION_NAME_ACCEPT_DISCLAIMER = "accept-disclaimer-and-licenses";
    private static final String OPTION_NAME_NO_GUI = Launcher.OPTION_NAME_NO_GUI;
    private static final String OPTION_NAME_NO_CLASSPATH_CHECK = Launcher.OPTION_NAME_NO_CLASSPATH_CHECK;
    private static final String OPTION_NAME_CONFIG_PATH = "config";
    private static final String OPTION_NAME_SAVE_CONFIG = "save-config";
    private static final String OPTION_NAME_VERSION = "version";
    private static final String OPTION_NAME_SHOW_LICENSE = "license";

    private static final String DEFAULT_CONFIG_PATH = "legacy-status-proxy-vatsim.properties";

    private static class DisclaimerNotFound extends RuntimeException {
        public DisclaimerNotFound() {
            super("Disclaimer could not be found");
        }
    }

    public static void main(String[] args) throws Exception {
        Options options = new Options();
        addOptions(options);

        CommandLineParser parser = new DefaultParser();
        CommandLine parameters = parser.parse(options, args);
        if (parameters.hasOption(OPTION_NAME_HELP)) {
            new HelpFormatter().printHelp(APPLICATION_JAR_NAME, options);
            System.exit(0);
        }

        if (parameters.hasOption(OPTION_NAME_VERSION)) {
            printVersion();
            System.exit(0);
        }

        if (parameters.hasOption(OPTION_NAME_SHOW_LICENSE)) {
            String licenseName = parameters.getOptionValue(OPTION_NAME_SHOW_LICENSE);
            License license = null;
            try {
                license = License.valueOf(licenseName);
            } catch (Exception ex) {
                // ignore
            }

            if (license == null) {
                System.err.println("License " + licenseName + " not found");
                System.err.println();
                System.err.println("Available: " + sortedLicenseKeys().collect(Collectors.joining(", ")));
                System.exit(1);
            }

            printLicense(license);
            System.exit(0);
        }

        boolean shouldRunHeadless = GraphicsEnvironment.isHeadless() || parameters.hasOption(OPTION_NAME_NO_GUI);

        String configFilePath = parameters.getOptionValue(OPTION_NAME_CONFIG_PATH, DEFAULT_CONFIG_PATH);
        configuration = new Configuration(new File(configFilePath));

        if (parameters.hasOption(OPTION_NAME_ACCEPT_DISCLAIMER)) {
            configuration.setDisclaimerAccepted(true);
        }

        if (parameters.hasOption(OPTION_NAME_SHOW_DISCLAIMER)
            || (!configuration.isDisclaimerAccepted() && shouldRunHeadless)) {
            printDisclaimer();
            System.exit(1);
        }

        if (parameters.hasOption(OPTION_NAME_SAVE_CONFIG)) {
            configuration.save();
        }

        boolean shouldShutdownOnStartFailure = shouldRunHeadless;
        server = new Server(shouldShutdownOnStartFailure);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                server.stopAll();
            }
        });

        server.startHttpServer();

        configuration.addDisclaimerListener(() -> {
            if (!configuration.isDisclaimerAccepted() && (server.getState() == State.RUNNING)) {
                LOGGER.warn("Stopping HTTP server because disclaimer has not been accepted");
                server.stopHttpServer();
            } else if (configuration.isDisclaimerAccepted() && (server.getState() == State.BLOCKED_BY_DISCLAIMER)) {
                LOGGER.info("Disclaimer has been accepted, starting HTTP server");
                server.startHttpServer();
            }
        });

        if (shouldRunHeadless) {
            BufferAppender.disableAndClearAll();
        } else {
            new MainWindow(() -> {
                server.stopAll();
                System.exit(0);
            });
        }
    }

    private static void printLicense(License license) {
        String html = license.getText();

        // TODO: deprecation warning is a false-positive in Eclipse?
        @SuppressWarnings("deprecation")
        String text = StringEscapeUtils.unescapeHtml4(html.replaceAll("<[^>]*?>", ""));

        System.out.println(text);
    }

    private static void printVersion() {
        System.out.println(APPLICATION_NAME);
        System.out.println("version " + APPLICATION_VERSION);
        System.out.println(APPLICATION_URL);
        License license = getEffectiveLicense();
        System.out.println("released under " + license.getCanonicalName() + " [" + license.name() + "]");
        System.out.println(APPLICATION_COPYRIGHT);
        System.out.println();
        System.out.println(AppConstants.DEPENDENCIES_CAPTION);
        getDependencies().stream().sorted(Comparator.comparing(Project::getName)).forEachOrdered(Main::printDependency);
        System.out.println();
        System.out.println("Generic copies of all involved software licenses are included with this program.");
        System.out.println(
            "To view a license run with --" + OPTION_NAME_SHOW_LICENSE + " <"
                + sortedLicenseKeys().collect(Collectors.joining("|")) + ">");
        System.out.println("The corresponding license IDs are shown in brackets [ ] above.");
    }

    private static void printDependency(Project project) {
        System.out.println();
        System.out.println(project.getName() + " (version " + project.getVersion() + ")");
        project.getUrl().ifPresent(System.out::println);

        StringBuilder sb = new StringBuilder();
        sb.append(AppConstants.PROJECT_DEPENDENCY_LICENSE_INTRO);
        List<License> licenses = project.getLicenses() //
            .stream() //
            .sorted(Comparator.comparing(License::getCanonicalName)) //
            .collect(Collectors.toList());
        boolean isFirst = true;
        for (License license : licenses) {
            if (!isFirst) {
                sb.append(" & ");
            } else {
                isFirst = false;
            }
            sb.append(license.getCanonicalName());
            sb.append(" [");
            sb.append(license.name());
            sb.append("]");
        }
        System.out.println(sb.toString());

        System.out.println();
        System.out.println(indent("    ", CopyrightNotice.getNotice(project)));
        System.out.println();
    }

    private static String indent(String prefix, String s) {
        Pattern pattern = Pattern.compile("^", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(s);
        return matcher.replaceAll(prefix);
    }

    private static void printDisclaimer() {
        System.out.println(DISCLAIMER);
    }

    private static Stream<String> sortedLicenseKeys() {
        return Arrays.stream(License.values()) //
            .map(License::name) //
            .sorted();
    }

    private static void addOptions(Options options) {
        options.addOption(Option
            .builder()
            .longOpt(OPTION_NAME_HELP)
            .desc("prints the help text")
            .build());

        options.addOption(Option
            .builder()
            .longOpt(OPTION_NAME_VERSION)
            .desc("prints all version, dependency and license information")
            .build());

        options.addOption(Option
            .builder()
            .longOpt(OPTION_NAME_SHOW_DISCLAIMER)
            .desc("prints the disclaimer")
            .build());

        options.addOption(Option
            .builder()
            .longOpt(OPTION_NAME_SHOW_LICENSE)
            .hasArg()
            .desc("prints the specified license, available: "
                + sortedLicenseKeys().collect(Collectors.joining(", ")))
            .build());

        options.addOption(Option
            .builder()
            .longOpt(OPTION_NAME_NO_GUI)
            .desc("disables GUI to force running headless on CLI")
            .build());

        options.addOption(Option
            .builder()
            .longOpt(OPTION_NAME_NO_CLASSPATH_CHECK)
            .desc("disables check for possibly broken Java class path at application startup")
            .build());

        options.addOption(Option
            .builder()
            .longOpt(OPTION_NAME_ACCEPT_DISCLAIMER)
            .desc("accept disclaimer and licenses (required before proxy server can be used)")
            .build());

        options.addOption(Option
            .builder()
            .longOpt(OPTION_NAME_CONFIG_PATH)
            .hasArg()
            .desc("path to configuration file to be used")
            .build());

        options.addOption(Option
            .builder()
            .longOpt(OPTION_NAME_SAVE_CONFIG)
            .desc("saves the configuration after processing CLI options (creates the file if it does not exist yet)")
            .build());
    }

    public static Configuration getConfiguration() {
        return configuration;
    }

    public static Collection<Project> getDependencies() {
        return DEPENDENCIES;
    }

    public static String getApplicationName() {
        return APPLICATION_NAME;
    }

    public static String getApplicationUrl() {
        return APPLICATION_URL;
    }

    public static String getApplicationVersion() {
        return APPLICATION_VERSION;
    }

    public static String getApplicationCopyright() {
        return APPLICATION_COPYRIGHT;
    }

    public static License getEffectiveLicense() {
        return EFFECTIVE_LICENSE;
    }

    public static String getDisclaimer() {
        return DISCLAIMER;
    }

    public static Server getServer() {
        return server;
    }
}
