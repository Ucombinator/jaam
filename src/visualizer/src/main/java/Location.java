/**
 * Created by timothyjohnson on 12/6/16.
 */
public class Location
{
    protected double left, right, top, bottom;
    protected double x, y, width, height;

    // Default constructor
    public Location()
    {
        this.setDefaultLocation();
    }

    // Copy constructor
    public Location(Location other)
    {
        this.left = other.left;
        this.right = other.right;
        this.top = other.top;
        this.bottom = other.bottom;

        this.x = other.x;
        this.y = other.y;
        this.width = other.width;
        this.height = other.height;
    }

    public void setDefaultLocation()
    {
        this.width = 1;
        this.height = 1;
        this.left = 0;
        this.right = 1;
        this.top = -1;
        this.bottom = 0;
        this.x = 0.5;
        this.y = -0.5;
    }

    public String toString()
    {
        return "(x, y) = (" + x + ", " + y + "), left = " + left + ", right = " + right
                + ", width = " + width + ", top = " + top + ", bottom = " + bottom + ", height = " + height;
    }
}
