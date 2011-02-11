package deltasquad.object;

import java.io.Serializable;
import java.util.LinkedList;

public class Point implements Serializable {
   private static final long serialVersionUID = -2237448984421612107L;
   public double             x_;
   public double             y_;
   public double             sightAngle_;

   public LinkedList<Point>  nearPoints_;

   public Point(double x, double y, double sightAngle) {
      x_ = x;
      y_ = y;
      sightAngle_ = sightAngle;
      nearPoints_ = new LinkedList<Point>();
   }

   public boolean near(Point p) {
      return (Math.abs(x_ - p.x_) <= 40.0 && Math.abs(y_ - p.y_) <= 40.0);
   }

   @Override
   public boolean equals(Object obj) {
      if (obj instanceof Point) {
         Point point = (Point) obj;
         return (Math.abs(x_ - point.x_) < 18.0 && Math.abs(y_ - point.y_) < 18.0);
      }
      return super.equals(obj);
   }
}