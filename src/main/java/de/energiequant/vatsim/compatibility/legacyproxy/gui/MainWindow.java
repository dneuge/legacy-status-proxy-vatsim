package de.energiequant.vatsim.compatibility.legacyproxy.gui;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.EnumMap;
import java.util.EnumSet;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import javax.swing.WindowConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.energiequant.apputils.misc.gui.AboutWindow;
import de.energiequant.apputils.misc.gui.ScrollableLogOutputPaneWrapper;
import de.energiequant.apputils.misc.gui.SwingHelper;
import de.energiequant.vatsim.compatibility.legacyproxy.Configuration;
import de.energiequant.vatsim.compatibility.legacyproxy.Main;
import de.energiequant.vatsim.compatibility.legacyproxy.server.Server;
import de.energiequant.vatsim.compatibility.legacyproxy.server.Server.State;

public class MainWindow extends JFrame {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainWindow.class);

    private final ScrollableLogOutputPaneWrapper log;
    private final JToggleButton runStopButton;
    private final JLabel statusLabel = SwingHelper.stylePlain(new JLabel());

    private static final EnumSet<Server.State> RUN_STOP_BUTTON_ENABLED_STATES = EnumSet.of(
        Server.State.BLOCKED_BY_DISCLAIMER,
        Server.State.RUNNING,
        Server.State.HTTP_SERVER_STOPPED
    );
    private static final EnumSet<Server.State> RUN_STOP_BUTTON_SELECTED_STATES = EnumSet.of(Server.State.RUNNING);

    private static final EnumMap<Server.State, String> MESSAGE_BY_SERVER_STATE = new EnumMap<>(Server.State.class);

    private final AboutWindow aboutWindow;
    private final ConfigurationWindow configurationWindow = new ConfigurationWindow();

    static {
        MESSAGE_BY_SERVER_STATE.put(
            Server.State.INITIAL,
            "Server has not been started yet."
        );
        MESSAGE_BY_SERVER_STATE.put(
            Server.State.BLOCKED_BY_DISCLAIMER,
            "Disclaimer needs to be accepted to run HTTP server."
        );
        MESSAGE_BY_SERVER_STATE.put(
            Server.State.RUNNING,
            "Server is running."
        );
        MESSAGE_BY_SERVER_STATE.put(
            Server.State.HTTP_SERVER_STOPPED,
            "HTTP server has been stopped."
        );
        MESSAGE_BY_SERVER_STATE.put(
            Server.State.FULLY_STOPPED,
            "Server has been stopped. Application restart is needed to run it again."
        );
    }

    public MainWindow(Runnable onCloseCallback) {
        super("Legacy Status Proxy for VATSIM");

        Configuration config = Main.getConfiguration();
        if (!config.isSaneLocation()) {
            aboutWindow = new AboutWindow(Main.getApplicationInfo(), Main.getDisclaimerState());
        } else {
            aboutWindow = new AboutWindow(Main.getApplicationInfo(), Main.getDisclaimerState(), config::save);
        }

        setSize(800, 600);
        setMinimumSize(new Dimension(600, 400));

        setLocationRelativeTo(null);
        aboutWindow.setLocationRelativeTo(this);
        configurationWindow.setLocationRelativeTo(this);

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 4;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        log = new ScrollableLogOutputPaneWrapper(this::add, gbc);

        gbc.gridy++;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        add(statusLabel, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        runStopButton = new JToggleButton("Run/Stop");
        runStopButton.addActionListener(this::onRunStopClicked);
        add(runStopButton, gbc);

        gbc.gridx++;
        JButton configureButton = new JButton("Configure");
        configureButton.addActionListener(this::onConfigureClicked);
        add(configureButton, gbc);

        gbc.gridx++;
        gbc.anchor = GridBagConstraints.EAST;
        JButton aboutButton = new JButton("About");
        aboutButton.addActionListener(this::onAboutClicked);
        add(aboutButton, gbc);

        gbc.gridx++;
        JButton quitButton = new JButton("Quit");
        quitButton.addActionListener(this::onQuitClicked);
        add(quitButton, gbc);

        log.appendLogOutput();

        setVisible(true);

        log.startAutoUpdate();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                log.stopAutoUpdate();
                onCloseCallback.run();
            }
        });

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        Main.getServer().addStateChangeListener(this::onServerStateChanged);
        onServerStateChanged();

        if (!Main.getDisclaimerState().isAccepted()) {
            aboutWindow.showDisclaimer();
        }
    }

    private void onServerStateChanged() {
        updateRunStopButton();
        updateStatusLabel();
        popupDisclaimerIfServerBlocked();
    }

    private void updateStatusLabel() {
        State state = Main.getServer().getState();
        statusLabel.setText(MESSAGE_BY_SERVER_STATE.getOrDefault(state, ""));
    }

    private void popupDisclaimerIfServerBlocked() {
        if (Main.getServer().getState() == Server.State.BLOCKED_BY_DISCLAIMER) {
            LOGGER.debug("Server indicates to be blocked by disclaimer, popping up disclaimer again");
            aboutWindow.showDisclaimer();
        }
    }

    private void updateRunStopButton() {
        State serverState = Main.getServer().getState();
        runStopButton.setSelected(RUN_STOP_BUTTON_SELECTED_STATES.contains(serverState));
        runStopButton.setEnabled(RUN_STOP_BUTTON_ENABLED_STATES.contains(serverState));
    }

    private void onRunStopClicked(ActionEvent event) {
        boolean shouldStart = runStopButton.isSelected();
        if (shouldStart) {
            Main.getServer().startHttpServer();
        } else {
            Main.getServer().stopHttpServer();
        }

        EventQueue.invokeLater(this::updateRunStopButton);
    }

    private void onConfigureClicked(ActionEvent event) {
        configurationWindow.setVisible(true);
    }

    private void onAboutClicked(ActionEvent event) {
        aboutWindow.setVisible(true);
    }

    private void onQuitClicked(ActionEvent event) {
        System.exit(0);
    }
}
