package deltasquad.robot;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.Serializable;

import deltasquad.data.Drawable;
import deltasquad.graphics.RGraphics;
import deltasquad.utils.Trig;
import deltasquad.utils.Utils;

// TODO document class

public class RobotVector implements Cloneable, Serializable, Drawable {

   /**
    * Determines if a deserialized file is compatible with this class.<BR>
    * <BR>
    * Maintainers must change this value if and only if the new version of this class is not compatible with old
    * versions.
    */
   private static final long serialVersionUID = 7415604949876623460L;

   private double            x;
   private double            y;
   private double            deltaX;
   private double            deltaY;
   private double            heading;
   private double            velocity;

   private boolean           updatedDeltaX    = true;
   private boolean           updatedDeltaY    = true;
   private boolean           updatedHeading   = true;
   private boolean           updatedVelocity  = true;

   public RobotVector() {
      init(-1.0D, -1.0D, 0.0D, 0.0D);
   }

   public RobotVector(final double heading, final double velocity) {
      init(-1.0D, -1.0D, heading, velocity);
   }

   public RobotVector(final double x, final double y, final double heading, final double velocity) {
      init(x, y, heading, velocity);
   }

   public RobotVector(final Point2D point, final double heading, final double velocity) {
      init(point.getX(), point.getY(), heading, velocity);
   }

   private RobotVector(final RobotVector vector) {
      init(vector.getX(), vector.getY(), vector.getHeading(), vector.getVelocity());
   }

   private void init(final double x, final double y, final double heading, final double velocity) {
      this.x = x;
      this.y = y;
      // this.deltaX = Utils.getDeltaX(velocity, heading);
      // this.deltaY = Utils.getDeltaY(velocity, heading);
      this.heading = heading;
      this.velocity = velocity;

      updatedDeltaX = false;
      updatedDeltaY = false;
      updatedHeading = true;
      updatedVelocity = true;
   }

   public double getX() {
      return x;
   }

   public double getY() {
      return y;
   }

   public double getDeltaX() {
      if (!updatedDeltaX) {
         deltaX = getVelocity() * Trig.d_sin(getHeading());
         updatedDeltaX = true;
      }
      return deltaX;
   }

   public double getDeltaY() {
      if (!updatedDeltaY) {
         deltaY = getVelocity() * Trig.d_cos(getHeading());
         updatedDeltaY = true;
      }
      return deltaY;
   }

   public double getHeading() {
      if (!updatedHeading) {
         heading = Trig.d_atan2(getDeltaX(), getDeltaY());
         updatedHeading = true;
      }
      return heading;
   }

   public double getVelocity() {
      if (!updatedVelocity) {
         velocity = Utils.sqrt(Utils.sqr(getDeltaX()) + Utils.sqr(getDeltaY()));
         updatedVelocity = true;
      }
      return velocity;
   }

   public RobotVector add(RobotVector vector) {
      deltaX += vector.getDeltaX();
      deltaY += vector.getDeltaY();
      updatedHeading = false;
      updatedVelocity = false;
      return this;
   }

   public RobotVector rotate(double turn) {
      this.heading = getHeading() + turn;
      updatedDeltaX = false;
      updatedDeltaY = false;
      return this;
   }

   public RobotVector velocity(double newVelocity) {
      // if (newVelocity == getVelocity() || newVelocity > Rules.MAX_VELOCITY || newVelocity < -Rules.MAX_VELOCITY) {
      // return this;
      // }
      this.velocity = newVelocity;
      updatedDeltaX = false;
      updatedDeltaY = false;
      return this;
   }

   public static void add(Point2D point, RobotVector vector) {
      point.setLocation(point.getX() + vector.getDeltaX(), point.getY() + vector.getDeltaY());
   }

   public RobotVector startNew() {
      return new RobotVector(getX() + getDeltaX(), getY() + getDeltaY(), getHeading(), getVelocity());
   }

   public Line2D getLine() {
      return new Line2D.Double(getX(), getY(), getX() + getDeltaX(), getY() + getDeltaY());
   }

   public void draw(RGraphics grid) {
      if (getX() > 0.0D && getY() > 0.0D) {
         grid.draw(getLine());
      }
   }

   @Override
   public Object clone() {
      return new RobotVector(this);
   }

   @Override
   public boolean equals(final Object obj) {
      if (obj instanceof RobotVector) {
         RobotVector vector = (RobotVector) obj;
         return (vector.getDeltaX() == getDeltaX()) && (vector.getDeltaY() == getDeltaY()) && (vector.getX() == getX())
               && (vector.getY() == getY());
      }
      return false;
   }

}
