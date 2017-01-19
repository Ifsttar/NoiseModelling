/*
 * NoiseMap is a scientific computation plugin for OrbisGIS developed in order to
 * evaluate the noise impact in urban areas. This model is
 * based on the French standard method NMPB2008. It includes traffic-to-noise
 * sources evaluation and sound propagation processing.
 *
 * This plugin is currently developed by the Environmental Acoustics Laboratory (LAE) of Ifsttar
 * (http://wwww.lae.ifsttar.fr/) in collaboration with the Lab-STICC CNRS laboratory.
 * It was initially developed as part of the Eval-PDU project, funded by the
 * French Agence Nationale de la Recherche (ANR) under contract ANR-08-VILL-0005-01.
 *
 * Noisemap is distributed under GPL 3 license. Its reference contact is Judicaël
 * Picaut <judicael.picaut@ifsttar.fr>. It is maintained by Nicolas Fortin
 * <nicolas.fortin@ifsttar.fr>
 *
 * Copyright (C) 2011-2016 IFSTTAR-CNRS
 *
 * Noisemap is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * Noisemap is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Noisemap. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information concerning NoiseM@p, please consult: <http://noisemap.orbisgis.org/>
 *
 * For more information concerning OrbisGis, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 *
 * info_at_ orbisgis.org
 */
package org.orbisgis.noisemap.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.WKTWriter;
import org.jdelaunay.delaunay.ConstrainedMesh;
import org.jdelaunay.delaunay.evaluator.InsertionEvaluator;
import org.jdelaunay.delaunay.geometries.DEdge;
import org.jdelaunay.delaunay.geometries.DPoint;
import org.jdelaunay.delaunay.geometries.DTriangle;
import org.jdelaunay.delaunay.error.DelaunayError;
import org.jdelaunay.delaunay.geometries.Element;

import com.vividsolutions.jts.algorithm.CGAlgorithms;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.CoordinateSequenceFilter;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Nicolas Fortin
 */
public class LayerJDelaunay implements LayerDelaunay {
    private static Logger logger = LoggerFactory.getLogger(LayerJDelaunay.class);
    private List<Coordinate> vertices = new ArrayList<Coordinate>();
    private ArrayList<DEdge> constraintEdge = new ArrayList<DEdge>();
    private LinkedList<DPoint> ptToInsert = new LinkedList<DPoint>();
    private HashMap<Integer,BuildingWithID> buildingWithID=new HashMap<Integer,BuildingWithID>();
    private boolean debugMode=false; //output primitives in a text file
    private boolean computeNeighbors=false;
    List<Triangle> triangles = new ArrayList<Triangle>();
    private List<Triangle> neighbors = new ArrayList<Triangle>(); // The first neighbor triangle is opposite the first corner of triangle  i
    HashMap<Integer, LinkedList<Integer>> hashOfArrayIndex = new HashMap<Integer, LinkedList<Integer>>();
    //triangletest is for JDeLaunayTriangleDirectionChange to test triangle direction
    private List<DTriangle> triangletest=new ArrayList<DTriangle>();
    private static GeometryFactory FACTORY = new GeometryFactory();
    private double maximumArea = 0;
    /** insertion point minimal distance, in meter */
    private static final double EPSILON_INSERTION_POINT = 1;


    private static DTriangle findTriByCoordinate(Coordinate pos,List<DTriangle> trilst) throws DelaunayError {
        DPoint pt;
        if(Double.isNaN(pos.z)) {
            pt=new DPoint(pos.x,pos.y,0);
        } else {
            pt=new DPoint(pos);
        }
        Element foundEl=trilst.get(trilst.size()/2).searchPointContainer(pt);
        if(foundEl instanceof DTriangle) {
            return (DTriangle)foundEl;
        }else{
            for(DTriangle tri : trilst) {
                if(tri.contains(pt)) {
                    return tri;
                }
            }
            return null;
        }
    }
    private static class SetZFilter implements CoordinateSequenceFilter {
        private boolean done = false;

