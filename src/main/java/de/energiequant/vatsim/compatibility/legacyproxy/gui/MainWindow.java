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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
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

import de.energiequant.vatsim.compatibility.legacyproxy.Main;
import de.energiequant.vatsim.compatibility.legacyproxy.logging.BufferAppender;
import de.energiequant.vatsim.compatibility.legacyproxy.logging.BufferAppender.FormattedEvent;
import de.energiequant.vatsim.compatibility.legacyproxy.utils.ResourceUtils;

public class MainWindow extends JFrame {
    private final JEditorPane logOutput;
    private final JScrollPane logScrollPane;
    private final JToggleButton runStopButton;

    private static final boolean RUN_STOP_RUNNING = true;
    private static final boolean RUN_STOP_STOPPED = false;

    private final AboutWindow aboutWindow = new AboutWindow();

    private static final Map<Level, String> LOG_STYLES_BY_LEVEL = new HashMap<Level, String>();

    // FIXME: split to log component
    // FIXME: log lines are duplicated

    static {
        LOG_STYLES_BY_LEVEL.put(Level.TRACE, styleForColor("#1E8449"));
        LOG_STYLES_BY_LEVEL.put(Level.DEBUG, styleForColor("#D4AC0D"));
        LOG_STYLES_BY_LEVEL.put(Level.WARN, styleForColor("#A93226"));
        LOG_STYLES_BY_LEVEL.put(Level.ERROR, styleForColor("#B03A2E"));
        LOG_STYLES_BY_LEVEL.put(Level.FATAL, styleForColor("#B03A2E"));
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
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        runStopButton = new JToggleButton("Run/Stop");
        // TODO: update button state depending on server state
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

        if (!Main.getConfiguration().isDisclaimerAccepted()) {
            aboutWindow.showDisclaimer();
        }
    }

    private String getDefaultHtml() {
        return ResourceUtils.getResourceContentAsString(getClass(), "LogOutput.html", StandardCharsets.UTF_8) //
            .orElseThrow(() -> new RuntimeException("missing log output template"));
    }

    private void appendLogOutput() {
        HTMLDocument document = (HTMLDocument) logOutput.getDocument();
        Element listElement = document.getElement(document.getDefaultRootElement(), StyleConstants.NameAttribute,
            HTML.Tag.UL);

        boolean updated = false;

        for (BufferAppender appender : BufferAppender.getInstances()) {
            List<FormattedEvent> events = appender.getFormattedEventsAndClear();
            for (FormattedEvent event : events) {
                updated = true;

                String message = event.getMessage();
                String attrStyle = LOG_STYLES_BY_LEVEL.getOrDefault(event.getLevel(), "");

                // FIXME: proper safe conversion to HTML
                String messageHtml = "<li " + attrStyle + ">" + sanitizeHtml(message).replace("\n", "<br/>") + "</li>";

                try {
                    document.insertBeforeEnd(listElement, messageHtml);
                } catch (BadLocationException | IOException ex) {
                    ex.printStackTrace();
                }
            }
        }

        if (updated) {
            logOutput.invalidate();
            logScrollPane.invalidate();
            EventQueue.invokeLater(this::scrollToEndOfLog);
        }
    }

    private String sanitizeHtml(String message) {
        // FIXME: why does Eclipse think the class is deprecated?
        return StringEscapeUtils.escapeHtml4(message);
    }

    private void scrollToEndOfLog() {
        logScrollPane.getViewport().scrollRectToVisible(new Rectangle(0, logOutput.getHeight() - 1, 1, 1));
    }

    private void onRunStopClicked(ActionEvent event) {
        // FIXME: toggle based on state
        boolean buttonState = runStopButton.isSelected();
        if (buttonState == RUN_STOP_RUNNING) {
            Main.getServer().start();
        } else {
            Main.getServer().stop();
        }
    }

    private void onConfigureClicked(ActionEvent event) {
        // TODO: implement
    }

    private void onAboutClicked(ActionEvent event) {
        aboutWindow.setVisible(true);
    }

    private void onQuitClicked(ActionEvent event) {
        // TODO: implement
    }
}
