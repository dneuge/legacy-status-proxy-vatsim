package de.energiequant.vatsim.compatibility.legacyproxy.gui;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Rectangle;
import java.util.function.BiConsumer;

import javax.swing.JScrollPane;

public class ScrollableLogOutputPaneWrapper extends JScrollPane {
    private final JScrollPane scrollPane;
    private final LogOutputPane logOutput;

    public ScrollableLogOutputPaneWrapper(BiConsumer<Component, Object> registration, Object constraints) {
        logOutput = new LogOutputPane(this::onLogUpdate);
        scrollPane = new JScrollPane(logOutput);
        registration.accept(scrollPane, constraints);
    }

    private void onLogUpdate() {
        scrollPane.invalidate();
        EventQueue.invokeLater(this::scrollToEndOfLog);
    }

    public void appendLogOutput() {
        logOutput.appendLogOutput();
    }

    public void startAutoUpdate() {
        logOutput.startAutoUpdate();
    }

    public void stopAutoUpdate() {
        logOutput.stopAutoUpdate();
    }

    private void scrollToEndOfLog() {
        scrollPane.getViewport().scrollRectToVisible(new Rectangle(0, logOutput.getHeight() - 1, 1, 1));
    }
}
