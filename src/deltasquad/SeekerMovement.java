package deltasquad;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import robocode.Event;
import robocode.HitObjectEvent;
import CTFApi.CaptureTheFlagApi;
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
      return risk + super.risk(point, angle, robots, teammateBullets) / 2;
   }

   @Override
   public void inEvent(Event e) {
      if (e instanceof HitObjectEvent) {
         HitObjectEvent hoe = (HitObjectEvent) e;
         if (hoe.getType().equals("flag") && info.distSq(robot.getEnemyFlag()) < Utils.sqr(30)) {
            captured_ = true;
         }
      }
      super.inEvent(e);
   }

}
