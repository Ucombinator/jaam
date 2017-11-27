package org.ucombinator.jaam.visualizer.gui;

public class GUINodeStatus
{
    public double left, right, top, bottom;
    public double x, y, width, height;
    public double bboxWidth, bboxHeight;
    public double opacity;

    // Default constructor
    public GUINodeStatus()
    {
        this.setDefaultStatus();
    }

    // Copy constructor
    public GUINodeStatus(GUINodeStatus other)
    {
        this.left = other.left;
        this.right = other.right;
        this.top = other.top;
        this.bottom = other.bottom;

        this.x = other.x;
        this.y = other.y;
        this.width = other.width;
        this.height = other.height;

        this.opacity = other.opacity;
    }

    public void setDefaultStatus()
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
