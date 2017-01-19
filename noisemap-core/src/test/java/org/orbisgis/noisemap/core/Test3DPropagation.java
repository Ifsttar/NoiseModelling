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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedList;
import java.util.Stack;
import static junit.framework.Assert.assertFalse;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;


import junit.framework.TestCase;
import org.junit.Test;
import org.omg.CORBA.PUBLIC_MEMBER;

/**
 *
 * @author SU Qi
 */
public class Test3DPropagation extends TestCase{

    public void testChangePlan() {
        GeometryFactory factory = new GeometryFactory();
        List<Coordinate> coords = JTSUtility.getNewCoordinateSystem(Arrays.asList(new Coordinate(5, 5, 5), new Coordinate(10, 5, 6)));
        List<Coordinate> coordsInv = JTSUtility.getNewCoordinateSystem(Arrays.asList(new Coordinate(10, 5, 6), new Coordinate(5, 5, 5)));
        assertEquals(coords.get(0).y, coordsInv.get(1).y);
        assertEquals(factory.createLineString(coords.toArray(new Coordinate[coords.size()])).getLength(),
                factory.createLineString(coordsInv.toArray(new Coordinate[coordsInv.size()])).getLength(), 1e-12);
        coords = JTSUtility.getNewCoordinateSystem(Arrays.asList(new Coordinate(5, 5, 5), new Coordinate(6, 5, 5.5), new Coordinate(10, 5, 6)));
        coordsInv = JTSUtility.getNewCoordinateSystem(Arrays.asList(new Coordinate(10, 5, 6), new Coordinate(6, 5, 5.5), new Coordinate(5, 5, 5)));
        assertEquals(factory.createLineString(coords.toArray(new Coordinate[coords.size()])).getLength(),
                factory.createLineString(coordsInv.toArray(new Coordinate[coordsInv.size()])).getLength(), 1e-12);
    }

    public void testLinearRegression() {
        double ab[] = JTSUtility.getLinearRegressionPolyline(JTSUtility.getNewCoordinateSystem(Arrays.asList(new Coordinate(5, 5, 5),
                new Coordinate(10, 5, 5))));
        assertArrayEquals(new double[]{0, 5}, ab, 1e-12);
        ab = JTSUtility.getLinearRegressionPolyline(JTSUtility.getNewCoordinateSystem(Arrays.asList(new Coordinate(10, 5, 5),
                new Coordinate(5, 5, 5))));
        assertArrayEquals(new double[]{0, 5}, ab, 1e-12);
        ab = JTSUtility.getLinearRegressionPolyline(JTSUtility.getNewCoordinateSystem(Arrays.asList(new Coordinate(5, 5, 5),
                new Coordinate(10, 5, 10))));
        assertArrayEquals(new double[]{1, 5}, ab, 1e-12);
        List<Coordinate> sample = Arrays.asList(new Coordinate(15, 10, 1.5), new Coordinate(17.5, 10, 1.8),
                new Coordinate(20, 10, 2),new Coordinate(22.5, 10, 2), new Coordinate(25, 10, 2.5));
        List<Coordinate> localSample = JTSUtility.getNewCoordinateSystem(sample);
        ab = JTSUtility.getLinearRegressionPolyline(localSample);
        final double funcError = 0.17;
        for (Coordinate aSample : localSample) {
            assertEquals(aSample.y, aSample.x * ab[0] + ab[1], funcError);
        }
    }
}
