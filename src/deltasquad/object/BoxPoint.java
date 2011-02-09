package deltasquad.object;

import java.io.Serializable;

public class BoxPoint implements Serializable {
   private static final long serialVersionUID = -2237448984421612107L;
   public double             x_;
   public double             y_;
   public double             sightAngle_;

   public BoxPoint(double x, double y, double sightAngle) {
      x_ = x;
      y_ = y;
      sightAngle_ = sightAngle;
   }

   public boolean near(BoxPoint p) {
      return near(p, 36.0);
   }

   public boolean near(BoxPoint p, double dist) {
      return (Math.abs(x_ - p.x_) <= dist && Math.abs(y_ - p.y_) < 2.0)
            || (Math.abs(y_ - p.y_) <= dist && Math.abs(x_ - p.x_) < 2.0);
   }

   @Override
   public boolean equals(Object obj) {
      if (obj instanceof BoxPoint) {
         BoxPoint p = (BoxPoint) obj;
         return x_ == p.x_ && y_ == p.y_;
      }
      return super.equals(obj);
   }

}