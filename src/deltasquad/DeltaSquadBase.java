package deltasquad;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;

import deltasquad.communication.RobotMessage;
import deltasquad.communication.ScannedRobotMessage;
import deltasquad.data.RobotChooser;
import deltasquad.graphics.Colors;
import deltasquad.graphics.DrawMenu;
import deltasquad.graphics.RGraphics;
import deltasquad.info.RobotInfo;
import deltasquad.management.RobotManager;
import deltasquad.management.TargetingManager;
import deltasquad.management.TeamManager;
import deltasquad.movement.gun.GunMovement;
import deltasquad.movement.radar.RadarMovement;
import deltasquad.movement.robot.RobotMovement;
import deltasquad.object.ObjectManager;
import deltasquad.robot.EnemyData;
import deltasquad.robot.RobotData;
import deltasquad.robot.TeammateData;
import deltasquad.targeting.CircularTargeting;
import deltasquad.targeting.HeadOnTargeting;
import deltasquad.targeting.LinearTargeting;
import deltasquad.targeting.Targeting;
import deltasquad.utils.Utils;
import deltasquad.virtual.VirtualBullet;
import deltasquad.virtual.VirtualGun;

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

   // private MinimumRiskPoint melee;
   private RadarMovement    radar;
   private GunMovement      gun;

   private RobotManager     robots;
   private TargetingManager targeting;

   private ObjectManager    objects;
   private TeamManager      team;

   @Override
   public void run() {
      registerMe();

      setColors(Colors.OFF_ORANGE, Colors.SILVER, Colors.VISER_BLUE);
      initMove();

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

      // melee = new MinimumRiskPoint(this, objects);
      radar = new RadarMovement(this);
      gun = new GunMovement(this);

      while (true) {
         UpdateBattlefieldState(getBattlefieldState());
         EnemyData enemy = robots.getEnemy(RobotChooser.CLOSEST);

         move();

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
   public void onScannedRobot(final ScannedRobotEvent e) {
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



   // *************************************************************************
   // MOVEMENT
   // *************************************************************************

   RobotInfo                info;
   RobotMovement            movement;

   private Point2D          nextPosition;
   private Point2D          oldPosition;

   private RoundRectangle2D battleField;
   private final double     walldist                = 40;
   private final double     cornerarc               = 100;

   private final int        NUM_OF_GENERATED_POINTS = 20;
   private final int        CORNER_RISK             = 2;
   private final int        BOT_RISK                = 100;

   public void initMove() {
      info = new RobotInfo(this);
      movement = new RobotMovement(this);
      nextPosition = new Point2D.Double(getX(), getY());
      battleField = new RoundRectangle2D.Double(walldist, walldist, getBattleFieldWidth() - 2 * walldist,
            getBattleFieldHeight() - 2 * walldist, cornerarc, cornerarc);
   }

   public void move() {
      movement.setMoveToPoint(getPoint(robots.getRobots(), team.getTeammateBullets()));
   }

   public Point2D getPoint(final RobotData[] robots, final VirtualBullet[] teammateBullets) {
      double myX = info.getX();
      double myY = info.getY();
      // long time = info.getTime();

      double minDist = Utils.sqrt(distSq(robots));

      double pointDist = Utils.distSq(nextPosition, myX, myY);
      double pointRisk = risk(nextPosition, robots, teammateBullets);
      if (pointDist < Utils.sqr(20)) {
         oldPosition = nextPosition;
      }

      double dist = Utils.random(minDist / 2, minDist);

      for (double a = 0; a < Utils.CIRCLE; a += Utils.CIRCLE / NUM_OF_GENERATED_POINTS) {
         double angle = Utils.random(a, a + Utils.CIRCLE / NUM_OF_GENERATED_POINTS);
         Point2D point = Utils.getPoint(myX, myY, dist, angle);

         if (battleField.contains(point)) {
            double risk = risk(point, angle, robots, teammateBullets);
            if (risk < pointRisk) {
               nextPosition = point;
               pointRisk = risk;
            }
         }

      }
      return nextPosition;
   }

   public double distSq(RobotData[] robots) {
      double minDist = info.distSq(RobotChooser.CLOSEST.getRobot(this, Arrays.asList(robots)));

      // for (VirtualBullet b : teammateBullets)
      // minDist = Math.min(2 * Utils.distSq(myX, myY, b.getX(time),
      // b.getY(time)), minDist);

      // for (ObjectManager.Point p : objectManager.getPoints())
      // minDist = Math.min(4.0 * Utils.distSq(myX, myY, p.x_, p.y_), minDist);

      return minDist;
   }

   public double risk(final Point2D point, final RobotData[] robots, final VirtualBullet[] teammateBullets) {
      return risk(point, info.angle(point), robots, teammateBullets);
   }

   public double risk(final Point2D point, final double angle, final RobotData[] robots,
         final VirtualBullet[] teammateBullets) {
      double myX = info.getX();
      double myY = info.getY();
      // double distSq = point.distanceSq(myX, myY);
      long time = info.getTime();
      double pointRisk = 0.0D;
      Line2D path = new Line2D.Double(myX, myY, point.getX(), point.getY());

      Line2D[] cornerPaths = new Line2D.Double[4];
      double add = 16;
      cornerPaths[0] = new Line2D.Double(myX - add, myY + add, point.getX() - add, point.getY() + add);
      cornerPaths[1] = new Line2D.Double(myX + add, myY + add, point.getX() + add, point.getY() + add);
      cornerPaths[2] = new Line2D.Double(myX + add, myY - add, point.getX() + add, point.getY() - add);
      cornerPaths[3] = new Line2D.Double(myX - add, myY - add, point.getX() - add, point.getY() - add);
      Rectangle2D destination = new Rectangle2D.Double(point.getX() - add, point.getY() - add, 2 * add, 2 * add);
      Rectangle2D me = new Rectangle2D.Double(myX - add, myY - add, 2 * add, 2 * add);


      for (RobotData r : robots) {
         if (!r.isDead()) {
            boolean intersects = path.intersects(r.getRectangle());
            for (int i = 0; !intersects && i < 4; i++) {
               intersects = cornerPaths[i].intersects(r.getRectangle());
            }

            if (intersects) {
               return Double.POSITIVE_INFINITY;
            } else {
               double robotRisk = BOT_RISK;
               if (!(r instanceof TeammateData)) {
                  robotRisk += r.getEnergy();
                  robotRisk *= (1 + Math.abs(Utils.cos(angle - Utils.angle(myX, myY, r.getX(), r.getY()))));

                  // boolean amiclosest = true;
                  // double myDist = Utils.distSq(myX, myY, r.getX(), r.getY());
                  // for (int i = 0; i < robots.length && amiclosest; i++) {
                  // if (!r.getName().equals(robots[i].getName()) &&
                  // Utils.distSq(r.getX(),
                  // r.getY(), robots[i].getX(),
                  // robots[i].getY()) < myDist)
                  // amiclosest = false;
                  // }
                  // if (amiclosest)
                  // robotRisk *= Utils.sqr(1 + Math.abs(Utils.cos(angle -
                  // Utils.angle(myX, myY,
                  // r.getX(), r.getY()))));
               }

               robotRisk /= Utils.distSq(point, r.getX(), r.getY());
               pointRisk += robotRisk;
            }
         }
      }


      // for (Edge e : objects.getEdges()) {
      // boolean intersects = path.intersectsLine(e);
      // if (!me.intersectsLine(e)) {
      // for (int i = 0; !intersects && i < 4; i++) {
      // intersects = cornerPaths[i].intersectsLine(e);
      // }
      // }
      // if (intersects || destination.intersectsLine(e)) {
      // return Double.POSITIVE_INFINITY;
      // }
      // }

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


      // for (ObjectManager.Point p : objectManager.getPoints()) {
      // double objPointRisk = 10.0;
      // if (Utils.distSq(myX, myY, p.x_, p.y_) < distSq
      // && Math.abs(Utils.relative(Utils.angle(myX, myY, p.x_, p.y_) - angle)) < 10.0) {
      // // objPointRisk = 10000.0;
      // return Double.POSITIVE_INFINITY;
      // } else {
      // objPointRisk /= point.distanceSq(p.x_, p.y_);
      // }
      // pointRisk += objPointRisk;
      // }

      pointRisk += info.getOthers()
            / Utils.distSq(point, info.getBattleFieldWidth() / 2, info.getBattleFieldHeight() / 2);

      pointRisk += CORNER_RISK / Utils.distSq(point, info.getBattleFieldWidth(), info.getBattleFieldHeight());
      pointRisk += CORNER_RISK / Utils.distSq(point, 0.0D, info.getBattleFieldHeight());
      pointRisk += CORNER_RISK / Utils.distSq(point, 0.0D, 0.0D);
      pointRisk += CORNER_RISK / Utils.distSq(point, info.getBattleFieldWidth(), 0.0D);



      RGraphics grid = new RGraphics(this.getGraphics(), this);
      grid.setColor(Color.RED);
      // grid.draw(path);
      // grid.draw(cornerPaths[0]);
      // grid.draw(cornerPaths[1]);
      // grid.draw(cornerPaths[2]);
      // grid.draw(cornerPaths[3]);
      grid.setColor(Color.RED);
      if (!objects.blocked(destination))
         grid.setColor(Color.GREEN);
      grid.draw(destination);

      grid.setColor(Color.RED);
      if (!objects.blocked(me))
         grid.setColor(Color.GREEN);
      grid.draw(me);

      return pointRisk;
   }
}
