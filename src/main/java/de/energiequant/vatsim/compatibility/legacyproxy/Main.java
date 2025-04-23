package de.energiequant.vatsim.compatibility.legacyproxy;

import static de.energiequant.vatsim.compatibility.legacyproxy.AppConstants.APPLICATION_COPYRIGHT;
import static de.energiequant.vatsim.compatibility.legacyproxy.AppConstants.APPLICATION_JAR_NAME;
import static de.energiequant.vatsim.compatibility.legacyproxy.AppConstants.APPLICATION_NAME;
import static de.energiequant.vatsim.compatibility.legacyproxy.AppConstants.APPLICATION_URL;
import static de.energiequant.vatsim.compatibility.legacyproxy.AppConstants.APPLICATION_VERSION;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.energiequant.apputils.misc.ApplicationInfo;
import de.energiequant.apputils.misc.DisclaimerState;
import de.energiequant.apputils.misc.ResourceUtils;
import de.energiequant.apputils.misc.attribution.AttributionParser;
import de.energiequant.apputils.misc.attribution.CopyrightNoticeProvider;
import de.energiequant.apputils.misc.attribution.CopyrightNotices;
import de.energiequant.apputils.misc.attribution.License;
import de.energiequant.apputils.misc.attribution.Project;
import de.energiequant.apputils.misc.cli.CommandLineAbout;
import de.energiequant.apputils.misc.logging.BufferAppender;
import de.energiequant.vatsim.compatibility.legacyproxy.attribution.VatSpyMetaData;
import de.energiequant.vatsim.compatibility.legacyproxy.gui.MainWindow;
import de.energiequant.vatsim.compatibility.legacyproxy.server.Server;
import de.energiequant.vatsim.compatibility.legacyproxy.server.Server.State;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final Collection<Project> DEPENDENCIES = AttributionParser.getProjects(Main.class);

    private static final CopyrightNoticeProvider COPYRIGHT_NOTICE_PROVIDER = CopyrightNotices.loadXML(Main.class);

    private static final String DISCLAIMER = ResourceUtils
        .getRelativeResourceContentAsString(Main.class, "disclaimer.txt", StandardCharsets.UTF_8)
        .orElseThrow(DisclaimerNotFound::new);

    private static final String DISCLAIMER_ACCEPTANCE_TEXT = "I understand and accept the disclaimer and licenses (required to start the server)";

    private static Configuration configuration;
    private static Server server;

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

    private static final DateTimeFormatter UTC_HUMAN_READABLE_DATE_FORMATTER = DateTimeFormatter
        .ofPattern("d MMMM YYYY", Locale.US)
        .withZone(ZoneId.of("UTC"));

    private static class DisclaimerNotFound extends RuntimeException {
        public DisclaimerNotFound() {
            super("Disclaimer could not be found");
        }
    }

    private static final ApplicationInfo APP_INFO = new ApplicationInfo() {
        @Override
        public Collection<Project> getDependencies() {
            return DEPENDENCIES;
        }

        @Override
        public CopyrightNoticeProvider getCopyrightNoticeProvider() {
            return COPYRIGHT_NOTICE_PROVIDER;
        }

        @Override
        public String getApplicationName() {
            return APPLICATION_NAME;
        }

        @Override
        public String getApplicationUrl() {
            return APPLICATION_URL;
        }

        @Override
        public String getApplicationVersion() {
            return APPLICATION_VERSION;
        }

        @Override
        public List<String> getExtraInfo() {
            return Collections.singletonList(
                "Includes VAT-Spy data from "
                    + VatSpyMetaData.getIncludedDataTimestamp()
                                    .map(UTC_HUMAN_READABLE_DATE_FORMATTER::format)
                                    .orElse("unknown date")
            );
        }

        @Override
        public String getApplicationCopyright() {
            return APPLICATION_COPYRIGHT;
        }

        @Override
        public License getEffectiveLicense() {
            return EFFECTIVE_LICENSE;
        }

        @Override
        public Optional<String> getDisclaimer() {
            return Optional.of(DISCLAIMER);
        }

        @Override
        public Optional<String> getDisclaimerAcceptanceText() {
            return Optional.of(DISCLAIMER_ACCEPTANCE_TEXT);
        }
    };

    private static final DisclaimerState disclaimerState = new DisclaimerState(APP_INFO);

    public static void main(String[] args) throws Exception {
        Options options = new Options();
        addOptions(options);

        CommandLineParser parser = new DefaultParser();
        CommandLine parameters = parser.parse(options, args);
        if (parameters.hasOption(OPTION_NAME_HELP)) {
            new HelpFormatter().printHelp(APPLICATION_JAR_NAME, options);
            System.exit(0);
        }

        CommandLineAbout about = new CommandLineAbout(System.out, APP_INFO, "--" + OPTION_NAME_SHOW_LICENSE);
        if (parameters.hasOption(OPTION_NAME_VERSION)) {
            about.printVersion();
            System.exit(0);
        }

        if (parameters.hasOption(OPTION_NAME_SHOW_LICENSE)) {
            String licenseName = parameters.getOptionValue(OPTION_NAME_SHOW_LICENSE);
            about.printLicenseAndQuit(licenseName);
        }

        boolean shouldRunHeadless = GraphicsEnvironment.isHeadless() || parameters.hasOption(OPTION_NAME_NO_GUI);

        String configFilePath = parameters.getOptionValue(OPTION_NAME_CONFIG_PATH, DEFAULT_CONFIG_PATH);
        configuration = new Configuration(new File(configFilePath), disclaimerState);

        if (parameters.hasOption(OPTION_NAME_ACCEPT_DISCLAIMER)) {
            disclaimerState.setAccepted(true);
        }

        if (parameters.hasOption(OPTION_NAME_SHOW_DISCLAIMER)
            || (!disclaimerState.isAccepted() && shouldRunHeadless)) {
            about.printDisclaimer();
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

        disclaimerState.addListener(() -> {
            if (!disclaimerState.isAccepted() && (server.getState() == State.RUNNING)) {
                LOGGER.warn("Stopping HTTP server because disclaimer has not been accepted");
                server.stopHttpServer();
            } else if (disclaimerState.isAccepted() && (server.getState() == State.BLOCKED_BY_DISCLAIMER)) {
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

    private static Stream<String> sortedLicenseKeys() {
        return Arrays.stream(License.values())
                     .map(License::name)
                     .sorted();
    }

    private static void addOptions(Options options) {
        options.addOption(Option.builder()
                                .longOpt(OPTION_NAME_HELP)
                                .desc("prints the help text")
                                .build());

        options.addOption(Option.builder()
                                .longOpt(OPTION_NAME_VERSION)
                                .desc("prints all version, dependency and license information")
                                .build());

        options.addOption(Option.builder()
                                .longOpt(OPTION_NAME_SHOW_DISCLAIMER)
                                .desc("prints the disclaimer")
                                .build());

        options.addOption(Option.builder()
                                .longOpt(OPTION_NAME_SHOW_LICENSE)
                                .hasArg()
                                .desc("prints the specified license, available: "
                                          + sortedLicenseKeys().collect(Collectors.joining(", ")))
                                .build());

        options.addOption(Option.builder()
                                .longOpt(OPTION_NAME_NO_GUI)
                                .desc("disables GUI to force running headless on CLI")
                                .build());

        options.addOption(Option.builder()
                                .longOpt(OPTION_NAME_NO_CLASSPATH_CHECK)
                                .desc("disables check for possibly broken Java class path at application startup")
                                .build());

        options.addOption(Option.builder()
                                .longOpt(OPTION_NAME_ACCEPT_DISCLAIMER)
                                .desc("accept disclaimer and licenses (required before proxy server can be used)")
                                .build());

        options.addOption(Option.builder()
                                .longOpt(OPTION_NAME_CONFIG_PATH)
                                .hasArg()
                                .desc("path to configuration file to be used")
                                .build());

        options.addOption(Option.builder()
                                .longOpt(OPTION_NAME_SAVE_CONFIG)
                                .desc("saves the configuration after processing CLI options (creates the file if it does not exist yet)")
                                .build());
    }

    public static ApplicationInfo getApplicationInfo() {
        return APP_INFO;
    }

    public static DisclaimerState getDisclaimerState() {
        return disclaimerState;
    }

    public static Configuration getConfiguration() {
        return configuration;
    }

    public static Server getServer() {
        return server;
    }
}
