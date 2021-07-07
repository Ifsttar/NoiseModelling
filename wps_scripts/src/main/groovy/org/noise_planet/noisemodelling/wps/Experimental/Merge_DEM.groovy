/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by the DECIDE team from the Lab-STICC (CNRS) and by the Mixt Research Unit in Environmental Acoustics (Université Gustave Eiffel).
 * <http://noise-planet.org/noisemodelling.html>
 *
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 *
 * Contact: contact@noise-planet.org
 *
 */

/**
 * @Author Pierre Aumond, Université Gustave Eiffel
 */


package org.noise_planet.noisemodelling.wps.Experimental

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.Sql
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.SFSUtilities
import org.h2gis.utilities.TableLocation
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection

title = 'Merge your DEM with polygons, lines or force points'
description = 'Merge your DEM with polygons, lines or force points. This script removes points from your DEM that conflict with your forcing.'

inputs = [
        forceLinesPoints: [
                name       : 'Polygons, lines or force points',
                title      : 'Polygons, lines or force points',
                description: 'Polygons, lines or force points ((MULTI)POINT, (MULTI)LINESTRING or (MULTI)POLYGON)',
                type       : String.class
        ],
        dem : [
                name       : 'DEM',
                title: 'DEM - Digital Elevation Model compatible with NoiseModelling (X,Y,Z).',
                description: 'mnt',
                type       : String.class
        ],
        width : [
                name       : 'Buffer width',
                title: 'Buffer width',
                description: 'Width of your buffer to extend the area of influence of your polygons, lines or force points on the DEM. (Double)',
                type       : Double.class
        ],
        resolution : [
                name       : 'Resolution',
                title: 'Resolution',
                description: 'Resolution of the DEM that will be created around your polygons, lines or force points on the DEM. We advise to set the resolution to half of the resolution of your main DEM. (Double)',
                type       : Double.class
        ]

]

outputs = [
        result: [
                name       : 'Result output string',
                title      : 'Result output string',
                description: 'This type of result does not allow the blocks to be linked together.',
                type       : String.class
        ]
]

// Open Connection to Geoserver
static Connection openGeoserverDataStoreConnection(String dbName) {
    if (dbName == null || dbName.isEmpty()) {
        dbName = new GeoServer().catalog.getStoreNames().get(0)
    }
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}

// run the script
def run(input) {

    // Get name of the database
    // by default an embedded h2gis database is created
    // Advanced user can replace this database for a postGis or h2Gis server database.
    String dbName = "h2gisdb"

    // Open connection
    openGeoserverDataStoreConnection(dbName).withCloseable {
        Connection connection ->
            return [result: exec(connection, input)]
    }
}

// main function of the script
def exec(Connection connection, input) {

    // output string, the information given back to the user
    String resultString = null

    // Create a logger to display messages in the geoserver logs and in the command prompt.
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")

    // print to command window
    logger.info('Start : Merge DEM')
    logger.info("inputs {}", input) // log inputs of the run

    // import screen_table_name
    String forceLinesPoints = input['forceLinesPoints']
    // do it case-insensitive
    forceLinesPoints = forceLinesPoints.toUpperCase()

    // import screen_table_name
    String dem = input['dem']
    // do it case-insensitive
    dem = dem.toUpperCase()

    // Create a sql connection to interact with the database in SQL
    Sql sql = new Sql(connection)

    // Make sure that the screens do not cross each other with a 50 cm buffer
    double buffer = input['width']
    double resolution = input['resolution']

    //get SRID of the table
    int sridForceLinesPoints = SFSUtilities.getSRID(connection, TableLocation.parse(forceLinesPoints))
    if (sridForceLinesPoints == 3785 || sridForceLinesPoints == 4326) throw new IllegalArgumentException("Error : Please use a metric projection for Screens.")
    if (sridForceLinesPoints == 0) throw new IllegalArgumentException("Error : The table screens does not have an associated SRID.")

    //get SRID of the table
    int sridDEM = SFSUtilities.getSRID(connection, TableLocation.parse(dem))
    if (sridDEM == 3785 || sridDEM == 4326) throw new IllegalArgumentException("Error : Please use a metric projection for Buildings.")
    if (sridDEM == 0) throw new IllegalArgumentException("Error : The table buildings does not have an associated SRID.")

    if (sridDEM != sridForceLinesPoints) throw new IllegalArgumentException("Error : The SRID of table screens and buildings are not the same.")

        // Convert linestring screens to polygons with buffer function
        sql.execute("ALTER TABLE "+ forceLinesPoints + " ADD pk_line INT AUTO_INCREMENT;")

        sql.execute("DROP TABLE DEM_WITHOUT_PTLINE IF EXISTS;")
        sql.execute("CREATE TABLE DEM_WITHOUT_PTLINE AS SELECT d.the_geom FROM "+ dem + " d;")
        sql.execute("DELETE FROM DEM_WITHOUT_PTLINE WHERE EXISTS (SELECT 1 FROM "+ forceLinesPoints + "  b WHERE ST_EXPAND(DEM_WITHOUT_PTLINE .THE_GEOM," + buffer + "," + buffer + ")  && b.the_geom AND ST_DISTANCE(DEM_WITHOUT_PTLINE .THE_GEOM, b.the_geom )<" + buffer + " LIMIT 1) ;")

        sql.execute("DROP TABLE BUFFERED_PTLINE IF EXISTS;")
        sql.execute("CREATE TABLE BUFFERED_PTLINE AS SELECT st_tomultipoint(st_densify(st_buffer(the_geom, " + buffer + ",'endcap=flat'), " + resolution + ")) the_geom from "+ forceLinesPoints + " ;")

        sql.execute("DROP TABLE IF EXISTS BUFFERED_PTLINE_EX;")
        sql.execute("create table BUFFERED_PTLINE_EX as select * from st_explode('BUFFERED_PTLINE');")
        sql.execute("DROP TABLE BUFFERED_PTLINE IF EXISTS;")

        sql.execute("DROP TABLE IF EXISTS BUFFERED_PTLINE_Z ;")
        sql.execute("CREATE TABLE BUFFERED_PTLINE_Z  AS SELECT ST_ADDZ(ST_Force3D(r.THE_GEOM),  ST_Z(SELECT ST_ProjectPoint( r.THE_GEOM,t.THE_GEOM) THE_GEOM FROM "+ forceLinesPoints + "  t where st_expand(r.the_geom, " + buffer + ", " + buffer + ") && t.the_geom order by st_distance(r.the_geom, t.the_geom) LIMIT 1)) the_geom FROM BUFFERED_PTLINE_EX r ; ")
        sql.execute("DROP TABLE BUFFERED_PTLINE_EX IF EXISTS;")

        sql.execute("DROP TABLE IF EXISTS "+ dem + ";")
        sql.execute("CREATE TABLE "+ dem + " AS SELECT THE_GEOM FROM DEM_WIHTOUT_ROAD UNION ALL SELECT THE_GEOM FROM BUFFERED_PTLINE_Z;")
        sql.execute("DROP TABLE BUFFERED_PTLINE_Z IF EXISTS;")
        sql.execute("DROP TABLE DEM_WIHTOUT_PTLINE IF EXISTS;")   

    sql.execute("Create spatial index on DEM(the_geom);")
    resultString = "The table DEM has been created."

    // print to command window
    logger.info('Result : ' + resultString)
    logger.info('End : Merge DEM')

    // print to WPS Builder
    return resultString

}

