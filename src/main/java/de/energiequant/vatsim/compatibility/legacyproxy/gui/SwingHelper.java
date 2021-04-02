package de.energiequant.vatsim.compatibility.legacyproxy.gui;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;

public class SwingHelper {
    public static JLabel stylePlain(JLabel label) {
        label.setFont(label.getFont().deriveFont(Font.PLAIN));
        return label;
    }

    public static JLabel styleBold(JLabel label) {
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        return label;
    }

    public static void onChange(JTextField field, Runnable runnable) {
        field.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                EventQueue.invokeLater(runnable);
            }
        });
    }

    public static void onChange(JCheckBox checkBox, Runnable runnable) {
        checkBox.addChangeListener(event -> EventQueue.invokeLater(runnable));
    }

    public static void onChange(JSpinner spinner, Runnable runnable) {
        spinner.addChangeListener(event -> EventQueue.invokeLater(runnable));
    }

    public static JSpinner unformattedNumericSpinner(JSpinner spinner) {
        spinner.setEditor(new JSpinner.NumberEditor(spinner, "#"));
        return spinner;
    }
}
