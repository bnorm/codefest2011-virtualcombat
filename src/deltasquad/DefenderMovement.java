package deltasquad;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.IOException;

import robocode.Event;
import robocode.HitObjectEvent;
import robocode.MessageEvent;
import CTFApi.CaptureTheFlagApi;
import deltasquad.object.ObjectManager;
import deltasquad.robot.RobotData;
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
      // double flag = -500;
      // double base = flag;
      double risk = 0;
      // if (captured_) {
      // x = robot.getOwnBase().getCenterX();
      // y = robot.getOwnBase().getCenterY();
      // }
      // double myX = info.getX();
      // double myY = info.getY();
      // risk += 100 / dist;
      // risk += -0.5;

      Line2D path = new Line2D.Double(point, robot.getEnemyFlag());
      if (!captured_ && !robot.isOwnFlagAtBase() && !objects.blocked(path)) {
         risk += -1000 / point.distanceSq(robot.getOwnFlag());
         risk += -1000 / point.distanceSq(robot.getOwnBase().getCenterX(), robot.getOwnBase().getCenterY());
      } else {
         double x = robot.getEnemyBase().getCenterX();
         double y = robot.getEnemyBase().getCenterY();

         double dist = point.distance(x, y);
         risk += Utils.sqr(Utils.sqr((dist - 300) / 200));
      }
      risk += 100 / point.distanceSq(robot.getEnemyFlag());

      return risk + super.risk(point, angle, robots, teammateBullets);
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
