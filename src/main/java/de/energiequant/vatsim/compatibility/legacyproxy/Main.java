package de.energiequant.vatsim.compatibility.legacyproxy;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.energiequant.vatsim.compatibility.legacyproxy.attribution.AttributionParser;
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
    private static final String APPLICATION_JAR_NAME = "legacy-status-proxy-vatsim-0.1-SNAPSHOT.jar";
    private static final String APPLICATION_NAME = "Legacy status proxy for VATSIM";
    private static final String APPLICATION_VERSION = "0.1";
    private static final String APPLICATION_URL = "https://github.com/dneuge/legacy-status-proxy-vatsim";

    private static final License EFFECTIVE_LICENSE = License.MIT;

    private static final String OPTION_NAME_HELP = "help";
    private static final String OPTION_NAME_ACCEPT_DISCLAIMER = "accept-disclaimer";
    private static final String OPTION_NAME_NO_GUI = "no-gui";
    private static final String OPTION_NAME_CONFIG_PATH = "config";

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
            System.exit(1);
        }

        boolean shouldRunHeadless = GraphicsEnvironment.isHeadless() || parameters.hasOption(OPTION_NAME_NO_GUI);

        String configFilePath = parameters.getOptionValue(OPTION_NAME_CONFIG_PATH, DEFAULT_CONFIG_PATH);
        configuration = new Configuration(new File(configFilePath));

        if (parameters.hasOption(OPTION_NAME_ACCEPT_DISCLAIMER)) {
            configuration.setDisclaimerAccepted(true);
        }

        if (!configuration.isDisclaimerAccepted() && shouldRunHeadless) {
            printDisclaimer();
            System.exit(1);
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

    private static void printDisclaimer() {
        // TODO: convert line end chars?
        System.err.println(DISCLAIMER);
    }

    private static void addOptions(Options options) {
        options.addOption(Option
            .builder()
            .longOpt(OPTION_NAME_HELP)
            .desc("prints the help text")
            .build());

        options.addOption(Option
            .builder()
            .longOpt(OPTION_NAME_NO_GUI)
            .desc("disables GUI to force running headless on CLI")
            .build());

        options.addOption(Option
            .builder()
            .longOpt(OPTION_NAME_ACCEPT_DISCLAIMER)
            .desc("accept disclaimer (required before proxy server can be used)")
            .build());

        options.addOption(Option
            .builder()
            .longOpt(OPTION_NAME_CONFIG_PATH)
            .hasArg()
            .desc("path to configuration file to be used")
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
