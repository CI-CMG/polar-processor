package edu.colorado.cires.cmg.polarprocessor;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.math3.util.Precision;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;
import org.locationtech.spatial4j.shape.jts.JtsGeometry;

/**
 * Splits JTS geometries across the meridian and anti-meridian near the Earth’s poles
 */
public class PolarProcessor {


  private static final int PRECISION = 11;

  private static Coordinate preSplitTranslateCoordinate(Coordinate coordinate, boolean arctic) {

    double originalY = coordinate.getY();

    double distance;
    if (arctic) {
      distance = 90D - originalY;
    } else {
      distance = 90D + originalY;
    }

    double angle = coordinate.getX() + 90D;
    if(angle > 180D) {
      angle = angle - 360D;
    }

    double x = distance * Math.cos(Math.toRadians(angle)) + 180D;
    if (x > 180) {
      x = x - 360D;
    }
    double y = distance * Math.sin(Math.toRadians(angle));

    return new Coordinate(Precision.round(x, PRECISION), Precision.round(y, PRECISION));
  }


  private static Coordinate postSplitTranslateCoordinate(Coordinate coordinate, boolean arctic) {
    double originalX = coordinate.getX();
    double x = coordinate.getX();
    double y = coordinate.getY();

    x = x + 180D;

    if (x > 180D) {
      x = x - 360D;
    }

    double distance = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
    double angle = Math.toDegrees(Math.atan2(y, x)) - 90D;

    if (angle > 180D) {
      angle = angle - 360D;
    }
    if (angle < -180D) {
      angle = angle + 360D;
    }

    if (distance == 0D) {
      angle = 180D;
    }

    if (Math.abs(angle) == 180D) {
      angle = 180D;
      if (originalX < 0D) {
        angle = angle * -1;
      }
    }

    if (arctic) {
      distance = 90D - distance;
    } else {
      distance = distance - 90D;
    }

    return new Coordinate(Precision.round(angle, PRECISION), Precision.round(distance, PRECISION));
  }

  private static LinearRing preSplitTranslateLineString(LinearRing lineString, GeometryFactory geometryFactory, boolean arctic) {
    Coordinate[] coordinates = new Coordinate[lineString.getNumPoints()];
    for (int n = 0; n < lineString.getNumPoints(); n++) {
      coordinates[n] = preSplitTranslateCoordinate(lineString.getCoordinateN(n), arctic);
    }
    return geometryFactory.createLinearRing(coordinates);
  }

  private static Coordinate[] postSplitTranslateCoordinateArray(LineString lineString, boolean arctic) {
    Coordinate[] coordinates = new Coordinate[lineString.getNumPoints()];
    for (int n = 0; n < lineString.getNumPoints(); n++) {
      coordinates[n] = postSplitTranslateCoordinate(lineString.getCoordinateN(n), arctic);
    }
    return coordinates;
  }

  private static LineString postSplitTranslateLineString(LineString lineString, GeometryFactory geometryFactory, boolean arctic) {
    return geometryFactory.createLineString(postSplitTranslateCoordinateArray(lineString, arctic));
  }

  private static LinearRing postSplitTranslateLinearRing(LineString lineString, GeometryFactory geometryFactory, boolean arctic) {
    return geometryFactory.createLinearRing(postSplitTranslateCoordinateArray(lineString, arctic));
  }

  private static Polygon postSplitTranslatePolygon(Polygon geometry, GeometryFactory geometryFactory, boolean arctic) {
    LinearRing[] holes = new LinearRing[geometry.getNumInteriorRing()];
    for (int n = 0; n < geometry.getNumInteriorRing(); n++) {
      holes[n] = postSplitTranslateLinearRing(geometry.getInteriorRingN(n), geometryFactory, arctic);
    }
    return geometryFactory.createPolygon(postSplitTranslateLinearRing(geometry.getExteriorRing(), geometryFactory, arctic), holes);
  }

  /**
   * Translates {@link Polygon} coordinates before splitting
   * @param polygon {@link Polygon} to translate
   * @param geometryFactory {@link GeometryFactory} for creating translated polygons
   * @param arctic whether {@link Polygon} lies within arctic circle
   * @return translated {@link Polygon}
   */
  static Polygon preSplitTranslatePolygon(Polygon polygon, GeometryFactory geometryFactory, boolean arctic) {
    LinearRing[] holes = new LinearRing[polygon.getNumInteriorRing()];
    for (int n = 0; n < polygon.getNumInteriorRing(); n++) {
      holes[n] = preSplitTranslateLineString(polygon.getInteriorRingN(n), geometryFactory, arctic);
    }
    return geometryFactory.createPolygon(preSplitTranslateLineString(polygon.getExteriorRing(), geometryFactory, arctic), holes);
  }

  /**
   * Splits polygon across anti-meridian
   * @param translated {@link Polygon} to split
   * @return {@link Geometry} split across the anti-meridian
   */
  static Geometry split180(Polygon translated) {
    JtsGeometry jtsGeometry = new JtsGeometry(translated, JtsSpatialContext.GEO, true, false);
    return jtsGeometry.getGeom();
  }

