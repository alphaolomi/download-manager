package com.alphaolomi;

import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

class ProgressRenderer extends JProgressBar implements TableCellRenderer {
    ProgressRenderer(int min, int max) {
        super(min, max);
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        setValue((int) ((Float) value).floatValue());
        return this;
    }
}