        @Override
        public void filter(CoordinateSequence seq, int i) {
            double x = seq.getX(i);
            double y = seq.getY(i);
            double z = seq.getOrdinate(i, 2);
            seq.setOrdinate(i, 0, x);
            seq.setOrdinate(i, 1, y);
            if(Double.isNaN(z)){
                seq.setOrdinate(i, 2, 0);
            }
            else{
                seq.setOrdinate(i, 2, z);
            }
            if (i == seq.size()) {
                done = true;
            }
        }

        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public boolean isGeometryChanged() {
            return true;
        }
    }

    private static class BuildingWithID{
        private Polygon building;


        public BuildingWithID(Polygon building) {
            this.building=building;

        }

        public boolean isTriangleInBuilding(DPoint point)
        {
            return this.building.intersects(FACTORY.createPoint(point.getCoordinate()));
        }


    }
    private int getOrAppendVertices(Coordinate newCoord,
                                    List<Coordinate> vertices,
                                    HashMap<Integer, LinkedList<Integer>> hashOfArrayIndex) {
        // We can obtain the same hash with two different coordinate (4 Bytes or
        // 8 Bytes against 12 or 24 Bytes) , then we use a list as the value of
        // the hashmap
        // First step - Search the vertice parameter within the hashMap
        int newCoordIndex = -1;
        Integer coordinateHash = newCoord.hashCode();
        LinkedList<Integer> listOfIndex = hashOfArrayIndex.get(coordinateHash);
        if (listOfIndex != null) // There are the same hash value
        {
            for (int vind : listOfIndex) // Loop inside the coordinate index
            {
                if (newCoord.equals3D(vertices.get(vind))) // the coordinate is
                // equal to the
                // existing
                // coordinate
                {
                    newCoordIndex = vind;
                    break; // Exit for loop
                }
            }
            if (newCoordIndex == -1) {
                // No vertices has been found, we push the new coordinate into
                // the existing linked list
                newCoordIndex = vertices.size();
                listOfIndex.add(newCoordIndex);
                vertices.add(newCoord);
            }
        } else {
            // Push a new hash element
            listOfIndex = new LinkedList<Integer>();
            newCoordIndex = vertices.size();
            listOfIndex.add(newCoordIndex);
            vertices.add(newCoord);
            hashOfArrayIndex.put(coordinateHash, listOfIndex);
        }
        return newCoordIndex;
    }

    private ConstrainedMesh delaunayTool = null;

    @Override
    public void processDelaunay() throws LayerDelaunayError {
        processDelaunay(0., null);
    }

