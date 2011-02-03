package kid.management;

import java.awt.Color;
import java.awt.geom.Line2D;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.ListIterator;

import kid.data.Drawable;
import kid.graphics.RGraphics;
import kid.utils.Utils;
import robocode.AdvancedRobot;
import robocode.Condition;
import robocode.Event;
import robocode.HitObjectEvent;
import robocode.HitObstacleEvent;
import robocode.MessageEvent;
import robocode.ScannedObjectEvent;
import robocode.TeamRobot;

public class ObjectManager implements Drawable {

   private AdvancedRobot                  robot_;
   // private RobotInfo info_;
   private double                         prevRadarHeading_;
   private double                         curRadarHeading_;

   private long                           eventProcessingTime_;
   private LinkedList<ScannedObjectEvent> currentEvents_;

   private static LinkedList<Point>       points_  = new LinkedList<ObjectManager.Point>();
   private static LinkedList<Corner>      corners_ = new LinkedList<ObjectManager.Corner>();
   private static LinkedList<Edge>        edges_   = new LinkedList<ObjectManager.Edge>();

   public ObjectManager(AdvancedRobot robot) {
      robot_ = robot;
      // info_ = new RobotInfo(robot);

      robot.addCustomEvent(new Condition() {
         @Override
         public boolean test() {
            prevRadarHeading_ = curRadarHeading_;
            curRadarHeading_ = robot_.getRadarHeading();
            return false;
         }
      });
      prevRadarHeading_ = robot_.getRadarHeading();
      curRadarHeading_ = robot_.getRadarHeading();

      eventProcessingTime_ = -1;
      currentEvents_ = new LinkedList<ScannedObjectEvent>();
   }

   public LinkedList<Point> getPoints() {
      return points_;
   }

   public LinkedList<Corner> getCorners() {
      return corners_;
   }

   public LinkedList<Edge> getEdges() {
      return edges_;
   }

   public boolean blocked(Line2D path) {
      if (path != null) {
         for (Edge e : edges_) {
            if (e.intersectsLine(path)) {
               return true;
            }
         }
      }
      return false;
   }

   private void send(Point p) {
      if (robot_ instanceof TeamRobot) {
         try {
            ((TeamRobot) robot_).broadcastMessage(p);
         } catch (IOException e) {
            System.err.println("BROADCAST ERROR: could not send point to teammates");
            e.printStackTrace();
         }
      }
   }

   private void add(Point point) {
      LinkedList<Point> nearPoints = new LinkedList<Point>();
      boolean contains = false;
      ListIterator<Point> iter = points_.listIterator();
      while (!contains && iter.hasNext()) {
         Point next = iter.next();
         if (next.equals(point)) {
            contains = true;
         } else if (next.near(point)) {
            nearPoints.add(next);
         }
      }
      if (!contains) {
         points_.add(point);
         send(point);

         for (Corner c : corners_) {
            if (c.near(point)) {
               nearPoints.add(c);
            }
         }

         for (Point p : nearPoints) {
            point.nearPoints_.add(p);
            p.nearPoints_.add(point);
            Edge edge = new Edge(point, p);
            edges_.add(edge);
         }
      }
   }

   private void send(Corner c) {
      if (robot_ instanceof TeamRobot) {
         try {
            ((TeamRobot) robot_).broadcastMessage(c);
         } catch (IOException e) {
            System.err.println("BROADCAST ERROR: could not send corner to teammates");
            e.printStackTrace();
         }
      }
   }

   private void add(Corner corner) {
      LinkedList<Point> nearPoints = new LinkedList<Point>();
      boolean contains = false;
      ListIterator<Corner> iter = corners_.listIterator();
      while (!contains && iter.hasNext()) {
         Corner next = iter.next();
         if (next.equals(corner)) {
            contains = true;
         } else if (next.near(corner)) {
            nearPoints.add(next);
         }
      }
      if (!contains) {
         corners_.add(corner);
         send(corner);

         for (Point p : points_) {
            if (p.near(corner)) {
               nearPoints.add(p);
            }
         }

         for (Point p : nearPoints) {
            corner.nearPoints_.add(p);
            p.nearPoints_.add(corner);
            Edge edge = new Edge(corner, p);
            edges_.add(edge);
         }
      }
   }

