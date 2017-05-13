package deltasquad;

import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;

import robocode.Event;
import robocode.HitObstacleEvent;
import CTFApi.CaptureTheFlagApi;
import deltasquad.data.RobotChooser;
import deltasquad.graphics.RGraphics;
import deltasquad.info.RobotInfo;
import deltasquad.movement.robot.RobotMovement;
import deltasquad.object.ObjectManager;
import deltasquad.robot.RobotData;
import deltasquad.robot.TeammateData;
import deltasquad.utils.Trig;
import deltasquad.utils.Utils;
import deltasquad.virtual.VirtualBullet;

public class MinimumRiskPoint {

   protected CaptureTheFlagApi robot;
   protected RobotInfo         info;
   protected RobotMovement     movement;
   protected ObjectManager     objects;

   protected Point2D           nextPosition;
   protected Point2D           oldPosition;

   protected RoundRectangle2D  battleField;
   protected final double      walldist                = 20;
   protected final double      cornerarc               = 50;

   protected final int         NUM_OF_GENERATED_POINTS = 20;
   protected final int         CORNER_RISK             = 2;
   protected final int         BOT_RISK                = 100;

   public MinimumRiskPoint(CaptureTheFlagApi myRobot, ObjectManager objectManager) {
      this.robot = myRobot;
      this.objects = objectManager;
      this.movement = new RobotMovement(myRobot);
      this.info = new RobotInfo(myRobot);
      nextPosition = new Point2D.Double(robot.getX(), robot.getY());
      battleField = new RoundRectangle2D.Double(walldist, walldist, robot.getBattleFieldWidth() - 2 * walldist,
            robot.getBattleFieldHeight() - 2 * walldist, cornerarc, cornerarc);
   }

   public void move(RobotData[] robots, VirtualBullet[] teammateBullets) {
      movement.setMoveToPoint(getPoint(robots, teammateBullets));

      RGraphics grid = new RGraphics(robot.getGraphics(), robot);
      grid.setColor(Color.BLUE);
      if (oldPosition != null)
         grid.fillOvalCenter(oldPosition.getX(), oldPosition.getY(), 6, 6);
      grid.setColor(Color.RED);
      grid.fillOvalCenter(nextPosition.getX(), nextPosition.getY(), 6, 6);
   }

   public void inEvent(Event e) {
      if (e instanceof HitObstacleEvent) {
         oldPosition = nextPosition;
      }
   }

   public Point2D getPoint(RobotData[] robots, VirtualBullet[] teammateBullets) {
      double myX = info.getX();
      double myY = info.getY();

      double minDist = Utils.sqrt(distSq(robots));

      double pointDist = Utils.distSq(nextPosition, myX, myY);
      double pointRisk = risk(nextPosition, info.angle(nextPosition), robots, teammateBullets);
      if (pointDist < Utils.sqr(20)) {
         oldPosition = nextPosition; // new Point2D.Double(info.getX(), info.getY());
      }

      double dist = Utils.random(minDist / 2, minDist);

      for (double a = 0; a < Utils.CIRCLE; a += Utils.CIRCLE / NUM_OF_GENERATED_POINTS) {
         double angle = Utils.random(a, a + Utils.CIRCLE / NUM_OF_GENERATED_POINTS);
         Point2D point = Utils.getPoint(myX, myY, dist, angle);

         if (battleField.contains(point)) {
            double risk = risk(point, angle, robots, teammateBullets);
            if (risk < pointRisk) {
               // oldPosition = new Point2D.Double(info.getX(), info.getY());
               nextPosition = point;
               pointRisk = risk;
            }
         }

      }

      return nextPosition;
   }

   public double distSq(RobotData[] robots) {
      // double myX = info.getX();
      // double myY = info.getY();
      double minDist = // Utils.sqr(300);
      info.distSq(RobotChooser.CLOSEST.getRobot(robot, Arrays.asList(robots)));
      // for (RobotData r : robots) {
      // Line2D path = new Line2D.Double(myX, myY, r.getX(), r.getY());
      // if (!objects.blocked(path)) {
      // minDist = Math.min(2 * info.distSq(r), minDist);
      // }
      // }

      // for (VirtualBullet b : teammateBullets)
      // minDist = Math.min(2 * Utils.distSq(myX, myY, b.getX(time),
      // b.getY(time)), minDist);

      return minDist;
   }

