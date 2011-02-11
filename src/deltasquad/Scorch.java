package deltasquad;

import java.awt.Color;
import java.awt.geom.Point2D;

import deltasquad.graphics.Colors;
import deltasquad.robot.RobotData;
import deltasquad.virtual.VirtualBullet;

import robocode.HitObjectEvent;


// exploseves, heavy blaster, wild
/*
 * maybe the front line kind of guy fire higher power shots get up in the face of weak bots
 */
/**
 * <p>
 * Title: DeltaSixTwo, RC-1262, AKA 'Scorch'
 * </p>
 * <p>
 * Description: War does funny things to people. Similarly, some people do funny things with war. Six-Two is the Delta's
 * resident wiseacre, regularly dropping a world-weary bon mot into the stew of violence and destruction that serves as
 * the Deltas' steady diet. A competent soldier, and an excellent explosives technician, Six-Two has an overdeveloped
 * sense of irony that could be mistaken for fatalism. 'Scorch' earned his nickname after an ordnance accident that left
 * him and Sergeant Walon Vau without eyebrows for a short time.
 * </p>
 * 
 * @author Brian Norman
 * @version .1
 */
public class Scorch extends DeltaSquadBase {

   public boolean captured_ = false;

   @Override
   public void setColors(Color bodyColor, Color gunColor, Color radarColor) {
      super.setColors(Colors.DIRT_YELLOW, Colors.SILVER, Colors.VISER_BLUE);
   }

   @Override
   public double distSq(RobotData[] robots) {
      if (captured_)
         return Math.min(super.distSq(robots), info.distSq(getOwnBase().getCenterX(), getOwnBase().getCenterY()));
      else
         return Math.min(super.distSq(robots), info.distSq(getEnemyFlag()));
   }

   @Override
   public double risk(Point2D point, double angle, RobotData[] robots, VirtualBullet[] teammateBullets) {
      double risk = 0;
      double flag = -1000;
      double base = -1000;
      if (captured_) {
         risk = base / point.distance(getOwnBase().getCenterX(), getOwnBase().getCenterY());
      } else {
         risk = flag / point.distance(getEnemyFlag());
      }
      return risk + super.risk(point, angle, robots, teammateBullets);
   }

   public void onHitObject(HitObjectEvent e) {
      if (e.getType().equals("flag") && info.distSq(getEnemyFlag()) < 2500) {
         System.out.println("ENEMY FLAG CAPTURED!");
         captured_ = true;
      }
      super.onHitObject(e);
   }

}