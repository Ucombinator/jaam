package org.ucombinator.jaam.visualizer.gui;

public class GUINodeStatus
{
    public double left, right, top, bottom;
    public double x, y, width, height;

    // The width and height of the node's children within the same level of the hierarchy
    public double bboxWidth, bboxHeight;
    public double opacity;

    public GUINodeStatus()
    {
        this.width = 1;
        this.height = 1;
        this.left = 0;
        this.right = 1;
        this.top = -1;
        this.bottom = 0;
        this.x = 0.5;
        this.y = -0.5;
        this.opacity = 1;
    }

    public String toString()
    {
        return "(x, y) = (" + x + ", " + y + "), left = " + left + ", right = " + right
                + ", width = " + width + ", top = " + top + ", bottom = " + bottom + ", height = " + height
                + ", opacity = " + opacity;
    }
}
