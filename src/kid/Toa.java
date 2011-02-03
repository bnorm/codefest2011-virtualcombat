package kid;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;

import kid.communication.RobotMessage;
import kid.communication.ScannedRobotMessage;
import kid.data.RobotChooser;
import kid.graphics.Colors;
import kid.graphics.DrawMenu;
import kid.graphics.RGraphics;
import kid.management.ObjectManager;
import kid.management.RobotManager;
import kid.management.TargetingManager;
import kid.management.TeamManager;
import kid.movement.gun.GunMovement;
import kid.movement.radar.RadarMovement;
import kid.movement.robot.MinimumRiskPoint;
import kid.robot.EnemyData;
import kid.targeting.CircularTargeting;
import kid.targeting.HeadOnTargeting;
import kid.targeting.LinearTargeting;
import kid.targeting.Targeting;
import kid.utils.Utils;
import kid.virtual.VirtualGun;
import robocode.BulletHitBulletEvent;
import robocode.BulletHitEvent;
import robocode.DeathEvent;
import robocode.HitByBulletEvent;
import robocode.HitObjectEvent;
import robocode.HitObstacleEvent;
import robocode.MessageEvent;
import robocode.RobotDeathEvent;
import robocode.ScannedObjectEvent;
import robocode.ScannedRobotEvent;
import robocode.SkippedTurnEvent;
import robocode.WinEvent;
import CTFApi.CaptureTheFlagApi;

public class Toa extends CaptureTheFlagApi {

   private MinimumRiskPoint melee;
   private RadarMovement    radar;
   private GunMovement      gun;

   private RobotManager     robots;
   private TargetingManager targeting;

   private ObjectManager    objects;
   private TeamManager      team;

   @Override
   public void run() {

      objects = new ObjectManager(this);
      team = new TeamManager(this);

      setColors(Colors.DARK_RED, Colors.SILVER, Colors.DIRT_GREEN);
      setAdjustGunForRobotTurn(true);
      setAdjustRadarForGunTurn(true);

      Targeting[] targetings = { new HeadOnTargeting(this), new LinearTargeting(this), new LinearTargeting(this, true),
            new CircularTargeting(this), new CircularTargeting(this, false, true),
            new CircularTargeting(this, true, false), new CircularTargeting(this, true, true) };

      robots = new RobotManager(this);
      targeting = new TargetingManager(this, targetings);

      melee = new MinimumRiskPoint(this, objects);
      radar = new RadarMovement(this);
      gun = new GunMovement(this);

      while (true) {
         EnemyData enemy = robots.getEnemy(RobotChooser.CLOSEST);

         melee.move(robots.getRobots());

         setTurnRadarRight(Double.POSITIVE_INFINITY);
         if (getGunHeat() < .4 || getOthers() == 1)
            radar.setSweep(enemy, Utils.EIGHTIETH_CIRCLE / 3);

         double firepower = getFirePower(enemy);
         gun.setTurnTo(targeting.getBestGun(enemy).getTargeting(), enemy, firepower);
         if (shouldFire(enemy, firepower)) {
            targeting.fire(enemy, firepower);
         }


         team.broadcast(new RobotMessage(this));
         targeting.broadcastBullets();

         execute();
      }
   }

   private boolean shouldFire(EnemyData enemy, double firepower) {
      boolean fire = !enemy.isDead() && firepower < getEnergy() && Math.abs(getGunTurnRemaining()) < 2.0;
      if (getOthers() > 1) {
         double x = getX(), y = getY();
         double dist = Utils.dist(x, y, enemy.getX(), enemy.getY());
         double angle = getGunHeading();
         Line2D line = new Line2D.Double(x, y, Utils.getX(x, dist, angle), Utils.getY(y, dist, angle));
         getGraphics().setColor(Color.RED);
         getGraphics().draw(line);
         return fire && !objects.blocked(line);
      } else {
         return enemy.distSq(getX(), getY()) < 40000
               || (fire && (getEnergy() - firepower > enemy.getEnergy() || getEnergy() > 16.0));
      }
   }

   private double getFirePower(EnemyData enemy) {
      double firepower = 3.0;
      if (getOthers() > 1) {
         firepower = (400.0 * 3.0) / enemy.dist(getX(), getY());
         // double[] walls = {
         // getBattleFieldHeight(), getBattleFieldWidth()
         // };
         // firepower *= i.distToWall(getGunHeading()) / Utils.avg(walls) * getOthers();
      } else if (enemy.distSq(getX(), getY()) > 40000) {
         firepower = (400.0 * 2.0) / enemy.dist(getX(), getY());
         VirtualGun gun = targeting.getBestGun(enemy);
         if (gun.getRealHitRate() >= 0.12)
            firepower *= gun.getHitRate() / 0.15;
         if (getEnergy() < 32.0)
            firepower *= Utils.limit(0.5, getEnergy() / enemy.getEnergy(), 2.0);
      }
      firepower = Math.min(firepower, enemy.getEnergy() / 4.0);
      return Utils.limit(0.1, firepower, 3.0);
   }

   @Override
   public void onHitObject(HitObjectEvent e) {
      objects.inEvent(e);
   }

   @Override
   public void onHitObstacle(HitObstacleEvent e) {
      objects.inEvent(e);
   }

   @Override
   public void onScannedObject(ScannedObjectEvent e) {
      objects.inEvent(e);
   }

   @Override
   public void onMessageReceived(MessageEvent e) {
      team.inEvent(e);
      robots.inEvent(e);
      objects.inEvent(e);
   }

   @Override
   public void onPaint(final Graphics2D graphics) {
      RGraphics grid = new RGraphics(graphics, this);
      DrawMenu.draw(grid);
      robots.draw(grid);
      targeting.draw(grid);

      objects.draw(grid);
      team.draw(grid);
   }

   @Override
   public void onScannedRobot(final ScannedRobotEvent e) {
      // if (isTeammate(e.getName())) {
      // System.out.println("SCANNED TEAMAMTE: " + e.getName());
      // } else {
      // System.out.println("SCANNED ENEMY: " + e.getName());
      // }

      team.broadcast(new ScannedRobotMessage(e, this));
      team.inEvent(e);
      robots.inEvent(e);
      targeting.inEvent(e);
   }

   @Override
   public void onRobotDeath(final RobotDeathEvent e) {
      robots.inEvent(e);
      targeting.inEvent(e);
   }

   @Override
   public void onHitByBullet(final HitByBulletEvent e) {
   }

   @Override
   public void onBulletHit(final BulletHitEvent e) {
   }

   @Override
   public void onBulletHitBullet(final BulletHitBulletEvent e) {
   }

   @Override
   public void onWin(final WinEvent e) {
      robots.inEvent(e);
   }

   @Override
   public void onDeath(final DeathEvent e) {
      robots.inEvent(e);
      targeting.inEvent(e);
   }

   @Override
   public void onMouseClicked(final MouseEvent e) {
      DrawMenu.inMouseEvent(e);
   }


   private static long SKIPPED_TURNS = 0;

   @Override
   public void onSkippedTurn(final SkippedTurnEvent e) {
      out.println("SKIPPED TURN! (Time: " + e.getTime() + ", Total: " + ++SKIPPED_TURNS + ")");
   }

}