  private static Coordinate[] addPolarCoordinateCoordinateArray(LineString lineString, GeometryFactory geometryFactory) {
    LinkedList<Coordinate> coordinates = new LinkedList<>();
    for (int n = 0; n < lineString.getNumPoints(); n++) {
      Coordinate coordinate = lineString.getCoordinateN(n);
      if (coordinates.isEmpty()) {
        coordinates.add(coordinate);
      } else {
        Point neg = geometryFactory.createPoint(new Coordinate(-180D, 0D));
        Point pos = geometryFactory.createPoint(new Coordinate(180D, 0D));
        LineString seg = geometryFactory.createLineString(new Coordinate[]{coordinates.getLast(), coordinate});
        if (seg.intersects(neg)) {
          coordinates.add(neg.getCoordinate());
        } else if (seg.intersects(pos)) {
          coordinates.add(pos.getCoordinate());
        }
        coordinates.add(coordinate);
      }
    }
    return coordinates.toArray(new Coordinate[0]);
  }

  private static LineString addPolarCoordinateLineString(LineString lineString, GeometryFactory geometryFactory) {
    return geometryFactory.createLineString(addPolarCoordinateCoordinateArray(lineString, geometryFactory));
  }

  private static LinearRing addPolarCoordinateLinearRing(LineString lineString, GeometryFactory geometryFactory) {
    return geometryFactory.createLinearRing(addPolarCoordinateCoordinateArray(lineString, geometryFactory));
  }

  private static LinearRing toRing(LineString lineString, GeometryFactory geometryFactory) {
    Coordinate[] coordinates = new Coordinate[lineString.getNumPoints()];
    for (int n = 0; n < lineString.getNumPoints(); n++) {
      coordinates[n] = lineString.getCoordinateN(n);
    }
    return geometryFactory.createLinearRing(coordinates);
  }

  private static Polygon addPolarCoordinatePolygon(Polygon polygon, GeometryFactory geometryFactory) {
    LineString ext = polygon.getExteriorRing();
    LinearRing ring = addPolarCoordinateLinearRing(ext, geometryFactory);
    LinearRing[] holes = new LinearRing[polygon.getNumInteriorRing()];
    for (int n = 0; n < polygon.getNumInteriorRing(); n++) {
      holes[n] = toRing(polygon.getInteriorRingN(n), geometryFactory);
    }
    return geometryFactory.createPolygon(ring, holes);
  }

  private static MultiPolygon addPolarCoordinateMultiPolygon(MultiPolygon geometry, GeometryFactory geometryFactory) {
    Polygon[] polygons = new Polygon[geometry.getNumGeometries()];
    for (int n = 0; n < geometry.getNumGeometries(); n++) {
      polygons[n] = addPolarCoordinatePolygon((Polygon) geometry.getGeometryN(n), geometryFactory);
    }
    return geometryFactory.createMultiPolygon(polygons);
  }

  /**
   * Generates {@link Geometry} with polar coordinates
   * @param geometry {@link Geometry} without polar coordinates
   * @param geometryFactory {@link GeometryFactory} for generating translated {@link Geometry}
   * @return {@link Geometry} with polar coordinates
   */
  static Geometry addPolarCoordinate(Geometry geometry, GeometryFactory geometryFactory) {
    if (geometry instanceof Polygon) {
      return addPolarCoordinatePolygon((Polygon) geometry, geometryFactory);
    } else if (geometry instanceof MultiPolygon) {
      return addPolarCoordinateMultiPolygon((MultiPolygon) geometry, geometryFactory);
    } else if (geometry instanceof LineString) {
      return addPolarCoordinateLineString((LineString) geometry, geometryFactory);
    } else if (geometry instanceof MultiLineString) {
      MultiLineString collection = (MultiLineString) geometry;
      LineString[] geometries = new LineString[collection.getNumGeometries()];
      for (int n = 0; n < collection.getNumGeometries(); n++) {
        geometries[n] = addPolarCoordinateLineString((LineString) collection.getGeometryN(n), geometryFactory);
      }
      return geometryFactory.createMultiLineString(geometries);
    } else if (geometry instanceof Point) {
      return geometry;
    } else if (geometry instanceof GeometryCollection) {
      GeometryCollection collection = (GeometryCollection) geometry;
      Geometry[] geometries = new Geometry[collection.getNumGeometries()];
      for (int n = 0; n < collection.getNumGeometries(); n++) {
        geometries[n] = addPolarCoordinate(collection.getGeometryN(n), geometryFactory);
      }
      return geometryFactory.createGeometryCollection(geometries);
    } else {
      throw new IllegalStateException("Unsupported geometry: " + geometry);
    }
  }

