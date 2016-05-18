
//Stores a view of a graph (left, right, top, bottom)
public class View
{
	double left, right, top, bottom;
	View next;
	View prev;
	boolean initialized;
	
	public View(boolean initialized)
	{
		this.left = 0;
		this.right = 1;
		this.top = 0;
		this.bottom = 1;
		this.next = this;
		this.prev = this;
		this.initialized = initialized;
	}
	
	public View(double left, double right, double top, double bottom)
	{
		this.left = left;
		this.right = right;
		this.top = top;
		this.bottom = bottom;
		this.next = this;
		this.prev = this;
		validateView();
		initialized = true;
	}
	
	public View(View toCopy)
	{
		this.left = toCopy.left;
		this.right = toCopy.right;
		this.top = toCopy.top;
		this.bottom = toCopy.bottom;
		this.next = this;
		this.prev = this;
		initialized = toCopy.initialized;
	}
	
	public void setPrev(View prev)
	{
		this.prev = prev;
	}
	
	public View getPrev()
	{
		return this.prev;
	}
	
	public void setNext(View next)
	{
		this.next = next;
	}
	
	public View getNext()
	{
		return this.next;
	}
	
	public void validateView()
	{
		double temp;
		
		if(left>right)
		{
			temp=left;
			left=right;
			right=temp;
		}
		if(top>bottom)
		{
			temp=top;
			top=bottom;
			bottom=temp;
		}
		
		if(left < 0)
			left = 0;
		if(right > 1)
			right = 1;
		if(top < 0)
			top = 0;
		if(bottom > 1)
			bottom = 1;
	}
	
	public void reset()
	{
		left = 0;
		right = 1;
		top = 0;
		bottom = 1;
	}
	
	public void shiftView(int hor, int ver)
	{
		double xSpan = this.right - this.left;
		double ySpan = this.bottom - this.top;
		
		this.left = this.left + hor*xSpan/10;
		this.right = this.right + hor*xSpan/10;
		if(this.left<0)
		{
			this.left = 0;
			this.right = xSpan;
		}
		if(this.right>1)
		{
			this.left = 1-xSpan;
			this.right = 1;
		}
		
		this.top = this.top + ver*ySpan/10;
		this.bottom = this.bottom + ver*ySpan/10;
		if(this.top<0)
		{
			this.top = 0;
			this.bottom = ySpan;
		}
		if(this.bottom>1)
		{
			this.top = 1-ySpan;
			this.bottom = 1;
		}
	}
	
	public void zoomNPan(double centerX, double centerY, double factor)
	{
		this.left = this.left + centerX - (this.left+this.right)/2;
		this.right = this.right + centerX - (this.left+this.right)/2;
		this.top = this.top + centerY - (this.top+this.bottom)/2;
		this.bottom = this.bottom + centerY - (this.top+this.bottom)/2;
		
		this.left = centerX - (centerX - this.left)*factor; 
		this.right = centerX + (this.right - centerX)*factor; 
		this.top = centerY - (centerY - this.top)*factor; 
		this.bottom = centerY + (this.bottom - centerY)*factor;
		
		//Make sure that our coordinates are all within [0,1]
		validateView();
	}
}