   public void inEvent(Event e) {
      if (e instanceof HitObstacleEvent) {
         handleHitObstacle((HitObstacleEvent) e);
      } else if (e instanceof HitObjectEvent) {
         handleHitObject((HitObjectEvent) e);
      } else if (e instanceof ScannedObjectEvent) {
         handleScannedObject((ScannedObjectEvent) e);
      } else if (e instanceof MessageEvent) {
         handleMessage((MessageEvent) e);
      }
   }

   private void handleHitObstacle(HitObstacleEvent e) {
      // System.out.println("Hit Obstacle: " + e.getObstacleType());
   }

   private void handleHitObject(HitObjectEvent e) {
      // System.out.println("Hit Object: " + e.getType());
   }

   private void handleScannedObject(ScannedObjectEvent e) {
      // System.out.println("Scanned Object: " + e.getObjectType());
      if (eventProcessingTime_ != robot_.getTime()) {
         currentEvents_ = new LinkedList<ScannedObjectEvent>();
         eventProcessingTime_ = robot_.getTime();
      }

      if (e.getObjectType().equals("box")) {
         double angle = e.getBearing() + robot_.getHeading();
         double x = Utils.getX(robot_.getX(), e.getDistance(), angle);
         double y = Utils.getY(robot_.getY(), e.getDistance(), angle);

         boolean between = Math.abs(Utils.relative(angle - prevRadarHeading_)) > 1.0
               && Math.abs(Utils.relative(angle - curRadarHeading_)) > 1.0;
         boolean straight = Math.abs(Math.round(angle)) % 90 == 0;
         boolean blocked = currentEvents_.size() > 0;


         if (between && !straight && !blocked) {
            Corner corner = new Corner(x, y, angle);
            add(corner);
         } else {
            Point point = new Point(x, y, angle);
            add(point);
         }
      }

      currentEvents_.add(e);
   }

   private void handleMessage(MessageEvent e) {
      Serializable message = e.getMessage();
      if (message instanceof Point) {
         Point p = (Point) message;
         add(p);
      } else if (message instanceof Corner) {
         Corner c = (Corner) message;
         add(c);
      }
   }


   public static class Point implements Serializable {
      private static final long serialVersionUID = -2237448984421612107L;
      public double             x_;
      public double             y_;
      public double             sightAngle_;

      public LinkedList<Point>  nearPoints_;

      public Point(double x, double y, double sightAngle) {
         x_ = x;
         y_ = y;
         sightAngle_ = sightAngle;
         nearPoints_ = new LinkedList<ObjectManager.Point>();
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

   public static class Corner extends Point implements Serializable {
      private static final long serialVersionUID = 5518549448487386606L;

      public Corner(double x, double y, double sightAngle) {
         super(x, y, sightAngle);
      }

      public boolean equals(Corner corner) {
         return (Math.abs(x_ - corner.x_) < 18.0 && Math.abs(y_ - corner.y_) < 18.0);
      }

      @Override
      public boolean equals(Object obj) {
         if (obj instanceof Corner) {
            return equals((Corner) obj);
         }
         return false;
      }
   }

   public static class Edge extends Line2D.Double {
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

   @Override
   public void draw(RGraphics grid) {
      // System.out.println("Points: " + points_.size() + " Edges: " + edges_.size() + " Corners: " + corners_.size());
      grid.setColor(Color.BLUE);
      for (Edge e : edges_) {
         grid.drawLine(e.getX1(), e.getY1(), e.getX2(), e.getY2());
      }
      grid.setColor(Color.GREEN);
      for (Point e : points_) {
         grid.fillOvalCenter(e.x_, e.y_, 4, 4);
      }
      grid.setColor(Color.RED);
      for (Corner c : corners_) {
         grid.fillOvalCenter(c.x_, c.y_, 6, 6);
      }
   }
}
