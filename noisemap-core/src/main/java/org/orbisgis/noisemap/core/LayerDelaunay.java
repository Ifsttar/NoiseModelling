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

import java.util.List;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;

/*
 * This interface aims to link the acoustic module with many delaunay library,
 * to easy switch between libraries
 * @author Nicolas Fortin
 */
public interface LayerDelaunay {
	/**
	 * This optional method give an hint of the size of the delaunay process.
	 * Call this method before the first call of addPolygon This method is used
	 * only for optimization.
	 * 
	 * @param[in] boundingBox Bounding box of the delaunay mesh
	 * @param[in] polygonCount Size of the polygon count
	 * @warning The precision of the parameters value is not required, this is
	 *          only an hint.
	 */
	void hintInit(Envelope boundingBox, long polygonCount, long verticesCount)
			throws LayerDelaunayError;

	/**
	 * Append a polygon into the triangulation
	 * 
	 * @param[in] newPoly Polygon to append into the mesh, internal rings will
	 *            be inserted as holes.
	 * @param[in] isEmpty This polygon is a hole. If yes, only the external ring
	 *            is used.
	 */
	void addPolygon(Polygon newPoly, boolean isEmpty) throws LayerDelaunayError;

    /**
     * Append a polygon into the triangulation
     *
     * @param[in] newPoly Polygon to append into the mesh, internal rings will
     *            be inserted as holes.
     * @param[in] isEmpty This polygon is a hole. If yes, only the external ring
     *            is used.
     * @param[in] attribute Polygon attribute. {@link Triangle#getBuidlingID()}
     */
    void addPolygon(Polygon newPoly, boolean isEmpty,int attribute) throws LayerDelaunayError;

	/**
	 * Append a vertex into the triangulation
	 * 
	 * @param[in] vertexCoordinate Coordinate of the new vertex
	 */
	void addVertex(Coordinate vertexCoordinate) throws LayerDelaunayError;

	/**
	 * Append a LineString into the triangulation
	 * 
	 * @param[in] a Coordinate of the segment start
	 * @param[in] b Coordinate of the segment end
	 */
	void addLineString(LineString line) throws LayerDelaunayError;

	/**
	 * Set the minimum angle, if you wish to enforce the quality of the delaunay
	 * Call processDelauney after to take account of this method.
	 * 
	 * @param[in] minAngle Minimum angle in radiant
	 */
	void setMinAngle(Double minAngle) throws LayerDelaunayError;

	/**
	 * Set the maximum area in m² Call processDelauney after to take account of
	 * this method.
	 * 
	 * @param[in] maxArea Maximum area in m²
	 */
	void setMaxArea(Double maxArea) throws LayerDelaunayError;

	/**
	 * Launch delaunay process
	 */
	void processDelaunay() throws LayerDelaunayError;

	/**
	 * When the processDelaunay has been called, retrieve results vertices
	 */
	List<Coordinate> getVertices() throws LayerDelaunayError;

	/**
	 * When the processDelaunay has been called, retrieve results Triangle link
	 * unique vertices by their index.
	 */
	List<Triangle> getTriangles() throws LayerDelaunayError;
	/**
	 * When the processDelaunay has been called, retrieve results Triangle link
	 * triangles neighbor by their index.
	 */
	List<Triangle> getNeighbors() throws LayerDelaunayError;
	/**
	 * Remove all data, come back to the constructor state
	 */
	void reset();
	/**
	 * Enable or Disable the collecting of triangles neighboring data.
	 * @param retrieve
	 */
	public void setRetrieveNeighbors(boolean retrieve);
}
