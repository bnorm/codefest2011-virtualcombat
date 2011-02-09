package deltasquad.object;

import java.io.Serializable;
import java.util.LinkedList;

import deltasquad.utils.Utils;

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
      return near(p, 20.0);
   }

   public boolean near(Point p, double dist) {
      return Utils.sqr(x_ - p.x_) + Utils.sqr(y_ - p.y_) <= Utils.sqr(dist);
   }

   @Override
   public boolean equals(Object obj) {
      if (obj instanceof Point) {
         Point point = (Point) obj;
         return near(point, 10.0);
      }
      return super.equals(obj);
   }
}