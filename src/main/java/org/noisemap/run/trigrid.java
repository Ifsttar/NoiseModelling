/**
 * NoiseMap is a scientific computation plugin for OrbisGIS developed in order to
 * evaluate the noise impact on urban mobility plans. This model is
 * based on the French standard method NMPB2008. It includes traffic-to-noise
 * sources evaluation and sound propagation processing.
 *
 * This version is developed at French IRSTV Institute and at IFSTTAR
 * (http://www.ifsttar.fr/) as part of the Eval-PDU project, funded by the
 * French Agence Nationale de la Recherche (ANR) under contract ANR-08-VILL-0005-01.
 *
 * Noisemap is distributed under GPL 3 license. Its reference contact is Judicaël
 * Picaut <judicael.picaut@ifsttar.fr>. It is maintained by Nicolas Fortin
 * as part of the "Atelier SIG" team of the IRSTV Institute <http://www.irstv.fr/>.
 *
 * Copyright (C) 2011 IFSTTAR
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488)
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
 * For more information, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */
package org.noisemap.run;

import java.io.File;
import java.util.Stack;
import org.apache.log4j.Logger;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.driver.DataSet;
import org.gdms.driver.DiskBufferDriver;
import org.gdms.driver.DriverException;
import org.gdms.driver.gdms.GdmsDriver;
import org.gdms.sql.function.FunctionException;
import org.noisemap.core.BR_PtGrid;
import org.noisemap.core.BR_TriGrid;

/**
 * Independant run of BR_TriGrid plugin.
 * Take two gdms file as input. The single output is another gdms file.
 * 
 * @author Nicolas Fortin
 */
