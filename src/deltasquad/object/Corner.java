package deltasquad.object;

import java.io.Serializable;

public class Corner extends Point implements Serializable {
   private static final long serialVersionUID = 5518549448487386606L;

   public Corner(double x, double y, double sightAngle) {
      super(x, y, sightAngle);
   }

   public boolean equals(Corner c) {
      return near(c, 10.0);
   }

   @Override
   public boolean equals(Object obj) {
      if (obj instanceof Corner) {
         return equals((Corner) obj);
      }
      return false;
   }
}