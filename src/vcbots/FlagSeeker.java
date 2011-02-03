package vcbots;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import kid.graphics.RGraphics;
import kid.management.ObjectManager;

import robocode.HitByBulletEvent;
import robocode.HitObjectEvent;
import robocode.HitObstacleEvent;
import robocode.HitWallEvent;
import robocode.ScannedObjectEvent;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;
import CTFApi.CaptureTheFlagApi;

public class FlagSeeker extends CaptureTheFlagApi {

   /**
    * Note that CaptureTheFlagApi inherits TeamRobot, so users can directly use functions of TeamRobot.
    */

   String[]      myteam;

   Point2D       enemyFlag;
   Rectangle2D   homeBase;

   boolean       flagCaptured = false;

   Point2D       destination;
   int           skipGoTo     = 20;

   ObjectManager objectManager;

   public void run() {

      objectManager = new ObjectManager(this);

      /*
       * registerMe() needs to be called for every robot. Used for initialization.
       */
      registerMe();

      // write your logic here

      while (true) {
         UpdateBattlefieldState(getBattlefieldState());
         myteam = getTeammates();
         homeBase = getOwnBase();
         enemyFlag = getEnemyFlag();
         // System.out.println("UpdateBattlefieldState done");
         // System.out.println("enemyFlag::" + enemyFlag.toString());
         // System.out.println("UpdateBattlefieldState done");

         setTurnGunRight(Double.POSITIVE_INFINITY);

         if (!flagCaptured)
            destination = enemyFlag;
         else
            destination = new Point2D.Double(homeBase.getCenterX(), homeBase.getCenterY());

         goTo(destination);
         execute();
      }
   }

   @Override
   public void onPaint(Graphics2D g) {
      objectManager.draw(new RGraphics(g, this));

      g.setColor(Color.BLUE);
      if (homeBase != null)
         g.drawOval((int) (homeBase.getCenterX() - 5), (int) (homeBase.getCenterY() - 5), 10, 10);
      if (enemyFlag != null)
         g.drawOval((int) (enemyFlag.getX() - 5), (int) (enemyFlag.getY() - 5), 10, 10);
      g.setColor(Color.RED);
      if (destination != null)
         g.drawOval((int) (destination.getX() - 5), (int) (destination.getY() - 5), 10, 10);
   }

   public void goTo(Point2D point) {
      int direction = 1;
      double angle = Utils.normalRelativeAngle(Math.atan2(point.getX() - getX(), point.getY() - getY())
            - getHeadingRadians());
      if (Math.abs(angle) > Math.PI / 2) {
         direction = -1;
         angle += Math.PI;
      }
      setTurnRightRadians(Utils.normalRelativeAngle(angle));
      setAhead(direction * point.distance(getX(), getY()));
   }

   public void onHitObject(HitObjectEvent e) {
      System.out.println("Hit Object: " + e.getType());
      objectManager.inEvent(e);
      if (e.getType().equals("flag") && enemyFlag.distance(getX(), getY()) < 50) {
         flagCaptured = true;
         skipGoTo = 0;
      } else if (e.getType().equals("base") && homeBase.contains(new Point2D.Double(getX(), getY()))) {
         flagCaptured = false;
         skipGoTo = 0;
      }
   }

   public void onScannedRobot(ScannedRobotEvent e) {
      fire(1);
   }

   public void onHitByBullet(HitByBulletEvent e) {
      setTurnRight(5);
   }

   public void onHitObstacle(HitObstacleEvent e) {
      System.out.println("Hit Obstacle: " + e.getObstacleType());
      objectManager.inEvent(e);
      back(20);
      turnRight(30);
      ahead(30);
   }

   public void onHitWall(HitWallEvent e) {
      back(20);
      turnRight(30);
      ahead(30);
   }

   public void onScannedObject(ScannedObjectEvent e) {
      System.out.println("Scanned Object: " + e.getObjectType());
      objectManager.inEvent(e);

      double x = getX() + Math.sin((e.getBearing() + getHeading()) * Math.PI / 180.0) * e.getDistance();
      double y = getY() + Math.cos((e.getBearing() + getHeading()) * Math.PI / 180.0) * e.getDistance();
      getGraphics().setColor(Color.BLUE);
      getGraphics().fillOval((int) (x - 5), (int) (y - 5), 10, 10);

      // if (e.getObjectType().equals("flag")) {
      // e.getBearing();
      // }
   }


}