   public double risk(Point2D point, double angle, RobotData[] robots, VirtualBullet[] teammateBullets) {
      double myX = info.getX();
      double myY = info.getY();
      long time = info.getTime();
      double pointRisk = 0.0;
      Line2D path = new Line2D.Double(myX, myY, point.getX(), point.getY());

      // double addD = 22;
      // Rectangle2D destination = new Rectangle2D.Double(point.getX() - addD, point.getY() - addD, 2 * addD, 2 * addD);

      // double addM = 16;
      // Rectangle2D me = new Rectangle2D.Double(myX - addM, myY - addM, 2 * addM, 2 * addM);


      if (objects.blockedShadow(path)) {
         return Double.POSITIVE_INFINITY;
      }


      for (RobotData r : robots) {
         if (!r.isDead()) {
            // Line2D sight = new Line2D.Double(point.getX(), point.getY(), r.getX(), r.getY());
            // if (objects.blocked(sight)) {
            // continue;
            // }
            boolean intersects = path.intersects(r.getRectangle());

            if (intersects) {
               return Double.POSITIVE_INFINITY;
            } else {
               double robotRisk = BOT_RISK;
               if (!(r instanceof TeammateData)) {
                  robotRisk += r.getEnergy();
                  robotRisk *= (1 + Math.abs(Trig.t_cos(angle - Utils.angle(myX, myY, r.getX(), r.getY()))));
                  // if (r.getTime() + 10 < time) {
                  // robotRisk *= (r.getTime() + 10 - time);
                  // }
               }

               robotRisk /= Utils.distSq(point, r.getX(), r.getY());
               pointRisk += robotRisk;
            }
         }
      }


      if (oldPosition != null) {
         double oldPointRisk = 200.0;
         oldPointRisk /= point.distanceSq(oldPosition);
         pointRisk += oldPointRisk;
      }


      for (VirtualBullet b : teammateBullets) {
         double bulletRisk = 10.0D;
         double heading = b.getHeading();
         double angleBullet = Utils.angle(myX, myY, b.getX(time), b.getY(time));
         if (Math.abs(angleBullet - heading) < Utils.maxEscapeAngle(b.getVelocity())) {
            bulletRisk /= Utils.sqr(Utils.distSq(point, b.getX(time), b.getY(time)));
            pointRisk += bulletRisk;
         }
      }

      pointRisk += info.getOthers()
            / Utils.distSq(point, info.getBattleFieldWidth() / 2, info.getBattleFieldHeight() / 2);
      pointRisk += CORNER_RISK / Utils.distSq(point, info.getBattleFieldWidth(), info.getBattleFieldHeight());
      pointRisk += CORNER_RISK / Utils.distSq(point, 0.0D, info.getBattleFieldHeight());
      pointRisk += CORNER_RISK / Utils.distSq(point, 0.0D, 0.0D);
      pointRisk += CORNER_RISK / Utils.distSq(point, info.getBattleFieldWidth(), 0.0D);



      // RGraphics grid = new RGraphics(robot.getGraphics(), robot);

      // grid.setColor(Color.RED);
      // if (!objects.blocked(path))
      // grid.setColor(Color.GREEN);
      // grid.draw(path);

      // grid.draw(cornerPaths[0]);
      // grid.draw(cornerPaths[1]);
      // grid.draw(cornerPaths[2]);
      // grid.draw(cornerPaths[3]);

      // grid.setColor(Color.RED);
      // if (!objects.blocked(destination))
      // grid.setColor(Color.GREEN);
      // grid.draw(destination);

      // grid.setColor(Color.RED);
      // if (!objects.blocked(me))
      // grid.setColor(Color.GREEN);
      // grid.draw(me);

      return pointRisk;
   }

}