public class trigrid {
    private static String getHumanTime(long millisec)  {
        long day=millisec/(1000*3600*24);
        long millirest=millisec%(1000*3600*24);
        long hour=millirest/(1000*3600);
        millirest %= (1000 * 3600);
        long minute=millirest/(1000*60);
        millirest %= (1000 * 60);
        long sec=millirest/1000;
        return day+" day(s) "+hour+" hour(s) "+minute+" minutes "+sec+" seconds";
    }
    private static void printUsage() {
        System.out.println("BR_TriGrid version 04/10/2011 11:33");
        System.out.println("Usage :");
        System.out.println("java -jar trigrid.jar [options] -ib bpath -is spath -o outpath");
        System.out.println("Options :");
        System.out.println("-bfield the_geom : buildings column name (polygons)");
        System.out.println("-sfield the_geom : sources column nale (points or lines)");
        System.out.println("-ir receiverspath: Points receivers coordinates (points)");
        System.out.println("-splfield db_m   : sound lvl field name(string)");
        System.out.println("-maxdist 170     : maximum propagation distance (double meter)");
        System.out.println("-maxrdist 50     : maximum wall reflexion distance (double meter)");
        System.out.println("-splitdepth 3    : subdivision level 4^n cells (int) [0-n]");
        System.out.println("-rwidth 0.8      : roads width (double meter), only when receiver not specified");
        System.out.println("-dense 5         : densification of receivers near roads (meter double), only when receiver not specified");
        System.out.println("-marea 250       : maximum area of triangle (square meter), only when receiver not specified");
        System.out.println("-rdepth 2        : sound reflection order [0-n] (int)");
        System.out.println("-ddepth 1        : sound diffraction order [0-n] (int)");
        System.out.println("-awalls 0.2      : alpha of walls [0-1[ (double)");
        System.out.println("-ib builds.gdms  : file name of buildings gdms file");
        System.out.println("-is sources.gdms : file name of noise sources gdms file");
        System.out.println("-o trilvl.gdms   : output filename of gdms file");
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        long debComputeTime=System.currentTimeMillis();
        // Parameters
        String buildingsFilename="";
        String sourcesFilename="";
        String outputFilename="";
        String receiverFilename="";
        //Optionnal parameters
        String bField="the_geom";
        String sField="the_geom";
        String splField="db_m";
        double maxDist=170;
        double maxRDist=50;
        int splitDepth=3;
        double roadsWidth=0.8;
        double densification=5.;
        double maxarea=250;
        int reflectionDepth=2;
        int diffractionDepth=1;
        double wallAlpha=.2;


        //Read parameters
        Stack<String> sargs=new Stack<String>();
        for(String arg : args) {
            sargs.insertElementAt(arg, 0);
        }
        while(!sargs.empty()) {
            String argument=sargs.pop();
            if(argument.contentEquals("-bfield")) {
                bField=sargs.pop();
            }else if(argument.contentEquals("-sfield")) {
                sField=sargs.pop();
            }else if(argument.contentEquals("-splfield")) {
                splField=sargs.pop();
            }else if(argument.contentEquals("-maxdist")) {
                maxDist=Double.valueOf(sargs.pop());
            }else if(argument.contentEquals("-maxrdist")) {
                maxRDist=Double.valueOf(sargs.pop());
            }else if(argument.contentEquals("-splitdepth")) {
                splitDepth=Integer.valueOf(sargs.pop());
            }else if(argument.contentEquals("-rwidth")) {
                roadsWidth=Double.valueOf(sargs.pop());
            }else if(argument.contentEquals("-dense")) {
                densification=Double.valueOf(sargs.pop());
            }else if(argument.contentEquals("-marea")) {
                maxarea=Double.valueOf(sargs.pop());
            }else if(argument.contentEquals("-rdepth")) {
                reflectionDepth=Integer.valueOf(sargs.pop());
            }else if(argument.contentEquals("-ddepth")) {
                diffractionDepth=Integer.valueOf(sargs.pop());
            }else if(argument.contentEquals("-awalls")) {
                wallAlpha=Double.valueOf(sargs.pop());
            }else if(argument.contentEquals("-ib")) {
                buildingsFilename=sargs.pop();
            }else if(argument.contentEquals("-is")) {
                sourcesFilename=sargs.pop();
            }else if(argument.contentEquals("-ir")) {
                receiverFilename=sargs.pop();
            }else if(argument.contentEquals("-o")) {
                outputFilename=sargs.pop();
            }else{
                System.err.println("Unknown parameter :"+argument);
                printUsage();
                return;
            }
        }
        if(buildingsFilename.isEmpty() || sourcesFilename.isEmpty() || outputFilename.isEmpty()) {
            printUsage();
            return;
        }
        //Load files
        DataSourceFactory factory=new DataSourceFactory();
        GdmsDriver buildings=new GdmsDriver();
        GdmsDriver sources=new GdmsDriver();
        GdmsDriver receivers_driv=new GdmsDriver();

        DataSet[] tables={null,null,null};
        try {
            buildings.setFile(new File(buildingsFilename));
            buildings.open();
            tables[0]=buildings.getTable("main");
            sources.setFile(new File(sourcesFilename));
            sources.open();
            tables[1]=sources.getTable("main");
            if(!receiverFilename.isEmpty()) {
                receivers_driv.setFile(new File(receiverFilename));
                receivers_driv.open();
                tables[2]=receivers_driv.getTable("main");
            }
        } catch (DriverException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
            return;
        }
        //Run propagation
        if(receiverFilename.isEmpty()) {
            BR_TriGrid propa=new BR_TriGrid();
            Logger log = new ConsoleLogger("BR_TriGrid");
            propa.setLogger(log);
            Value[] propaArgs={ValueFactory.createValue(splField),ValueFactory.createValue(maxDist),ValueFactory.createValue(maxRDist),ValueFactory.createValue(splitDepth),ValueFactory.createValue(roadsWidth),ValueFactory.createValue(densification),ValueFactory.createValue(maxarea),ValueFactory.createValue(reflectionDepth),ValueFactory.createValue(diffractionDepth),ValueFactory.createValue(wallAlpha)};
            DataSet data;
            try {
                data = propa.evaluate(factory, tables, propaArgs, null);
                long overallComputeTime=System.currentTimeMillis()-debComputeTime;
                System.out.println("Overall computation time: "+overallComputeTime+" ms."+getHumanTime(overallComputeTime));

            } catch (FunctionException ex) {
                System.err.println(ex.getMessage());
                ex.printStackTrace(System.err);
                return;
            }
            //Rename output file
            ((DiskBufferDriver)data).getFile().renameTo(new File(outputFilename));
        }else{
            BR_PtGrid propa=new BR_PtGrid();
            Logger log = new ConsoleLogger("BR_PtGrid");
            propa.setLogger(log);
            Value[] propaArgs={ValueFactory.createValue(splField),ValueFactory.createValue(maxDist),ValueFactory.createValue(maxRDist),ValueFactory.createValue(splitDepth),ValueFactory.createValue(reflectionDepth),ValueFactory.createValue(diffractionDepth),ValueFactory.createValue(wallAlpha)};
            DataSet data;
            try {
                data=propa.evaluate(factory, tables, propaArgs, null);
                //Rename output file
                ((DiskBufferDriver)data).getFile().renameTo(new File(outputFilename));
                long overallComputeTime=System.currentTimeMillis()-debComputeTime;
                System.out.println("Overall computation time: "+overallComputeTime+" ms."+getHumanTime(overallComputeTime));
            } catch (FunctionException ex) {
                System.err.println(ex.getMessage());
                ex.printStackTrace(System.err);
                return;
            }
        }
        listAllThreads();

    }
  private static void printThreadInfo(Thread t, String indent) {
    if (t == null) {
      return;
     }
    System.out.println(indent + "Thread: " + t.getName() + "  Priority: "
        + t.getPriority() + (t.isDaemon() ? " Daemon" : "")
        + (t.isAlive() ? "" : " Not Alive"));
  }

  /** Display info about a thread group */
  private static void printGroupInfo(ThreadGroup g, String indent) {
    if (g == null) {
      return;
     }
    int numThreads = g.activeCount();
    int numGroups = g.activeGroupCount();
    Thread[] threads = new Thread[numThreads];
    ThreadGroup[] groups = new ThreadGroup[numGroups];

    g.enumerate(threads, false);
    g.enumerate(groups, false);

    System.out.println(indent + "Thread Group: " + g.getName()
        + "  Max Priority: " + g.getMaxPriority()
        + (g.isDaemon() ? " Daemon" : ""));

    for (int i = 0; i < numThreads; i++) {
      printThreadInfo(threads[i], indent + "    ");
      }
    for (int i = 0; i < numGroups; i++) {
      printGroupInfo(groups[i], indent + "    ");
      }
  }

  /** Find the root thread group and list it recursively */
  public static void listAllThreads() {
    ThreadGroup currentThreadGroup;
    ThreadGroup rootThreadGroup;
    ThreadGroup parent;

    // Get the current thread group
    currentThreadGroup = Thread.currentThread().getThreadGroup();

    // Now go find the root thread group
    rootThreadGroup = currentThreadGroup;
    parent = rootThreadGroup.getParent();
    while (parent != null) {
      rootThreadGroup = parent;
      parent = parent.getParent();
    }

    printGroupInfo(rootThreadGroup, "");
  }
}