    /**
     *
     * @param minTriangleLength Minimum triangle side length
     * @param insertionEvaluator
     * @throws LayerDelaunayError
     */
    public void processDelaunay(double minTriangleLength, InsertionEvaluator insertionEvaluator) throws LayerDelaunayError {
        if (delaunayTool != null) {
            try {
                // Push segments
                delaunayTool.setPoints(ptToInsert);
                delaunayTool.setConstraintEdges(constraintEdge);

                if(debugMode) {
                        //Debug mode SQL script to do same triangulation
                        GeometryFactory factory = new GeometryFactory();
                        StringBuilder out = new StringBuilder();
                        out.append("DROP TABLE IF EXISTS DEBUG_DATA;\n");
                        out.append("CREATE TABLE DEBUG_DATA(the_geom geometry);\n");
                        WKTWriter wktWriter = new WKTWriter(3);
                        // write pts
                        for(DPoint pt : ptToInsert) {
                            Geometry geom = factory.createPoint(pt.getCoordinate());
                            out.append("INSERT INTO DEBUG_DATA VALUES ('");
                            out.append(wktWriter.write(geom));
                            out.append("');\n");
                        }
                        //write segments
                        for(DEdge edge : constraintEdge) {
                            DPoint pt=edge.getStartPoint();
                            DPoint pt2=edge.getEndPoint();
                            Geometry geom = factory.createLineString(new Coordinate[]{pt.getCoordinate(), pt2.getCoordinate()});
                            out.append("INSERT INTO DEBUG_DATA VALUES ('");
                            out.append(wktWriter.write(geom));
                            out.append("');\n");
                        }
                        logger.info(out.toString());
                }


                delaunayTool.forceConstraintIntegrity();
                delaunayTool.processDelaunay();
                // Refine mesh
                if(insertionEvaluator != null) {
                    delaunayTool.refineTriangles(minTriangleLength , insertionEvaluator);
                }
                constraintEdge.clear();
                ptToInsert.clear();
                List<DTriangle> trianglesDelaunay = delaunayTool
                        .getTriangleList();
                //this value is for the unit test
                triangletest=delaunayTool.getTriangleList();
                //triangles.ensureCapacity(trianglesDelaunay.size());// reserve
                // memory
                HashMap<Integer, Integer> gidToIndex = new HashMap<Integer, Integer>();
                ArrayList<Triangle> gidTriangle=new ArrayList<Triangle>(trianglesDelaunay.size());

                //Build ArrayList for binary search
                //test add height



                for (DTriangle triangle : trianglesDelaunay) {
                    Coordinate [] ring = new Coordinate [] {triangle.getPoint(0).getCoordinate(),triangle.getPoint(1).getCoordinate(),triangle.getPoint(2).getCoordinate(),triangle.getPoint(0).getCoordinate()};
                    boolean orientationReversed=false;
                    //if one of three vertices have buildingID and buildingID>=1
                    if(triangle.getPoint(0).getProperty()>=1||triangle.getPoint(1).getProperty()>=1||triangle.getPoint(2).getProperty()>=1){
                        int propertyBulidingID=0;
                        for(int i=0;i<=2;i++){
                            int potentialBuildingID=triangle.getPoint(i).getProperty();
                            if(potentialBuildingID>=1){
                                //get the Barycenter of the triangle so we can sure this point is in this triangle and we will check if the building contain this point

                                if(this.buildingWithID.get(potentialBuildingID).isTriangleInBuilding(triangle.getBarycenter())){
                                    propertyBulidingID=potentialBuildingID;
                                    break;
                                }

                            }

                        }
                        triangle.setProperty(propertyBulidingID);

                    }
                    //if there are less than 3 points have buildingID this triangle is out of building
                    else{
                        triangle.setProperty(0);
                    }


                    if(!CGAlgorithms.isCCW(ring)) {
                        Coordinate tmp= new Coordinate(ring[0]);
                        ring[0]=ring[2];
                        ring[2]=tmp;
                        orientationReversed=true;
                    }

                    int a = getOrAppendVertices(ring[0], vertices, hashOfArrayIndex);
                    int b = getOrAppendVertices(ring[1], vertices, hashOfArrayIndex);
                    int c = getOrAppendVertices(ring[2], vertices, hashOfArrayIndex);
                    int buildingID=triangle.getProperty();
                    triangles.add(new Triangle(a, b, c, buildingID));
                    if (this.computeNeighbors) {
                        Triangle gidTri = new Triangle(-1, -1, -1, 0);
                        for (int i = 0; i < 3; i++) {
                            DTriangle neighTriangle = triangle.getOppositeEdge(triangle.getPoint(i)).getOtherTriangle(triangle);
                            if (neighTriangle != null) {
                                if (neighTriangle.getPoint(0).getProperty() >= 1 || neighTriangle.getPoint(1).getProperty() >= 1 || neighTriangle.getPoint(2).getProperty() >= 1) {
                                    //if neighbor is in building
                                    int neighBuildingID = 0;
                                    for (int j = 0; j <= 2; j++) {
                                        int potentialNeighBuildingID = neighTriangle.getPoint(j).getProperty();
                                        if (potentialNeighBuildingID >= 1) {
                                            if (this.buildingWithID.get(potentialNeighBuildingID).isTriangleInBuilding(neighTriangle.getBarycenter())) {
                                                neighBuildingID = potentialNeighBuildingID;
                                                break;
                                            }
                                        }
                                    }
                                    neighTriangle.setProperty(neighBuildingID);
                                } else {
                                    neighTriangle.setProperty(0);
                                }
                                gidTri.set(i, neighTriangle.getGID());
                            }
                        }

                        if (!orientationReversed) {
                            gidTriangle.add(gidTri);
                        } else {
                            gidTriangle.add(new Triangle(gidTri.getC(), gidTri.getB(), gidTri.getA(), buildingID));
                        }
                        gidToIndex.put(triangle.getGID(), gidTriangle.size() - 1);
                    }







                }

                if(this.computeNeighbors) {
                    //Translate GID to local index
                    for(Triangle tri : gidTriangle) {
                        Triangle localTri=new Triangle(-1,-1,-1,0);
                        for(int i=0;i<3;i++) {
                            int index=tri.get(i);
                            if(index!=-1){
                                localTri.set(i, gidToIndex.get(index));
                            }

                        }

                        neighbors.add(localTri);
                    }
                }
                delaunayTool = null;

            } catch (DelaunayError e) {
                StringBuilder msgStack=new StringBuilder(e.getMessage());
                for(StackTraceElement lign : e.getStackTrace()) {
                    msgStack.append(lign.toString());
                    msgStack.append("\n");
                }
                throw new LayerDelaunayError(msgStack.toString());
            }
        }
    }
    @Override
    public void addPolygon(Polygon newPoly, boolean isEmpty)
            throws LayerDelaunayError {

        if (delaunayTool == null) {
            delaunayTool = new ConstrainedMesh();
        }

        // To avoid errors we set the Z coordinate to 0.
        SetZFilter zFilter = new SetZFilter();
        newPoly.apply(zFilter);
        GeometryFactory factory = new GeometryFactory();
        final Coordinate[] coordinates = newPoly.getExteriorRing()
                .getCoordinates();
        if (coordinates.length > 1) {
            LineString newLineString = factory.createLineString(coordinates);
            this.addLineString(newLineString);
        }
        // Append holes
        final int holeCount = newPoly.getNumInteriorRing();
        for (int holeIndex = 0; holeIndex < holeCount; holeIndex++) {
            LineString holeLine = newPoly.getInteriorRingN(holeIndex);
            // Convert hole into a polygon, then compute an interior point
            Polygon polyBuffnew = factory.createPolygon(
                    factory.createLinearRing(holeLine.getCoordinates()), null);
            if (polyBuffnew.getArea() > 0.) {
                Coordinate interiorPoint = polyBuffnew.getInteriorPoint()
                        .getCoordinate();
                if (!factory.createPoint(interiorPoint).intersects(holeLine)) {
                    this.addLineString(holeLine);
                } else {
                    logger.info("Warning : hole rejected, can't find interior point.");
                }
            } else {
                logger.info("Warning : hole rejected, area=0");
            }
        }
    }

