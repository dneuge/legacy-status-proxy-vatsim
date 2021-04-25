package de.energiequant.vatsim.compatibility.legacyproxy.gui;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
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

    public static JLabel styleMonospaced(JLabel label) {
        label.setFont(Font.decode(Font.MONOSPACED));
        return label;
    }

    public static void onChange(JTextField field, Runnable runnable) {
        field.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                EventQueue.invokeLater(runnable);
            }

            @Override
            public void keyReleased(KeyEvent e) {
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

    public static void onSelect(JComboBox<?> comboBox, Runnable runnable) {
        comboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                EventQueue.invokeLater(runnable);
            }
        });
    }

    public static JSpinner unformattedNumericSpinner(JSpinner spinner) {
        spinner.setEditor(new JSpinner.NumberEditor(spinner, "#"));
        return spinner;
    }

    public static Insets sumInsets(Insets... insets) {
        int sumTop = 0;
        int sumLeft = 0;
        int sumBottom = 0;
        int sumRight = 0;

        for (Insets x : insets) {
            sumTop += x.top;
            sumLeft += x.left;
            sumBottom += x.bottom;
            sumRight += x.right;
        }

        return new Insets(sumTop, sumLeft, sumBottom, sumRight);
    }
}
