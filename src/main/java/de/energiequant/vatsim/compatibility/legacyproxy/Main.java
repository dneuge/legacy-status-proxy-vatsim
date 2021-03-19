package de.energiequant.vatsim.compatibility.legacyproxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.energiequant.vatsim.compatibility.legacyproxy.gui.MainWindow;
import de.energiequant.vatsim.compatibility.legacyproxy.server.Server;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

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
}
