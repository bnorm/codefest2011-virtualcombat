package deltasquad.object;

import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;

import robocode.AdvancedRobot;
import robocode.Event;
import robocode.HitObjectEvent;
import robocode.HitObstacleEvent;
import robocode.MessageEvent;
import robocode.ScannedObjectEvent;
import robocode.TeamRobot;
import deltasquad.data.Drawable;
import deltasquad.graphics.RGraphics;
import deltasquad.utils.Utils;

public class ObjectManager implements Drawable {

  private AdvancedRobot               robot_;

  private static LinkedList<BoxPoint> points_ = new LinkedList<BoxPoint>();
  private static LinkedList<BoxEdge>  edges_  = new LinkedList<BoxEdge>();

  public ObjectManager(AdvancedRobot robot) {
    robot_ = robot;
  }

  public LinkedList<Point2D> getShadowPoints(double buffer) {
    LinkedList<Point2D> shadows = new LinkedList<Point2D>();
    for (BoxEdge e : edges_) {
      Line2D shadow = e.getLineShadow(buffer);
      shadows.add(shadow.getP1());
      shadows.add(shadow.getP2());
    }
    return shadows;
  }

  public boolean blocked(Line2D path) {
    if (path != null) {
      for (BoxEdge e : edges_) {
        if (e.intersectsLine(path)) {
          return true;
        }
      }
    }
    return false;
  }

  public boolean blockedShadow(Line2D path) {
    if (path != null) {
      for (BoxEdge e : edges_) {
        if (e.getLineShadow().intersectsLine(path)) {
          return true;
        }
      }
    }
    return false;
  }

  public LinkedList<Line2D> blockingShadows(Line2D path) {
    if (path != null) {
      LinkedList<Line2D> blockers = new LinkedList<Line2D>();
      for (BoxEdge e : edges_) {
        if (e.getLineShadow().intersectsLine(path)) {
          blockers.add(e.getLineShadow(BoxEdge.SHADOW_LENGTH + 10.0));
        }
      }

      return blockers;
    }
    return null;
  }

  public boolean blocked(Rectangle2D robot) {
    if (robot != null) {
      for (BoxEdge e : edges_) {
        if (e.intersects(robot)) {
          return true;
        }
      }
    }
    return false;
  }

  private void send(Serializable s) {
    if (robot_ instanceof TeamRobot) {
      try {
        ((TeamRobot) robot_).broadcastMessage(s);
      } catch (IOException e) {
        System.err.println("BROADCAST ERROR: could not send point to teammates");
        e.printStackTrace();
      }
    }
  }

  private void add(BoxPoint point) {
    LinkedList<BoxEdge> nearEdges = new LinkedList<BoxEdge>();

    for (BoxEdge e : edges_) {
      if (e.contains(point)) {
        return;
      } else if (e.extend(point, false)) {
        nearEdges.add(e);
      }
    }

    LinkedList<BoxPoint> removePoints = new LinkedList<BoxPoint>();
    for (BoxPoint p : points_) {
      if (p.equals(point)) {
        removePoints.add(p);
      } else {
        BoxEdge edge = BoxEdge.createEdge(point, p, false, false);
        if (edge != null) {
          removePoints.add(p);
          edges_.add(edge);
          nearEdges.add(edge);
        }
      }
    }

    for (BoxPoint p : removePoints) {
      points_.remove(p);
    }

    if (nearEdges.size() > 0) {
      BoxEdge[] edges = nearEdges.toArray(new BoxEdge[nearEdges.size()]);
      for (int i = 0; i < edges.length - 1; i++) {
        for (int j = i + 1; j < edges.length; j++) {
          if (edges[i].extend(edges[j]) || edges[i].inside(edges[j])) {
            edges_.remove(edges[j]);
          }
        }
      }
    } else { // if (nearEdges.size() == 0)
      points_.add(point);
    }
  }

  public void inEvent(Event e) {
    if (e instanceof HitObstacleEvent) {
      handleHitObstacle((HitObstacleEvent) e);
    } else if (e instanceof HitObjectEvent) {
      handleHitObject((HitObjectEvent) e);
    } else if (e instanceof ScannedObjectEvent) {
      handleScannedObject((ScannedObjectEvent) e);
    } else if (e instanceof MessageEvent) {
      handleMessage((MessageEvent) e);
    }
  }

  private void handleHitObstacle(HitObstacleEvent e) {
    // System.out.println("Hit Obstacle: " + e.getObstacleType());
    // double angle = Utils.relative(robot_.getHeading() + e.getBearing());
    // System.out.println("From: " + angle + " To: " + Utils.normalize(angle,
    // -45, 45));
    // double dist = 18 / Utils.cosd(Utils.normalize(angle, -45, 45));
    // double x = Utils.getX(robot_.getX(), dist, angle);
    // double y = Utils.getY(robot_.getY(), dist, angle);
    // BoxPoint point = new BoxPoint(x, y, angle);
    // add(point);
  }

  private void handleHitObject(HitObjectEvent e) {
    // System.out.println("Hit Object: " + e.getType());
  }

  private void handleScannedObject(ScannedObjectEvent e) {
    if (e.getObjectType().equals("box")) {
      double angle = e.getBearing() + robot_.getHeading();
      double x = Utils.getX(robot_.getX(), e.getDistance(), angle);
      double y = Utils.getY(robot_.getY(), e.getDistance(), angle);

      BoxPoint point = new BoxPoint(x, y, angle);
      add(point);
      send(point);

    }
  }

  private void handleMessage(MessageEvent e) {
    Serializable message = e.getMessage();
    if (message instanceof BoxPoint) {
      BoxPoint p = (BoxPoint) message;
      add(p);
    }
  }

  @Override
  public void draw(RGraphics grid) {
    for (BoxEdge e : edges_) {
      grid.setColor(Color.BLUE);
      grid.draw(e);
      grid.setColor(Color.RED);
      grid.draw(e.getLineShadow());
      // grid.setColor(Color.RED);
      // grid.fillOvalCenter(e.start.x_, e.start.y_, 4, 4);
      // grid.fillOvalCenter(e.end.x_, e.end.y_, 4, 4);
    }
    // grid.setColor(Color.GREEN);
    // for (BoxPoint e : points_) {
    // grid.fillOvalCenter(e.x_, e.y_, 4, 4);
    // }
    grid.setColor(Color.WHITE);
    grid.drawString("" + edges_.size(), 20, 20);
    grid.drawString("" + points_.size(), 40, 20);
  }
}
