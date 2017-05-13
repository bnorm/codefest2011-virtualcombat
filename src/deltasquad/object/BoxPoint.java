package deltasquad.object;

import java.io.Serializable;

import deltasquad.utils.Utils;

public class BoxPoint implements Serializable {
   private static final long serialVersionUID = -2237448984421612107L;
   public double             x_;
   public double             y_;
   public double             sightAngle_;

   public BoxPoint(double x, double y, double sightAngle) {
      x_ = x;
      y_ = y;
      sightAngle_ = Utils.relative(sightAngle);
   }

   public boolean near(BoxPoint p) {
      return near(p, 36.0);
   }

   public boolean near(BoxPoint p, double dist) {
      return Math.abs(x_ - p.x_) <= dist && Math.abs(y_ - p.y_) <= dist;
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