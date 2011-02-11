package deltasquad.object;

import java.awt.geom.Line2D;


public class Edge extends Line2D.Double {
   private static final long serialVersionUID = -2461145688771462782L;

   public Edge(double x1, double y1, double x2, double y2) {
      super(x1, y1, x2, y2);
   }

   public Edge(Point p1, Point p2) {
      super(p1.x_, p1.y_, p2.x_, p2.y_);
   }

   // @Override
   // public boolean contains(double x, double y) {
   // if (getX1() == getX2() && Math.abs(getX1() - x) < 1.0) {
   // return true;
   // } else if (getY1() == getY2() && Math.abs(getY1() - y) < 1.0) {
   // return true;
   // }
   // return super.contains(x, y);
   // }

}