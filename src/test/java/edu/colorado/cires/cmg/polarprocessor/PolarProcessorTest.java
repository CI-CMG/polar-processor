package edu.colorado.cires.cmg.polarprocessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.WKTReader;

public class PolarProcessorTest {

  @Test
  public void testPreSplitTranslatePolygonRing() throws Exception {
    GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    WKTReader wktReader = new WKTReader(geometryFactory);
    String wkt = "POLYGON ((180 70, 90 70, 0 70, -90 70, 180 70))";
    Polygon result = PolarProcessor.preSplitTranslatePolygon((Polygon) wktReader.read(wkt), geometryFactory, true);
    String expected = "POLYGON ((180 -20, 160 0, 180 20, -160 0, 180 -20))";
    assertEquals(expected, result.toString());
  }

  @Test
  public void testSplit180Ring() throws Exception {
    GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    WKTReader wktReader = new WKTReader(geometryFactory);
    String wkt = "POLYGON ((180 -20, 160 0, 180 20, -160 0, 180 -20))";
    Geometry result = PolarProcessor.split180((Polygon) wktReader.read(wkt));
    String expected = "MULTIPOLYGON (((-180 -20, -180 20, -160 0, -180 -20)), ((180 20, 180 -20, 160 0, 180 20)))";
    assertEquals(expected, result.toString());
  }

  @Test
  public void testAddPolarCoordinateRing() throws Exception {
    GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    WKTReader wktReader = new WKTReader(geometryFactory);
    String wkt = "MULTIPOLYGON (((-180 -20, -180 20, -160 0, -180 -20)), ((180 20, 180 -20, 160 0, 180 20)))";
    Geometry result = PolarProcessor.addPolarCoordinate((MultiPolygon) wktReader.read(wkt), geometryFactory);
    String expected = "MULTIPOLYGON (((-180 -20, -180 0, -180 20, -160 0, -180 -20)), ((180 20, 180 0, 180 -20, 160 0, 180 20)))";
    assertEquals(expected, result.toString());
  }

  @Test
  public void testPostSplitTransformRing() throws Exception {
    GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    WKTReader wktReader = new WKTReader(geometryFactory);
    String wkt = "MULTIPOLYGON (((-180 -20, -180 0, -180 20, -160 0, -180 -20)), ((180 20, 180 0, 180 -20, 160 0, 180 20)))";
    Geometry result = PolarProcessor.postSplitTranslateGeometry(wktReader.read(wkt), geometryFactory, true);
    String expected = "MULTIPOLYGON (((-180 70, -180 90, 0 70, -90 70, -180 70)), ((0 70, 180 90, 180 70, 90 70, 0 70)))";
    assertEquals(expected, result.toString());
  }


  @Test
  public void testSplitPolar() throws Exception {
    GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    WKTReader wktReader = new WKTReader(geometryFactory);
    String wkt = "POLYGON ((180 70, 100 60, 45 50, -30 50, -90 70, 180 70))";
    Geometry result = PolarProcessor.splitPolar((Polygon) wktReader.read(wkt), geometryFactory).get();
    String expected = "MULTIPOLYGON (((0 57.99203380037, 180 90, 180 70, 100 60, 45 50, 0 57.99203380037)), ((-180 70, -180 90, 0 57.99203380037, -30 50, -90 70, -180 70)))";
    assertEquals(expected, result.toString());
  }

  @Test
  public void testSplitPolarRing() throws Exception {
    GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    WKTReader wktReader = new WKTReader(geometryFactory);
    String wkt = "POLYGON ((180 70, 90 70, 0 70, -90 70, 180 70))";
    Geometry result = PolarProcessor.splitPolar((Polygon) wktReader.read(wkt), geometryFactory).get();
    String expected = "MULTIPOLYGON (((-180 70, -180 90, 0 70, -90 70, -180 70)), ((0 70, 180 90, 180 70, 90 70, 0 70)))";
    assertEquals(expected, result.toString());
  }

