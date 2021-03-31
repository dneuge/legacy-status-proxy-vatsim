package de.energiequant.vatsim.compatibility.legacyproxy;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.energiequant.vatsim.compatibility.legacyproxy.attribution.AttributionParser;
import de.energiequant.vatsim.compatibility.legacyproxy.attribution.License;
import de.energiequant.vatsim.compatibility.legacyproxy.attribution.Project;
import de.energiequant.vatsim.compatibility.legacyproxy.gui.MainWindow;
import de.energiequant.vatsim.compatibility.legacyproxy.server.Server;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final Collection<Project> DEPENDENCIES = AttributionParser.getProjects();

    // TODO: set application meta data automatically during build
    private static final String APPLICATION_NAME = "Legacy status proxy for VATSIM";
    private static final String APPLICATION_VERSION = "0.1";
    private static final String APPLICATION_URL = "https://github.com/dneuge/legacy-status-proxy-vatsim";

    private static final License EFFECTIVE_LICENSE = License.MIT;

    public static void main(String[] args) throws Exception {
        Server server = new Server();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                server.stop();
            }
        });

        server.start();

        new MainWindow(() -> {
            server.stop();
            System.exit(0);
        });
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
}
