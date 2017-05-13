package deltasquad.object;

import java.awt.geom.Line2D;

public abstract class BoxEdge extends Line2D.Double {
   private static final long  serialVersionUID = -2461145688771462782L;

   public static final double WIDTH_TOLERANCE  = 1.0;
   public static final double SHADOW_LENGTH    = 14.0;

   private BoxPoint           start_;
   private BoxPoint           end_;
   protected Line2D           shadowLine_;

   public static BoxEdge createEdge(BoxPoint p1, BoxPoint p2, boolean minEnded, boolean maxEnded) {
      if (p1.near(p2)) {
         boolean virtical = false;
         boolean horizontal = false;

         if (Math.abs(p1.x_ - p2.x_) < WIDTH_TOLERANCE) {
            virtical = true;
         }
         if (Math.abs(p1.y_ - p2.y_) < WIDTH_TOLERANCE) {
            horizontal = true;
         }
         if (virtical && !horizontal) {
            return new Virtical(p1, p2, minEnded, maxEnded);
         }
         if (horizontal && !virtical) {
            return new Horizontal(p1, p2, minEnded, maxEnded);
         }
      }
      return null;
   }

   public Line2D getLineShadow() {
      return shadowLine_;
   }

   public abstract Line2D getLineShadow(double buffer);

   public abstract boolean extend(BoxPoint p, boolean ended);

   public abstract boolean extend(BoxEdge e);

   public boolean near(BoxPoint p) {
      return p != null && (start_.near(p) || end_.near(p));
   }

   @Override
   public abstract boolean contains(double x, double y);

   public boolean contains(BoxPoint p) {
      return contains(p.x_, p.y_);
   }

   @Override
   public void setLine(double x1, double y1, double x2, double y2) {
      start_ = new BoxPoint(x1, y1, 0);
      end_ = new BoxPoint(x2, y2, 0);
      super.setLine(x1, y1, x2, y2);
   }

   public boolean inside(BoxEdge edge) {
      return contains(edge.start_) && contains(edge.end_);
   }

   private static class Virtical extends BoxEdge {
      private static final long serialVersionUID = -2461145688771462783L;

      private double            x_;
      private boolean           shadow_          = false;                // false = left, true = right

      private double            minY_;
      private double            maxY_;

      private boolean           minEnded_        = false;
      private boolean           maxEnded_        = false;

      public Virtical(BoxPoint p1, BoxPoint p2, boolean minEnded, boolean maxEnded) {
         x_ = (p1.x_ + p2.x_) / 2.0;
         shadow_ = p1.sightAngle_ < 0;

         minY_ = Math.min(p1.y_, p2.y_);
         maxY_ = Math.max(p1.y_, p2.y_);

         minEnded_ = minEnded;
         maxEnded_ = maxEnded;

         setLine(x_, minY_, x_, maxY_);
         shadowLine_ = getLineShadow(SHADOW_LENGTH);
      }

      @Override
      public Line2D getLineShadow(double buffer) {
         double dx = (shadow_ ? 1 : -1) * buffer;
         return new Line2D.Double(x_ + dx, minY_ - buffer, x_ + dx, maxY_ + buffer);
      }

      @Override
      public boolean extend(BoxPoint p, boolean ended) {
         boolean extended = false;
         if (this.near(p) && Math.abs(x_ - p.x_) < WIDTH_TOLERANCE) {
            if (!maxEnded_ && p.y_ > maxY_) {
               maxY_ = p.y_;
               maxEnded_ = ended;
               extended = true;
            } else if (!minEnded_ && p.y_ < minY_) {
               minY_ = p.y_;
               minEnded_ = ended;
               extended = true;
            }

            if (extended) {
               x_ = (x_ + p.x_) / 2.0;
               setLine(x_, minY_, x_, maxY_);
               shadowLine_ = getLineShadow(SHADOW_LENGTH);
            }
         }
         return extended;
      }

