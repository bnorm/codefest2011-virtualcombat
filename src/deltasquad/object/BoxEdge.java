package deltasquad.object;

import java.awt.geom.Line2D;

public class BoxEdge extends Line2D.Double {
   private static final long serialVersionUID = -2461145688771462782L;

   private boolean           virtical_;
   private double            maxX;
   private double            minX;
   private double            maxY;
   private double            minY;

   BoxPoint                  start;
   BoxPoint                  end;

   public BoxEdge(BoxPoint p1, BoxPoint p2) {
      init(p1, p2);
   }

   private void init(BoxPoint p1, BoxPoint p2) {
      if (p1.near(p2)) {
         double x1 = p1.x_, y1 = p1.y_, x2 = p2.x_, y2 = p2.y_;
         if (Math.abs(x1 - x2) < 2.0) {
            virtical_ = true;
            x1 = x2 = (x1 + x2) / 2.0;
         } else {
            virtical_ = false;
            y1 = y2 = (y1 + y2) / 2.0;
         }
         maxX = Math.max(x1, x2);
         minX = Math.min(x1, x2);
         maxY = Math.max(y1, y2);
         minY = Math.min(y1, y2);

         start = new BoxPoint(minX, minY, 0);
         end = new BoxPoint(maxX, maxY, 0);
         setLine(minX, minY, maxX, maxY);
      }
   }

   public void extend(BoxPoint p) {
      if (virtical_ && Math.abs(minX - p.x_) < 2.0) {
         if (p.y_ > maxY) {
            maxY = p.y_;
         } else if (p.y_ < minY) {
            minY = p.y_;
         }
         minX = maxX = (minX + maxX + p.x_) / 3.0;

         start = new BoxPoint(minX, minY, 0);
         end = new BoxPoint(maxX, maxY, 0);
         setLine(minX, minY, maxX, maxY);
      } else if (!virtical_ && Math.abs(minY - p.y_) < 2.0) {
         if (p.x_ > maxX) {
            maxX = p.x_;
         } else if (p.x_ < minX) {
            minX = p.x_;
         }
         minY = maxY = (minY + maxY + p.y_) / 3.0;

         start = new BoxPoint(minX, minY, 0);
         end = new BoxPoint(maxX, maxY, 0);
         setLine(minX, minY, maxX, maxY);
      }
   }

   public boolean extend(BoxEdge e) {
      if (virtical_ && e.virtical_ && Math.abs(minX - e.minX) < 2.0) {
         minX = Math.min(minX, e.minX);
         maxX = Math.max(maxX, e.maxX);
         minY = Math.min(minY, e.minY);
         maxY = Math.max(maxY, e.maxY);

         minX = maxX = (minX + maxX) / 2.0;

         start = new BoxPoint(minX, minY, 0);
         end = new BoxPoint(maxX, maxY, 0);
         setLine(minX, minY, maxX, maxY);
         return true;
      } else if (!virtical_ && !e.virtical_ && Math.abs(minY - e.minY) < 2.0) {
         minX = Math.min(minX, e.minX);
         maxX = Math.max(maxX, e.maxX);
         minY = Math.min(minY, e.minY);
         maxY = Math.max(maxY, e.maxY);

         minY = maxY = (minY + maxY) / 2.0;

         start = new BoxPoint(minX, minY, 0);
         end = new BoxPoint(maxX, maxY, 0);
         setLine(minX, minY, maxX, maxY);
         return true;
      }
      return false;
   }

   public boolean near(BoxPoint p) {
      return p != null && (start.near(p) || end.near(p));
   }

   @Override
   public boolean contains(double x, double y) {
      if (virtical_) {
         return (Math.abs(minX - x) < 2.0 && y < maxY && y > minY);
      } else {
         return (Math.abs(minY - y) < 2.0 && x < maxX && x > minX);
      }
   }

   public boolean contains(BoxPoint p) {
      return contains(p.x_, p.y_);
   }

}