  @Test
  public void testSplitPolarAntarcticRing() throws Exception {
    GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    WKTReader wktReader = new WKTReader(geometryFactory);
    String wkt = "POLYGON ((180 -70, 90 -70, 0 -70, -90 -70, 180 -70))";
    Geometry result = PolarProcessor.splitPolar((Polygon) wktReader.read(wkt), geometryFactory).get();
    String expected = "MULTIPOLYGON (((-180 -70, -180 -90, 0 -70, -90 -70, -180 -70)), ((0 -70, 180 -90, 180 -70, 90 -70, 0 -70)))";
    assertEquals(expected, result.toString());
  }


  @Test
  public void testSplitPolarAntarctic() throws Exception {
    GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    WKTReader wktReader = new WKTReader(geometryFactory);
    String wkt = "POLYGON ((0 -70, -80 -60, -160 -50, 150 -50, 75 -70, 0 -70))";
    Geometry result = PolarProcessor.splitPolar((Polygon) wktReader.read(wkt), geometryFactory).get();
    String expected = "MULTIPOLYGON (((-180 -53.6092105781, -180 -90, 0 -70, -80 -60, -160 -50, -180 -53.6092105781)), ((0 -70, 180 -90, 180 -53.6092105781, 150 -50, 75 -70, 0 -70)))";
    assertEquals(expected, result.toString());
  }

  @Test
  public void testNotPolar() throws Exception {
    GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    WKTReader wktReader = new WKTReader(geometryFactory);
    String wkt = "POLYGON ((0 60, -80 60, -80 70, 0 70, 90 70, 180 70, -110 70, -110 60, 180 60, 90 60, 0 60))";
    assertFalse(PolarProcessor.splitPolar((Polygon) wktReader.read(wkt), geometryFactory).isPresent());
  }

  @Test
  public void testNotPolarAntarctic() throws Exception {
    GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    WKTReader wktReader = new WKTReader(geometryFactory);
    String wkt = "POLYGON ((0 -60, -80 -60, -80 -70, 0 -70, 90 -70, 180 -70, -110 -70, -110 -60, 180 -60, 90 -60, 0 -60))";
    assertFalse(PolarProcessor.splitPolar((Polygon) wktReader.read(wkt), geometryFactory).isPresent());
  }

  @Test
  public void testNotPolarOnPole() throws Exception {
    GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    WKTReader wktReader = new WKTReader(geometryFactory);
    String wkt = "POLYGON ((0 60, 180 60, 90 60, 0 60))";
    assertFalse(PolarProcessor.splitPolar((Polygon) wktReader.read(wkt), geometryFactory).isPresent());
    wkt = "POLYGON ((0 60, 180 60, -90 60, 0 60))";
    assertFalse(PolarProcessor.splitPolar((Polygon) wktReader.read(wkt), geometryFactory).isPresent());
    wkt = "POLYGON ((90 60, -90 60, 180 60, 90 60))";
    assertFalse(PolarProcessor.splitPolar((Polygon) wktReader.read(wkt), geometryFactory).isPresent());
    wkt = "POLYGON ((90 60, -90 60, 0 60, 90 60))";
    assertFalse(PolarProcessor.splitPolar((Polygon) wktReader.read(wkt), geometryFactory).isPresent());
  }

  @Test
  public void testNotPolarOnPoleAntarctic() throws Exception {
    GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    WKTReader wktReader = new WKTReader(geometryFactory);
    String wkt = "POLYGON ((0 -60, 180 -60, 90 -60, 0 -60))";
    assertFalse(PolarProcessor.splitPolar((Polygon) wktReader.read(wkt), geometryFactory).isPresent());
    wkt = "POLYGON ((0 -60, 180 -60, -90 -60, 0 -60))";
    assertFalse(PolarProcessor.splitPolar((Polygon) wktReader.read(wkt), geometryFactory).isPresent());
    wkt = "POLYGON ((90 -60, -90 -60, 180 -60, 90 -60))";
    assertFalse(PolarProcessor.splitPolar((Polygon) wktReader.read(wkt), geometryFactory).isPresent());
    wkt = "POLYGON ((90 -60, -90 -60, 0 -60, 90 -60))";
    assertFalse(PolarProcessor.splitPolar((Polygon) wktReader.read(wkt), geometryFactory).isPresent());
  }


}