    /**
     * Add height of building
     * @return
     */
    @Override
    public void addPolygon(Polygon newPoly, boolean isEmpty,int buildingId)
            throws LayerDelaunayError {

        if (delaunayTool == null) {
            delaunayTool = new ConstrainedMesh();
        }

        // To avoid errors we set the Z coordinate to 0.
        SetZFilter zFilter = new SetZFilter();
        newPoly.apply(zFilter);
        GeometryFactory factory = new GeometryFactory();
        final Coordinate[] coordinates = newPoly.getExteriorRing()
                .getCoordinates();
        if (coordinates.length > 1) {
            LineString newLineString = factory.createLineString(coordinates);
            this.addLineString(newLineString,buildingId);
            this.buildingWithID.put(buildingId, new BuildingWithID(newPoly));
        }
        // Append holes
        final int holeCount = newPoly.getNumInteriorRing();
        for (int holeIndex = 0; holeIndex < holeCount; holeIndex++) {
            LineString holeLine = newPoly.getInteriorRingN(holeIndex);
            // Convert hole into a polygon, then compute an interior point
            Polygon polyBuffnew = factory.createPolygon(
                    factory.createLinearRing(holeLine.getCoordinates()), null);
            if (polyBuffnew.getArea() > 0.) {
                Coordinate interiorPoint = polyBuffnew.getInteriorPoint()
                        .getCoordinate();
                if (!factory.createPoint(interiorPoint).intersects(holeLine)) {
                    this.addLineString(holeLine, buildingId);
                } else {
                    logger.info("Warning : hole rejected, can't find interior point.");
                }
            } else {
                logger.info("Warning : hole rejected, area=0");
            }
        }
    }
    @Override
    public void setMinAngle(Double minAngle) {
        // TODO Auto-generated method stub

    }

