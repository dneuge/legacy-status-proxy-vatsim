package de.energiequant.vatsim.compatibility.legacyproxy.gui;

import java.awt.Font;

import javax.swing.JLabel;

public class SwingHelper {
    public static JLabel stylePlain(JLabel label) {
        label.setFont(label.getFont().deriveFont(Font.PLAIN));
        return label;
    }

    public static JLabel styleBold(JLabel label) {
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        return label;
    }
}
