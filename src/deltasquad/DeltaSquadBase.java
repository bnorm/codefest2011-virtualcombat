package deltasquad;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;

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
import deltasquad.communication.RobotMessage;
import deltasquad.communication.ScannedRobotMessage;
import deltasquad.graphics.DrawMenu;
import deltasquad.graphics.RGraphics;
import deltasquad.management.RobotManager;
import deltasquad.management.TargetingManager;
import deltasquad.management.TeamManager;
import deltasquad.movement.gun.GunMovement;
import deltasquad.movement.radar.RadarMovement;
import deltasquad.object.ObjectManager;
import deltasquad.robot.EnemyData;
import deltasquad.robot.TeammateData;
import deltasquad.targeting.CircularTargeting;
import deltasquad.targeting.HeadOnTargeting;
import deltasquad.targeting.LinearTargeting;
import deltasquad.targeting.Targeting;
import deltasquad.utils.Utils;
import deltasquad.virtual.VirtualGun;

/**
 * <p>
 * Title: DeltaThreeEight, RC-1138, AKA 'Boss'
 * </p>
 * <p>
 * Description: Even among clones, there is a heirarchy. Three-Eight is undisputed leader of the Deltas. Relatively
 * taciturn, when he speaks, it's usually to bark out an order to his squad. Three-Eight has earned the respect and
 * loyalty of his squad, and he repays that dedication in strong leadership. Despite being trained by Walon Vau,
 * Three-Eight somehow inherited Jango's strong Concord Dawn accent and speech patterns.
 * </p>
 * 
 * @author Brian Norman
 * @version .1
 */
public class DeltaSquadBase extends CaptureTheFlagApi {

   public boolean          roundEnded = false;

   public RobotManager     robots;
   public ObjectManager    objects;
   public TeamManager      team;
   public RadarMovement    radar;
   public GunMovement      gun;

   public boolean          wasSeeker  = false;
   public MinimumRiskPoint movement;
   public SeekerMovement   seeker;

   public TargetingManager targeting;

   public EnemyData        enemy      = new EnemyData();

   @Override
   public void run() {
      registerMe();
      roundEnded = false;

      setAdjustGunForRobotTurn(true);
      setAdjustRadarForGunTurn(true);
      setColors(Color.BLUE, Color.BLUE, Color.BLUE);

      Targeting[] targetings = { new HeadOnTargeting(this), new LinearTargeting(this), new LinearTargeting(this, true),
            new CircularTargeting(this), new CircularTargeting(this, false, true),
            new CircularTargeting(this, true, false), new CircularTargeting(this, true, true) };

      robots = new RobotManager(this);
      objects = new ObjectManager(this);
      team = new TeamManager(this);

      radar = new RadarMovement(this);
      gun = new GunMovement(this);
      movement = new DefenderMovement(this, objects);
      seeker = new SeekerMovement(this, objects);

      targeting = new TargetingManager(this, targetings);

      while (true) {
         UpdateBattlefieldState(getBattlefieldState());
         // enemy = robots.getEnemy(RobotChooser.CLOSEST);
         enemy = getEnemy();

         if (isSeeker()) {
            // if (!wasSeeker) {
            // System.out.println("GOING FOR THE FLAG!");
            // }
            seeker.move(robots.getRobots(), team.getTeammateBullets());
            wasSeeker = true;
         } else {
            movement.move(robots.getRobots(), team.getTeammateBullets());
            wasSeeker = false;
         }

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

   public boolean isSeeker() {
      if (roundEnded)
         return wasSeeker;
      boolean isSeeker = true;
      double dist = getEnemyFlag().distanceSq(getX(), getY());
      for (TeammateData t : robots.getTeammates()) {
         if (!t.isDead() && getEnemyFlag().distanceSq(t.getX(), t.getY()) < dist) {
            isSeeker = false;
            break;
         }
      }
      return isSeeker;
   }

   public EnemyData getEnemy() {
      double x = getX();
      double y = getY();

      EnemyData enemy = this.enemy;
      double distSq = Utils.distSq(x, y, enemy.getX(), enemy.getY()) * 0.9;

      for (EnemyData e : robots.getEnemies()) {
         if (!e.isDead()) {
            double eDistDq = Utils.distSq(x, y, e.getX(), e.getY());
            Line2D line = new Line2D.Double(x, y, e.getX(), e.getY());
            if (eDistDq < distSq && !objects.blocked(line)) {
               distSq = eDistDq;
               enemy = e;
            }
         }
      }

      return enemy;
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
      seeker.inEvent(e);
      movement.inEvent(e);
   }

   @Override
   public void onHitObstacle(HitObstacleEvent e) {
      objects.inEvent(e);
      seeker.inEvent(e);
      movement.inEvent(e);
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
      movement.inEvent(e);
   }

   @Override
   public void onPaint(Graphics2D graphics) {
      if (getTime() < 10)
         return;
      RGraphics grid = new RGraphics(graphics, this);
      DrawMenu.draw(grid);
      robots.draw(grid);
      targeting.draw(grid);

      objects.draw(grid);
      team.draw(grid);
   }

   @Override
   public void onScannedRobot(ScannedRobotEvent e) {
      team.broadcast(new ScannedRobotMessage(e, this));
      team.inEvent(e);
      robots.inEvent(e);
      targeting.inEvent(e);
   }

   @Override
   public void onRobotDeath(RobotDeathEvent e) {
      robots.inEvent(e);
      targeting.inEvent(e);
   }

   @Override
   public void onHitByBullet(HitByBulletEvent e) {
   }

   @Override
   public void onBulletHit(BulletHitEvent e) {
   }

   @Override
   public void onBulletHitBullet(BulletHitBulletEvent e) {
   }

   @Override
   public void onWin(WinEvent e) {
      robots.inEvent(e);
      roundEnded = true;
   }

   @Override
   public void onDeath(DeathEvent e) {
      robots.inEvent(e);
      targeting.inEvent(e);
   }

   @Override
   public void onMouseClicked(MouseEvent e) {
      DrawMenu.inMouseEvent(e);
   }


   private static long SKIPPED_TURNS = 0;

   @Override
   public void onSkippedTurn(final SkippedTurnEvent e) {
      out.println("SKIPPED TURN! (Time: " + e.getTime() + ", Total: " + ++SKIPPED_TURNS + ")");
   }

}
