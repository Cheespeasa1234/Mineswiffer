import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JSlider;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

public class LeviSlider extends JSlider {
    TitledBorder border;
    public LeviSlider(int min, int max, int minTick, int majTick, int val, boolean snap, String titleName) {
        super(min, max, val);
        setPaintTicks(true);
        setSnapToTicks(snap);
        setMinorTickSpacing(minTick);
        setMajorTickSpacing(majTick);
        setPreferredSize(new Dimension(200, 50));
        setBackground(Color.GRAY);
        setOpaque(true);

        border = new TitledBorder(new LineBorder(Color.BLACK, 1), titleName + ": " + val);
        border.setTitleColor(Color.BLACK);
        setBorder(border);

        addChangeListener(e -> {
            updateValue(getValue());
        });
    }

    public void updateValue(int val) {
        border.setTitle(border.getTitle().split(":")[0] + ": " + val);
    }
}
