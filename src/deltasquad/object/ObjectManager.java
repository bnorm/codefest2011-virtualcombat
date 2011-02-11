package deltasquad.object;

import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.ListIterator;

import deltasquad.data.Drawable;
import deltasquad.graphics.RGraphics;
import deltasquad.utils.Utils;

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

   private static LinkedList<Point>       points_  = new LinkedList<Point>();
   private static LinkedList<Corner>      corners_ = new LinkedList<Corner>();
   private static LinkedList<Edge>        edges_   = new LinkedList<Edge>();

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

   public boolean blocked(Rectangle2D robot) {
      if (robot != null) {
         for (Edge e : edges_) {
            if (e.intersects(robot)) {
               return true;
            }
         }
      }
      return false;
   }

   private void send(Serializable s) {
      if (robot_ instanceof TeamRobot) {
         try {
            ((TeamRobot) robot_).broadcastMessage(s);
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
      System.out.println("Hit Obstacle: " + e.getObstacleType());
      double angle = Utils.relative(robot_.getHeading() + e.getBearing());
      double dist = 18;
      // System.out.println("From: " + angle + " To: " + Utils.normalize(angle, -45, 45));
      double x = Utils.getX(robot_.getX(), dist, angle);
      double y = Utils.getY(robot_.getY(), dist, angle);

      Point point = new Point(x, y, angle);
      add(point);
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
