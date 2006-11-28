package ecologylab.xml.geom;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import ecologylab.xml.xml_inherit;

/**
 * Encapsulates a Rectangle2D.Double for use in translating to/from XML.
 * 
 * ***WARNING!!!***
 * 
 * Performing transformations (such as setFrame()) on the result of getRect() will cause this object
 * to become out of synch with its underlying Rectangle2D. DO NOT DO THIS!
 * 
 * If other transformation methods are required, either notify me, or implement them yourself. :D
 * 
 * Accessor methods (such as contains()) on the result of getRect() are fine.
 * 
 * @author Zach Toups (toupsz@gmail.com)
 */
public @xml_inherit class Rectangle2DDoubleState extends RectangularShape
{
    private Rectangle2D.Double shape = null;

    public Rectangle2DDoubleState()
    {
        super();
    }

    public Rectangle2DDoubleState(double x, double y, double width, double height)
    {
        super(x, y, width, height);
    }
    
    /**
     * Returns an Rectangle2D object represented by this.
     */
    public Rectangle2D.Double shape()
    {
        if (shape == null)
        {
            shape = new Rectangle2D.Double(x, y, w, h);
        }
        else if (shape.x != x || shape.y != y || shape.height != h || shape.width != w)
        {
            shape.setFrame(x, y, w, h);
        }

        return shape;
    }
    
    public boolean contains(Point2D p)
    {
        return shape().contains(p);
    }

    public boolean contains(Rectangle2D r)
    {
        return shape().contains(r);
    }

    public boolean contains(double x, double y)
    {
        return shape().contains(x, y);
    }

    public boolean contains(double x, double y, double w, double h)
    {
        return shape().contains(x, y, w, h);
    }

    public Rectangle getBounds()
    {
        return shape().getBounds();
    }

    public Rectangle2D getBounds2D()
    {
        return shape().getBounds2D();
    }

    public PathIterator getPathIterator(AffineTransform at)
    {
        return shape().getPathIterator(at);
    }

    public PathIterator getPathIterator(AffineTransform at, double flatness)
    {
        return shape().getPathIterator(at, flatness);
    }

    public boolean intersects(Rectangle2D r)
    {
        return shape().intersects(r);
    }

    public boolean intersects(double x, double y, double w, double h)
    {
        return shape().intersects(x, y, w, h);
    }
}
