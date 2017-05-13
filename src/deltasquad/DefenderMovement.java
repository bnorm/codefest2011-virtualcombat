package deltasquad;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;

import robocode.Event;
import robocode.HitObjectEvent;
import robocode.MessageEvent;
import CTFApi.CaptureTheFlagApi;
import deltasquad.object.ObjectManager;
import deltasquad.robot.RobotData;
import deltasquad.robot.TeammateData;
import deltasquad.utils.Trig;
import deltasquad.utils.Utils;
import deltasquad.virtual.VirtualBullet;

public class DefenderMovement extends MinimumRiskPoint {

   boolean             captured_        = false;
   static final String CAPTURED_MESSAGE = "CAPTURED_OWN_FALG";

   public DefenderMovement(CaptureTheFlagApi myRobot, ObjectManager objectManager) {
      super(myRobot, objectManager);
   }

   @Override
   public double distSq(RobotData[] robots) {
      double x = robot.getOwnFlag().getX();
      double y = robot.getOwnFlag().getY();

      Line2D path = new Line2D.Double(info.getX(), info.getY(), x, y);

      if (captured_ || robot.isOwnFlagAtBase() || objects.blocked(path)) {
         return super.distSq(robots);
      } else {
         return Math.min(super.distSq(robots), info.distSq(x, y));
      }
   }

   @Override
   public double risk(Point2D point, double angle, RobotData[] robots, VirtualBullet[] teammateBullets) {
      double risk = 0.0;
      // if (captured_) {
      // x = robot.getOwnBase().getCenterX();
      // y = robot.getOwnBase().getCenterY();
      // }
      // double myX = info.getX();
      // double myY = info.getY();
      // risk += 100 / dist;
      // risk += -0.5;

      Line2D sight = new Line2D.Double(point, robot.getEnemyFlag());
      if (!captured_ && !robot.isOwnFlagAtBase() && !objects.blocked(sight)) {
         risk += -1000 / point.distanceSq(robot.getOwnFlag());
         risk += -1000 / point.distanceSq(robot.getOwnBase().getCenterX(), robot.getOwnBase().getCenterY());
      } else {
         double x = robot.getEnemyBase().getCenterX();
         double y = robot.getEnemyBase().getCenterY();

         double dist = point.distance(x, y);
         risk += Utils.sqr(Utils.sqr((dist - 300) / 200));
      }
      risk += 100 / point.distanceSq(robot.getEnemyFlag());

      double myX = info.getX();
      double myY = info.getY();
      long time = info.getTime();
      Line2D path = new Line2D.Double(myX, myY, point.getX(), point.getY());

      Line2D[] cornerPaths = new Line2D.Double[4];
      double addM = 16;
      double addD = 22;
      cornerPaths[0] = new Line2D.Double(myX - addM, myY + addM, point.getX() - addD, point.getY() + addD);
      cornerPaths[1] = new Line2D.Double(myX + addM, myY + addM, point.getX() + addD, point.getY() + addD);
      cornerPaths[2] = new Line2D.Double(myX + addM, myY - addM, point.getX() + addD, point.getY() - addD);
      cornerPaths[3] = new Line2D.Double(myX - addM, myY - addM, point.getX() - addD, point.getY() - addD);
      Rectangle2D destination = new Rectangle2D.Double(point.getX() - addD, point.getY() - addD, 2 * addD, 2 * addD);
      Rectangle2D me = new Rectangle2D.Double(myX - addM, myY - addM, 2 * addM, 2 * addM);


      if (objects.blocked(destination) || objects.blocked(path)) {
         return Double.POSITIVE_INFINITY;
      } else {
         if (!objects.blocked(me)) {
            for (int i = 0; i < 4; i++) {
               if (objects.blocked(cornerPaths[i]))
                  return Double.POSITIVE_INFINITY;
            }
         }
      }


      for (RobotData r : robots) {
         if (!r.isDead()) {
            // Line2D sight = new Line2D.Double(point.getX(), point.getY(), r.getX(), r.getY());
            // if (objects.blocked(sight)) {
            // continue;
            // }

            double robotRisk = BOT_RISK;
            if (!(r instanceof TeammateData)) {
               robotRisk += r.getEnergy();
               robotRisk *= (1 + Math.abs(Trig.d_cos(angle - Utils.angle(myX, myY, r.getX(), r.getY()))));
               // if (r.getTime() + 10 < time) {
               // robotRisk *= (r.getTime() + 10 - time);
               // }
            } else {
               boolean intersects = path.intersects(r.getRectangle());
               for (int i = 0; !intersects && i < 4; i++) {
                  intersects = cornerPaths[i].intersects(r.getRectangle());
               }
               if (intersects) {
                  return Double.POSITIVE_INFINITY;
               }
            }

            robotRisk /= Utils.distSq(point, r.getX(), r.getY());
            risk += robotRisk;
         }
      }


      if (oldPosition != null) {
         double oldPointRisk = 200.0;
         oldPointRisk /= point.distanceSq(oldPosition);
         risk += oldPointRisk;
      }


      for (VirtualBullet b : teammateBullets) {
         double bulletRisk = 10.0D;
         double heading = b.getHeading();
         double angleBullet = Utils.angle(myX, myY, b.getX(time), b.getY(time));
         if (Math.abs(angleBullet - heading) < Utils.maxEscapeAngle(b.getVelocity())) {
            bulletRisk /= Utils.sqr(Utils.distSq(point, b.getX(time), b.getY(time)));
            risk += bulletRisk;
         }
      }

      risk += info.getOthers() / Utils.distSq(point, info.getBattleFieldWidth() / 2, info.getBattleFieldHeight() / 2);
      risk += CORNER_RISK / Utils.distSq(point, info.getBattleFieldWidth(), info.getBattleFieldHeight());
      risk += CORNER_RISK / Utils.distSq(point, 0.0D, info.getBattleFieldHeight());
      risk += CORNER_RISK / Utils.distSq(point, 0.0D, 0.0D);
      risk += CORNER_RISK / Utils.distSq(point, info.getBattleFieldWidth(), 0.0D);



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

      return risk;
   }

   @Override
   public void inEvent(Event e) {
      if (e instanceof HitObjectEvent) {
         HitObjectEvent hoe = (HitObjectEvent) e;
         if (hoe.getType().equals("flag") && info.distSq(robot.getOwnFlag()) < Utils.sqr(50)) {
            captured_ = true;
            try {
               robot.broadcastMessage(CAPTURED_MESSAGE);
            } catch (IOException e1) {
            }
         }
      } else if (e instanceof MessageEvent) {
         MessageEvent me = (MessageEvent) e;
         if (CAPTURED_MESSAGE.equals(me.getMessage())) {
            captured_ = true;
         }
      }
      super.inEvent(e);
   }
}
