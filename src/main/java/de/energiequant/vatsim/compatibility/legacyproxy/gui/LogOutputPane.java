package de.energiequant.vatsim.compatibility.legacyproxy.gui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JEditorPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.logging.log4j.Level;

import de.energiequant.apputils.misc.ResourceUtils;
import de.energiequant.apputils.misc.logging.BufferAppender;

public class LogOutputPane extends JEditorPane {
    private static final Map<Level, String> LOG_STYLES_BY_LEVEL = new HashMap<Level, String>();

    private final Runnable updateCallback;

    private final Thread logUpdateThread;

    static {
        LOG_STYLES_BY_LEVEL.put(Level.TRACE, styleForColor("#1E8449"));
        LOG_STYLES_BY_LEVEL.put(Level.DEBUG, styleForColor("#D4AC0D"));
        LOG_STYLES_BY_LEVEL.put(Level.WARN, styleForColor("#A93226"));
        LOG_STYLES_BY_LEVEL.put(Level.ERROR, styleForColor("#B03A2E"));
        LOG_STYLES_BY_LEVEL.put(Level.FATAL, styleForColor("#B03A2E"));
    }

    public LogOutputPane() {
        this(null);
    }

    public LogOutputPane(Runnable updateCallback) {
        super();

        this.updateCallback = updateCallback;

        setEditable(false);
        setContentType("text/html");
        setText(getDefaultHtml());

        logUpdateThread = new Thread(() -> {
            while (true) {
                appendLogOutput();
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ex) {
                    return;
                }
            }
        });
    }

    private String getDefaultHtml() {
        return ResourceUtils.getRelativeResourceContentAsString(getClass(), "LogOutput.html", StandardCharsets.UTF_8)
                            .orElseThrow(() -> new RuntimeException("missing log output template"));
    }

    public void appendLogOutput() {
        StringBuilder sb = new StringBuilder();

        for (BufferAppender appender : BufferAppender.getInstances()) {
            List<BufferAppender.FormattedEvent> events = appender.getFormattedEventsAndClear();
            for (BufferAppender.FormattedEvent event : events) {
                sb.append("<li ");
                sb.append(LOG_STYLES_BY_LEVEL.getOrDefault(event.getLevel(), ""));
                sb.append(">");
                appendTextAsEditorHtml(sb, event.getMessage());
                sb.append("</li>");
            }
        }

        if (sb.length() == 0) {
            return;
        }

        HTMLDocument document = (HTMLDocument) getDocument();
        Element listElement = document.getElement(document.getDefaultRootElement(), StyleConstants.NameAttribute,
                                                  HTML.Tag.UL
        );

        try {
            document.insertBeforeEnd(listElement, sb.toString());
        } catch (BadLocationException | IOException ex) {
            ex.printStackTrace();
        }

        invalidate();

        if (updateCallback != null) {
            updateCallback.run();
        }
    }

    /**
     * Sanitizes and transforms the specified string so that it can be properly used on a {@link JEditorPane}.
     * The output is directly appended to the given {@link StringBuilder}.
     * <p>
     * The following shortcomings of {@link JEditorPane} are being worked around by this method:
     * </p>
     * <ul>
     * <li>tabs ({@code \t}) at the beginning of a line are expanded to 4 non-breaking spaces ({@code &nbsp;});
     * otherwise they would just be rendered as a single white-space</li>
     * <li>multi-line strings need to be wrapped as paragraphs ({@code <p>...</p>}); line-breaks ({@code <br/>}) only
     * work for rendering but get lost when copying to clipboard, while paragraphs are only treated like line-breaks
     * both during rendering and for the clipboard</li>
     * </ul>
     *
     * @param sb where to append the transformed content to
     * @param s  the input to be transformed
     */
    private void appendTextAsEditorHtml(StringBuilder sb, String s) {
        s = sanitizeHtml(s);

        if (s.isEmpty()) {
            return;
        }

        String[] lines = s.split("\n");

        boolean multipleLines = lines.length > 1;
        for (int i = 0; i < lines.length; i++) {
            if (multipleLines) {
                if (i > 0) {
                    sb.append("</p>");
                }
                sb.append("<p>");
            }

            if (lines[i].startsWith("\t")) {
                sb.append("&nbsp;&nbsp;&nbsp;&nbsp;");
                if (lines[i].length() > 1) {
                    sb.append(lines[i], 1, lines[i].length() - 1);
                }
            } else {
                sb.append(lines[i]);
            }
        }

        if (multipleLines) {
            sb.append("</p>");
        }
    }

    public void startAutoUpdate() {
        logUpdateThread.start();
    }

    public void stopAutoUpdate() {
        logUpdateThread.interrupt();
    }

    private String sanitizeHtml(String message) {
        // TODO: deprecation warning is a false-positive in Eclipse?
        return StringEscapeUtils.escapeHtml4(message);
    }

    private static String styleForColor(String color) {
        return "style='color:" + color + "' ";
    }
}