    @Override
    public void hintInit(Envelope bBox, long polygonCount, long verticesCount)
            throws LayerDelaunayError {
    }

    @Override
    public List<Coordinate> getVertices() throws LayerDelaunayError {
        return this.vertices;
    }

    @Override
    public List<Triangle> getTriangles() throws LayerDelaunayError {
        return this.triangles;
    }

    @Override
    public void addVertex(Coordinate vertexCoordinate)
            throws LayerDelaunayError {
        this.getOrAppendVertices(vertexCoordinate, vertices, hashOfArrayIndex);
    }

    @Override
    public void setMaxArea(Double maxArea) throws LayerDelaunayError {
        maximumArea = Math.max(0, maxArea);
    }

    @Override
    public void addLineString(LineString lineToProcess)
            throws LayerDelaunayError {
        Coordinate[] coords = lineToProcess.getCoordinates();
        try {
            for (int ind = 1; ind < coords.length; ind++) {
                this.constraintEdge.add(new DEdge(new DPoint(coords[ind - 1]),
                        new DPoint(coords[ind])));
            }
        } catch (DelaunayError e) {
            throw new LayerDelaunayError(e.getMessage());
        }
    }

    //add buildingID to edge property and to points property
    public void addLineString(LineString lineToProcess,int buildingID)
            throws LayerDelaunayError {
        Coordinate[] coords = lineToProcess.getCoordinates();
        try {
            for (int ind = 1; ind < coords.length; ind++) {
                DPoint point1=new DPoint(coords[ind - 1]);
                DPoint point2=new DPoint(coords[ind]);
                point1.setProperty(buildingID);
                point2.setProperty(buildingID);
                DEdge edge=new DEdge(point1,
                        point2);
                edge.setProperty(buildingID);
                this.constraintEdge.add(edge);
            }
        } catch (DelaunayError e) {
            throw new LayerDelaunayError(e.getMessage());
        }
    }
    //add buildingID to edge property and to points property

    public void addTopoPoint(Coordinate point)
            throws LayerDelaunayError{
        try{
            DPoint topoPoint=new DPoint(point);
            topoPoint.setProperty(0);
            this.ptToInsert.add(topoPoint);
        }catch (DelaunayError e) {
            throw new LayerDelaunayError(e.getMessage());
        }
    }

    @Override
    public void reset() {
        // TODO Auto-generated method stub

    }

    @Override
    public List<Triangle> getNeighbors() throws LayerDelaunayError {
        if(computeNeighbors) {
            return neighbors;
        } else {
            throw new LayerDelaunayError("You must call setRetrieveNeighbors(True) before process delaunay triangulation");
        }
    }

    @Override
    public void setRetrieveNeighbors(boolean retrieve) {
        this.computeNeighbors=retrieve;

    }


    public List<DTriangle> gettriangletest() {
        return this.triangletest;

    }

    /**
     * Hold a DTriangle and a DPoint
     */
    private static class PointTriangleTuple {
        private DTriangle triangle;
        private DPoint point;

        private PointTriangleTuple(DTriangle triangle, DPoint point) {
            this.triangle = triangle;
            this.point = point;
        }

        /**
         * @return DTriangle instance
         */
        public DTriangle getTriangle() {
            return triangle;
        }

        /**
         * @return DPoint instance
         */
        public DPoint getPoint() {
            return point;
        }
    }

}
