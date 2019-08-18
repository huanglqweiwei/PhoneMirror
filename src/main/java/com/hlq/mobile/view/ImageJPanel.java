package com.hlq.mobile.view;

import javax.swing.*;
import java.awt.*;

public class ImageJPanel extends JPanel {
    private Image mImage;

    public void setImage(Image mImage) {
        this.mImage = mImage;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (mImage == null) {
            super.paintComponent(g);
        } else {
            g.drawImage(mImage,0,0,getWidth(),getHeight(),this);
        }
    }
}
