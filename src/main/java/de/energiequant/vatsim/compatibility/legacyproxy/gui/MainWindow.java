package de.energiequant.vatsim.compatibility.legacyproxy.gui;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.WindowConstants;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.logging.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.energiequant.vatsim.compatibility.legacyproxy.Main;
import de.energiequant.vatsim.compatibility.legacyproxy.logging.BufferAppender;
import de.energiequant.vatsim.compatibility.legacyproxy.logging.BufferAppender.FormattedEvent;
import de.energiequant.vatsim.compatibility.legacyproxy.server.Server;
import de.energiequant.vatsim.compatibility.legacyproxy.server.Server.State;
import de.energiequant.vatsim.compatibility.legacyproxy.utils.ResourceUtils;

public class MainWindow extends JFrame {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainWindow.class);

    private final JEditorPane logOutput;
    private final JScrollPane logScrollPane;
    private final JToggleButton runStopButton;
    private final JLabel statusLabel = SwingHelper.stylePlain(new JLabel());

    private static final EnumSet<Server.State> RUN_STOP_BUTTON_ENABLED_STATES = EnumSet.of( //
        Server.State.BLOCKED_BY_DISCLAIMER, //
        Server.State.RUNNING, //
        Server.State.HTTP_SERVER_STOPPED //
    );
    private static final EnumSet<Server.State> RUN_STOP_BUTTON_SELECTED_STATES = EnumSet.of(Server.State.RUNNING);

    private static final EnumMap<Server.State, String> MESSAGE_BY_SERVER_STATE = new EnumMap<>(Server.State.class);

    private final AboutWindow aboutWindow = new AboutWindow();
    private final ConfigurationWindow configurationWindow = new ConfigurationWindow();

    private static final Map<Level, String> LOG_STYLES_BY_LEVEL = new HashMap<Level, String>();

    // TODO: split to log component

    static {
        LOG_STYLES_BY_LEVEL.put(Level.TRACE, styleForColor("#1E8449"));
        LOG_STYLES_BY_LEVEL.put(Level.DEBUG, styleForColor("#D4AC0D"));
        LOG_STYLES_BY_LEVEL.put(Level.WARN, styleForColor("#A93226"));
        LOG_STYLES_BY_LEVEL.put(Level.ERROR, styleForColor("#B03A2E"));
        LOG_STYLES_BY_LEVEL.put(Level.FATAL, styleForColor("#B03A2E"));

        MESSAGE_BY_SERVER_STATE.put(//
            Server.State.INITIAL, //
            "Server has not been started yet." //
        );
        MESSAGE_BY_SERVER_STATE.put( //
            Server.State.BLOCKED_BY_DISCLAIMER, //
            "Disclaimer needs to be accepted to run HTTP server." //
        );
        MESSAGE_BY_SERVER_STATE.put( //
            Server.State.RUNNING, //
            "Server is running." //
        );
        MESSAGE_BY_SERVER_STATE.put( //
            Server.State.HTTP_SERVER_STOPPED, //
            "HTTP server has been stopped." //
        );
        MESSAGE_BY_SERVER_STATE.put( //
            Server.State.FULLY_STOPPED, //
            "Server has been stopped. Application restart is needed to run it again." //
        );
    }

    private static String styleForColor(String color) {
        return "style='color:" + color + "' ";
    }

    public MainWindow(Runnable onCloseCallback) {
        super("Legacy Status Proxy for VATSIM");

        setSize(800, 600);
        setMinimumSize(new Dimension(600, 400));

        setLocationRelativeTo(null);
        aboutWindow.setLocationRelativeTo(this);
        configurationWindow.setLocationRelativeTo(this);

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        logOutput = new JEditorPane();
        logOutput.setEditable(false);
        logOutput.setContentType("text/html");
        logOutput.setText(getDefaultHtml());

        logScrollPane = new JScrollPane(logOutput);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 4;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        add(logScrollPane, gbc);

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

        appendLogOutput();

        setVisible(true);

        Thread logUpdateThread = new Thread(() -> {
            while (true) {
                appendLogOutput();
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ex) {
                    return;
                }
            }
        });
        logUpdateThread.start();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                logUpdateThread.interrupt();
                onCloseCallback.run();
            }
        });

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        Main.getServer().addStateChangeListener(this::onServerStateChanged);
        onServerStateChanged();

        if (!Main.getConfiguration().isDisclaimerAccepted()) {
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

    private String getDefaultHtml() {
        return ResourceUtils.getRelativeResourceContentAsString(getClass(), "LogOutput.html", StandardCharsets.UTF_8) //
            .orElseThrow(() -> new RuntimeException("missing log output template"));
    }

    private void appendLogOutput() {
        StringBuilder sb = new StringBuilder();

        for (BufferAppender appender : BufferAppender.getInstances()) {
            List<FormattedEvent> events = appender.getFormattedEventsAndClear();
            for (FormattedEvent event : events) {
                String message = event.getMessage();

                sb.append("<li ");
                sb.append(LOG_STYLES_BY_LEVEL.getOrDefault(event.getLevel(), ""));
                sb.append(">");
                sb.append(
                    Arrays.stream(sanitizeHtml(message).split("\n"))
                          .map(line -> line.replaceFirst("^\t", "&nbsp;&nbsp;&nbsp;&nbsp;"))
                          .collect(Collectors.joining("<br/>"))
                );
                sb.append("</li>");
            }
        }

        if (sb.length() == 0) {
            return;
        }

        HTMLDocument document = (HTMLDocument) logOutput.getDocument();
        Element listElement = document.getElement(document.getDefaultRootElement(), StyleConstants.NameAttribute,
            HTML.Tag.UL);

        try {
            document.insertBeforeEnd(listElement, sb.toString());
        } catch (BadLocationException | IOException ex) {
            ex.printStackTrace();
        }

        logOutput.invalidate();
        logScrollPane.invalidate();
        EventQueue.invokeLater(this::scrollToEndOfLog);
    }

    private String sanitizeHtml(String message) {
        // TODO: deprecation warning is a false-positive in Eclipse?
        return StringEscapeUtils.escapeHtml4(message);
    }

    private void scrollToEndOfLog() {
        logScrollPane.getViewport().scrollRectToVisible(new Rectangle(0, logOutput.getHeight() - 1, 1, 1));
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
