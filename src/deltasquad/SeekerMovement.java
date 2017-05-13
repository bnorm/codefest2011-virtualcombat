package deltasquad;


import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.LinkedList;
import java.util.ListIterator;

import robocode.Event;
import robocode.HitObjectEvent;
import CTFApi.CaptureTheFlagApi;
import deltasquad.graphics.RGraphics;
import deltasquad.object.ObjectManager;
import deltasquad.robot.RobotData;
import deltasquad.utils.Utils;
import deltasquad.virtual.VirtualBullet;


public class SeekerMovement extends MinimumRiskPoint {

   public boolean captured_ = false;

   public SeekerMovement(CaptureTheFlagApi myRobot, ObjectManager objectManager) {
      super(myRobot, objectManager);
   }

   @Override
   public void move(RobotData[] robots, VirtualBullet[] teammateBullets) {
      double gx = robot.getEnemyFlag().getX();
      double gy = robot.getEnemyFlag().getY();
      if (captured_) {
         gx = robot.getOwnBase().getCenterX();
         gy = robot.getOwnBase().getCenterY();
      }
      double mx = robot.getX();
      double my = robot.getY();

      // LinkedList<Node> open = new LinkedList<Node>();
      //
      // for (Point2D p : objects.getShadowPoints(20)) {
      //
      // }



      Line2D path = new Line2D.Double(mx, my, gx, gy);
      LinkedList<Line2D> blockers = objects.blockingShadows(path);
      LinkedList<LineDist> safe = findSafeLines(blockers, path);


      if (safe.size() > 0) {
         movement.setMoveToXY(safe.getFirst().line.getX2(), safe.getFirst().line.getY2());
      }

      // super.move(robots, teammateBullets);
   }

   public LinkedList<LineDist> findSafeLines(LinkedList<Line2D> blockers, Line2D path) {
      RGraphics grid = new RGraphics(robot.getGraphics(), robot);
      LinkedList<LineDist> safe = new LinkedList<LineDist>();

      if (blockers.size() == 0) {
         grid.setColor(Color.GREEN);
         grid.draw(path);
         safe.add(new LineDist(path, 0));
      } else {
         grid.setColor(Color.RED);
         grid.draw(path);

         double mx = path.getX1();
         double my = path.getY1();
         double gx = path.getX2();
         double gy = path.getY2();

         LinkedList<PointDist> points = new LinkedList<PointDist>();
         for (Line2D l : blockers) {
            ListIterator<PointDist> iter = points.listIterator();
            PointDist p1 = new PointDist(l.getP1(), gx, gy);

            while (iter.hasNext()) {
               PointDist next = iter.next();
               if (next.distSq > p1.distSq) {
                  iter.previous();
                  break;
               }
            }
            iter.add(p1);

            iter = points.listIterator();
            PointDist p2 = new PointDist(l.getP2(), gx, gx);
            while (iter.hasNext()) {
               PointDist next = iter.next();
               if (next.distSq > p2.distSq) {
                  iter.previous();
                  break;
               }
            }
            iter.add(p2);
         }

         LinkedList<LineDist> newPaths = new LinkedList<LineDist>();
         for (PointDist p : points) {
            Line2D l = new Line2D.Double(mx, my, p.point.getX(), p.point.getY());
            newPaths.add(new LineDist(l, p.distSq));

            grid.setColor(Color.YELLOW);
            grid.fillOvalCenter(p.point.getX(), p.point.getY(), 8, 8);
            grid.draw(l);
         }

         for (LineDist l : newPaths) {
            if (!objects.blockedShadow(l.line)) {
               grid.setColor(Color.GREEN);
               grid.draw(l.line);
               safe.add(l);
            } else {
               grid.setColor(Color.RED);
               grid.draw(l.line);
            }
         }

         if (safe.size() == 0) {

         }
      }
      return safe;
   }

   static class PointDist {
      Point2D point;
      double  distSq;

      public PointDist(Point2D point, double gx, double gy) {
         this.point = point;
         distSq = point.distanceSq(gx, gy);
      }
   }

   static class LineDist {
      Line2D line;
      double distSq;

      // public LineDist(Line2D line, double gx, double gy) {
      // this.line = line;
      // distSq = line.getP2().distanceSq(gx, gy);
      // }

      public LineDist(Line2D line, double distSq) {
         this.line = line;
         this.distSq = distSq;
      }
   }

   static class Node {
      Point2D          point;
      LinkedList<Node> path;
      double           f;

      public Node(Point2D point, double f, Node old) {
         this.point = point;
         this.path = new LinkedList<Node>(old.path);
         this.path.add(old);
         this.f = f;
      }

      public Node(Point2D point, double f) {
         this.point = point;
         this.path = new LinkedList<Node>();
         this.f = f;
      }
   }


   @Override
   public double distSq(RobotData[] robots) {
      double x = robot.getEnemyFlag().getX();
      double y = robot.getEnemyFlag().getY();
      if (captured_) {
         x = robot.getOwnBase().getCenterX();
         y = robot.getOwnBase().getCenterY();
      }
      double dist = super.distSq(robots);
      Line2D path = new Line2D.Double(robot.getX(), robot.getY(), x, y);
      if (!objects.blocked(path)) {
         // return Math.min(super.distSq(robots), 9 * info.distSq(x, y));
         // } else {
         return Math.min(dist, info.distSq(x, y));
      }
      return dist;
   }

   @Override
   public double risk(Point2D point, double angle, RobotData[] robots, VirtualBullet[] teammateBullets) {
      // double flag = -500;
      // double base = flag;
      double risk = -1000;
      double x = robot.getEnemyFlag().getX();
      double y = robot.getEnemyFlag().getY();

      if (captured_) {
         // risk = base;
         x = robot.getOwnBase().getCenterX();
         y = robot.getOwnBase().getCenterY();
      }
      risk /= point.distance(x, y);

      Line2D path = new Line2D.Double(point.getX(), point.getY(), x, y);
      if (objects.blocked(path)) {
         risk = -1000 / point.distanceSq(x, y);
      }
      return risk + super.risk(point, angle, robots, teammateBullets);
   }

   @Override
   public void inEvent(Event e) {
      if (e instanceof HitObjectEvent) {
         HitObjectEvent hoe = (HitObjectEvent) e;
         if (hoe.getType().equals("flag") && info.distSq(robot.getEnemyFlag()) < Utils.sqr(36)) {
            captured_ = true;
         }
      }
      super.inEvent(e);
   }

}