  /**
   * Translates {@link Polygon} after splitting
   * @param geometry Split {@link Geometry}
   * @param geometryFactory {@link GeometryFactory} for generating translated {@link Geometry}
   * @param arctic whether {@link Geometry} lies within arctic circle
   * @return translated {@link Geometry}
   */
  static Geometry postSplitTranslateGeometry(Geometry geometry, GeometryFactory geometryFactory, boolean arctic) {
    if (geometry instanceof Polygon) {
      return postSplitTranslatePolygon((Polygon) geometry, geometryFactory, arctic);
    } else if (geometry instanceof MultiPolygon) {
      return postSplitTranslateMultiPolygon((MultiPolygon) geometry, geometryFactory, arctic);
    } else if (geometry instanceof LineString) {
      return postSplitTranslateLineString((LineString) geometry, geometryFactory, arctic);
    } else if (geometry instanceof MultiLineString) {
      MultiLineString collection = (MultiLineString) geometry;
      LineString[] geometries = new LineString[collection.getNumGeometries()];
      for (int n = 0; n < collection.getNumGeometries(); n++) {
        geometries[n] = postSplitTranslateLineString((LineString) collection.getGeometryN(n), geometryFactory, arctic);
      }
      return geometryFactory.createMultiLineString(geometries);
    } else if (geometry instanceof Point) {
      return geometryFactory.createPoint(postSplitTranslateCoordinate(geometry.getCoordinate(), arctic));
    } else if (geometry instanceof GeometryCollection) {
      GeometryCollection collection = (GeometryCollection) geometry;
      Geometry[] geometries = new Geometry[collection.getNumGeometries()];
      for (int n = 0; n < collection.getNumGeometries(); n++) {
        geometries[n] = postSplitTranslateGeometry(collection.getGeometryN(n), geometryFactory, arctic);
      }
      return geometryFactory.createGeometryCollection(geometries);
    } else {
      throw new IllegalStateException("Unsupported geometry: " + geometry);
    }
  }

  private static MultiPolygon postSplitTranslateMultiPolygon(MultiPolygon geometry, GeometryFactory geometryFactory, boolean arctic) {
    Polygon[] polygons = new Polygon[geometry.getNumGeometries()];
    for (int n = 0; n < geometry.getNumGeometries(); n++) {
      polygons[n] = postSplitTranslatePolygon((Polygon) geometry.getGeometryN(n), geometryFactory, arctic);
    }
    return geometryFactory.createMultiPolygon(polygons);
  }

  /**
   * Splits JTS geometries across the meridian and anti-meridian near the Earth’s poles
   * @param polygon {@link Polygon} to split
   * @param geometryFactory {@link Geometry} for generating split components of polygon
   * @return {@link Geometry} if splitting {@link Polygon} produces results different from original {@link Polygon}
   */
  public static Optional<Geometry> splitPolar(Polygon polygon, GeometryFactory geometryFactory) {
    boolean arctic = isArctic(polygon);
    return isPolar(polygon, geometryFactory)
        .map(preSplit -> postSplitTranslateGeometry(addPolarCoordinate(split180(preSplit), geometryFactory), geometryFactory, arctic));
  }

  private static boolean isArctic(Polygon polygon) {
    Envelope envelope = polygon.getEnvelopeInternal();
    double nDist = 90D - envelope.getMaxY();
    double sDist = 90D + envelope.getMinY();
    return nDist <= sDist;
  }

  private static Coordinate shiftCoordinate(Coordinate c) {
    c = c.copy();
    double x = c.getX();
    x = x + 180D;
    if (x > 180D) {
      x = x - 360D;
    }
    c.setX(x);
    return c;
  }

  private static Optional<Polygon> isPolar(Polygon polygon, GeometryFactory geometryFactory) {

    polygon = preSplitTranslatePolygon(polygon, geometryFactory, isArctic(polygon));

    Set<Coordinate> crossings = new HashSet<>();
    LineString mask = geometryFactory.createLineString(new Coordinate[]{new Coordinate(0D, 0D), new Coordinate(0D, 360D)});
    Point origin = geometryFactory.createPoint(new Coordinate(0D, 0D));

    LineString ext = polygon.getExteriorRing();
    for (int i = 0; i < ext.getNumPoints(); i++) {
      Coordinate c = shiftCoordinate(ext.getCoordinateN(i));
      if (c.getX() == 0D && c.getY() == 0D) {
        return Optional.empty();  //If polygon contains the pole, it should already be split.
      }
      if (i == 0) {
        continue;
      }
      Coordinate c1 = shiftCoordinate(ext.getCoordinateN(i - 1));
      LineString segment = geometryFactory.createLineString(new Coordinate[]{c1, c});
      if(segment.intersects(origin)) {
        return Optional.empty();  //If polygon contains the pole, it should already be split.
      }
      Geometry crossing = segment.intersection(mask);
      if(crossing instanceof Point) {
        crossings.add(crossing.getCoordinate());
      }

    }
    if (crossings.size() % 2 == 1) {
      return Optional.of(polygon);
    }
    return Optional.empty();
  }

}

