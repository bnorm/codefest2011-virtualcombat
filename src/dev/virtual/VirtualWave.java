package dev.virtual;

import java.awt.geom.Rectangle2D;

import robocode.Robot;
import robocode.Rules;
import dev.data.robots.RobotData;
import dev.draw.RobotColor;
import dev.draw.RobotGraphics;
import dev.utils.Trig;
import dev.utils.Utils;


public class VirtualWave {

   /**
    * Determines if a deserialized file is compatible with this class.<br>
    * <br>
    * Maintainers must change this value if and only if the new version of this class is not compatible with old
    * versions.
    */
   private static final long serialVersionUID = 3030661579076677220L;

   /**
    * The x coordinate of the point from which this wave originated.
    */
   protected double          originX;

   /**
    * The y coordinate of the point from which this wave originated.
    */
   protected double          originY;

   /**
    * The supposed heading of this wave. I.e., a wave has no real heading, it is only used for a reference.
    */
   protected double          heading;

   /**
    * The fire power at which the wave was fired.
    */
   protected double          firePower;

   /**
    * The speed at which the wave is traveling.
    */
   protected double          velocity;

   /**
    * The maximum angle a robot could reach with reference to the supposed heading before the wave passes it.
    */
   protected double          maxEscapeAngle;

   /**
    * The time at which the wave was fired or created.
    */
   protected long            creationTime;

   /**
    * Creates a blank {@code VirtualWave class}.
    */
   public VirtualWave() {
      this(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Trig.HALF_CIRCLE, 0.0, -1L);
   }

   protected VirtualWave(VirtualWave wave) {
      this(wave.getStartX(), wave.getStartX(), wave.getHeading(), wave.getFirePower(), wave.getCreationTime());
   }

   public VirtualWave(double startX, double startY, double heading, double firePower, long creationTime) {
      init(startX, startY, heading, firePower, creationTime);
   }

   private void init(double startX, double startY, double heading, double firePower, final long creationTime) {
      this.originX = startX;
      this.originY = startY;
      this.heading = heading;
      this.firePower = firePower;
      this.velocity = Rules.getBulletSpeed(this.firePower);
      this.maxEscapeAngle = Utils.maxEscapeAngle(this.velocity);
      this.creationTime = creationTime;
   }

   /**
    * @return the maximum escape angle
    * @see #maxEscapeAngle
    */
   public double getMaxEscapeAngle() {
      return maxEscapeAngle;
   }

   /**
    * Returns the X coordinate of the wave's origin to {@code double} precision.
    * 
    * @return the X coordinate of the wave's origin
    * @see #originX
    */
   public double getStartX() {
      return originX;
   }

   /**
    * Returns the Y coordinate of the wave's origin to {@code double} precision.
    * 
    * @return the Y coordinate of the wave's origin
    * @see #originY
    */
   public double getStartY() {
      return originY;
   }

   /**
    * Returns the supposed heading that this wave is traveling in to <code>double</code> precision.
    * 
    * @return the wave's supposed heading
    * @see #heading
    */
   public double getHeading() {
      return heading;
   }

   /**
    * Returns the fire power of the wave to <code>double</code> precision.
    * 
    * @return the wave's fire power
    * @see #firePower
    */
   public double getFirePower() {
      return firePower;
   }

   /**
    * Returns the speed at which the wave is traveling to <code>double</code> precision.
    * 
    * @return the wave's speed
    * @see #velocity
    */
   public double getVelocity() {
      return velocity;
   }

   /**
    * Returns the creation time of the wave to <code>long</code> precision.
    * 
    * @return the wave's creation time.
    * @see #creationTime
    */
   public long getCreationTime() {
      return creationTime;
   }

   // BORED documentation: VirtualWave - active(long)
   public boolean active(long time) {
      return !(getStartX() < 0 || getStartY() < 0 || getDist(time) > 7500.0D);
   }

   /**
    * Returns the travel distance of the wave at <code>time</code> to <code>double</code> precision.
    * 
    * @param time
    *           game time.
    * @return wave's travel distance.
    */
   public double getDist(long time) {
      return getVelocity() * (time - getCreationTime());
   }

   // BORED documentation: VirtualWave - getDistSq(long)
   public double getDistSq(long time) {
      return Utils.sqr(getDist(time));
   }

   /**
    * Returns the distance to the wave's impact with the coordinates <code>(x,y)</code> to <code>double</code>
    * precision, with relation to <code>time</code>.
    * 
    * @param x
    *           impact's x coordinate
    * @param y
    *           impact's y coordinate
    * @param time
    *           game time
    * @return distance to wave's impact
    */
   public double getDistToImpact(double x, double y, long time) {
      double dist = Utils.dist(getStartX(), getStartY(), x, y);
      dist -= getDist(time);
      return dist;
   }

   /**
    * Returns the time to the wave's impact with the coordinates <code>(x,y)</code> to <code>double</code> precision,
    * with relation to <code>time</code>.
    * 
    * @param x
    *           impact's x coordinate
    * @param y
    *           impact's y coordinate
    * @param time
    *           game time
    * @return time to wave's impact
    */
   public long getTimeToImpact(double x, double y, long time) {
      double dist = getDistToImpact(x, y, time);
      return (long) (dist / getVelocity());
   }

   public boolean testHit(RobotData robot) {
      return testHit(robot.getX(), robot.getY(), robot.getTime());
   }

   public boolean testHit(Robot robot) {
      return testHit(robot.getX(), robot.getY(), robot.getTime());
   }

   public boolean testHit(Rectangle2D rectangle, long time) {
      return testHit(rectangle.getCenterX(), rectangle.getCenterY(), time);
   }

   public boolean testHit(double x, double y, long time) {
      return Utils.distSq(getStartX(), getStartY(), x, y) <= Utils.sqr(getDist(time));
   }

   public void draw(RobotGraphics grid) {
      double dist = getDist(grid.getTime());
      if (dist < 7500) {
         grid.setColor(RobotColor.SILVER);
         grid.drawArcCenter(getStartX(), getStartY(), 2.0D * dist, 2.0D * dist, getHeading() - getMaxEscapeAngle(),
               2.0D * getMaxEscapeAngle());
         grid.fillOvalCenter(getStartX(), getStartY(), 5.0D, 5.0D);
         grid.drawLine(getStartX(), getStartY(), Utils.getX(getStartX(), dist, getHeading() - getMaxEscapeAngle()),
               Utils.getY(getStartY(), dist, getHeading() - getMaxEscapeAngle()));
         grid.drawLine(getStartX(), getStartY(), Utils.getX(getStartX(), dist, getHeading() + getMaxEscapeAngle()),
               Utils.getY(getStartY(), dist, getHeading() + getMaxEscapeAngle()));
      }
   }

   public VirtualWave copy() {
      return new VirtualWave(this);
   }

   @Override
   public Object clone() {
      return copy();
   }

   @Override
   public boolean equals(Object obj) {
      if (obj instanceof VirtualWave) {
         VirtualWave wave = (VirtualWave) obj;
         return (wave.getStartX() == this.getStartX()) && (wave.getStartY() == this.getStartY())
               && (wave.getHeading() == getHeading()) && (wave.getCreationTime() == getCreationTime());
      }
      return super.equals(obj);
   }

   @Override
   public void finalize() {
      originX = 0.0D;
      originY = 0.0D;
      heading = 0.0D;
      firePower = 0.0D;
      velocity = 0.0D;
      maxEscapeAngle = 0.0D;
      creationTime = 0L;
   }

}