      @Override
      public boolean extend(BoxEdge edge) {
         boolean extended = false;
         if (edge instanceof Virtical) {
            Virtical virt = (Virtical) edge;
            if (Math.abs(x_ - virt.x_) < WIDTH_TOLERANCE) {
               if (!minEnded_ && virt.minY_ < minY_) {
                  minY_ = virt.minY_;
                  minEnded_ = virt.minEnded_;
                  extended = true;
               }
               if (!maxEnded_ && virt.maxY_ > maxY_) {
                  maxY_ = virt.maxY_;
                  maxEnded_ = virt.maxEnded_;
                  extended = true;
               }

               if (extended) {
                  x_ = (x_ + virt.x_) / 2.0;
                  setLine(x_, minY_, x_, maxY_);
                  shadowLine_ = getLineShadow(SHADOW_LENGTH);
               }
            }
         }
         return extended;
      }

      @Override
      public boolean contains(double x, double y) {
         return (Math.abs(x_ - x) < 2.0 * WIDTH_TOLERANCE && y <= maxY_ && y >= minY_);
      }

   }

   private static class Horizontal extends BoxEdge {
      private static final long serialVersionUID = -2461145688771462784L;

      private double            y_;
      private boolean           shadow_          = false;                // false = bottom, true = top

      private double            minX_;
      private double            maxX_;

      private boolean           minEnded_        = false;
      private boolean           maxEnded_        = false;

      public Horizontal(BoxPoint p1, BoxPoint p2, boolean minEnded, boolean maxEnded) {
         y_ = (p1.y_ + p2.y_) / 2.0;
         shadow_ = Math.abs(p1.sightAngle_) > 90.0;

         minX_ = Math.min(p1.x_, p2.x_);
         maxX_ = Math.max(p1.x_, p2.x_);

         minEnded_ = minEnded;
         maxEnded_ = maxEnded;

         setLine(minX_, y_, maxX_, y_);
         shadowLine_ = getLineShadow(SHADOW_LENGTH);
      }

      @Override
      public Line2D getLineShadow(double buffer) {
         double dy = (shadow_ ? 1 : -1) * buffer;
         return new Line2D.Double(minX_ - buffer, y_ + dy, maxX_ + buffer, y_ + dy);
      }

      @Override
      public boolean extend(BoxPoint p, boolean ended) {
         boolean extended = false;
         if (this.near(p) && Math.abs(y_ - p.y_) < WIDTH_TOLERANCE) {
            if (!maxEnded_ && p.x_ > maxX_) {
               maxX_ = p.x_;
               maxEnded_ = ended;
               extended = true;
            } else if (!minEnded_ && p.x_ < minX_) {
               minX_ = p.x_;
               minEnded_ = ended;
               extended = true;
            }

            if (extended) {
               y_ = (y_ + p.y_) / 2.0;
               setLine(minX_, y_, maxX_, y_);
               shadowLine_ = getLineShadow(SHADOW_LENGTH);
            }
         }
         return extended;
      }

      @Override
      public boolean extend(BoxEdge edge) {
         boolean extended = false;
         if (edge instanceof Horizontal) {
            Horizontal horz = (Horizontal) edge;
            if (Math.abs(y_ - horz.y_) < WIDTH_TOLERANCE) {
               if (!minEnded_ && horz.minX_ < minX_) {
                  minX_ = horz.minX_;
                  minEnded_ = horz.minEnded_;
                  extended = true;
               }
               if (!maxEnded_ && horz.maxX_ > maxX_) {
                  maxX_ = horz.maxX_;
                  maxEnded_ = horz.maxEnded_;
                  extended = true;
               }

               if (extended) {
                  y_ = (y_ + horz.y_) / 2.0;
                  setLine(minX_, y_, maxX_, y_);
                  shadowLine_ = getLineShadow(SHADOW_LENGTH);
               }
            }
         }
         return extended;
      }

      @Override
      public boolean contains(double x, double y) {
         return (Math.abs(y_ - y) < WIDTH_TOLERANCE && x <= maxX_ && x >= minX_);
      }
   }

}