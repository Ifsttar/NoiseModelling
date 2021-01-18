package org.noise_planet.noisemodelling.emission;

import org.junit.Test;

public class EvaluateTrainSourceCNOSSOSTest {
    private static final double EPSILON_TEST1 = 0.01;
    private static final int[] FREQUENCIES = new int[]{50, 63, 80, 100, 125, 160, 200, 250, 315, 400, 500, 630, 800, 1000, 1250, 1600, 2000, 2500, 3150, 4000, 5000, 6300, 8000, 10000};

    @Test
    public void Test_X_TER_bicaisse_D() {
        String vehCat = "X-TER-bicaisse-D";
        double vehicleSpeed = 160;
        double vehiclePerHour = 1;
        int vehPerTrain = 2;

        int trackTransfer = 4;
        int impactNoise = 0;
        int bridgeConstant = 0;
        int curvate = 0;
        int railRoughness = 4;

        double vMaxInfra = 160;
        LWRailWay lWRailWay = null;

        VehiculeParametersCnossos vehiculeParameters = new VehiculeParametersCnossos(vehCat, "", vehPerTrain,
                vehicleSpeed, vehiclePerHour, 0, 0);
        TrackParametersCnossos trackParameters = new TrackParametersCnossos(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeConstant, curvate);
        lWRailWay = EvaluateTrainSourceCnossos.evaluate(vehiculeParameters, trackParameters);
        double[] LWRolling = lWRailWay.getLWRolling();
    }
    @Test
    public void Test_TGV_DUPLEX() {
        String[] vehCat = new String[3];
        vehCat[0] = "TGV-DUPLEX-motrice";
        vehCat[1] = "TGV-DUPLEX-voiture-1";
        vehCat[2] = "TGV-DUPLEX-voiture-2";

        double vehicleSpeed = 320;
        double vehiclePerHour = 1;
        int vehPerTrain = 3;

        int trackTransfer = 3;
        int impactNoise = 3;
        int bridgeConstant = 3;
        int curvate = 2;
        int railRoughness = 4;

        double vMaxInfra = 320;
        LWRailWay lWRailWay = null;
        for(int i=0;i<vehPerTrain;i++){
            VehiculeParametersCnossos vehiculeParameters = new VehiculeParametersCnossos(vehCat[i], "", vehPerTrain,
                    vehicleSpeed, vehiclePerHour, 0, 0);
            TrackParametersCnossos trackParameters = new TrackParametersCnossos(vMaxInfra, trackTransfer, railRoughness,
                    impactNoise, bridgeConstant, curvate);
            lWRailWay = EvaluateTrainSourceCnossos.evaluate(vehiculeParameters, trackParameters);
            double[] LWRolling = lWRailWay.getLWRolling();
        }
    }
    /*
    @Test
    public void Test_TGV_DUPLEX() {
        String vehCat1 = "TGV-DUPLEX-motrice";
        String vehCat2 = "TGV-DUPLEX-voiture-1";
        String vehCat3 = "TGV-DUPLEX-voiture-2";
        double vehicleSpeed = 160;
        double vehiclePerHour = 1;
        int vehPerTrain = 2;

        int trackTransfer = 4;
        int impactNoise = 0;
        int bridgeConstant = 0;
        int curvate = 0;
        int railRoughness = 4;

        double vMaxInfra = 160;
        LWRailWay lWRailWayA = null;
        LWRailWay lWRailWayB = null;

        for (){
            for (int sourceHeight = 0; sourceHeight < 2; sourceHeight++) {
                VehiculeParametersCnossos vehiculeParameters = new VehiculeParametersCnossos(vehCat, "", vehPerTrain,
                        vehicleSpeed, vehiclePerHour, 0, 0);
                TrackParametersCnossos trackParameters = new TrackParametersCnossos(vMaxInfra, trackTransfer, railRoughness,
                        impactNoise, bridgeConstant, curvate);
                if (sourceHeight == 0) {
                    lWRailWayA = EvaluateTrainSourceCnossos.evaluate(vehiculeParameters, trackParameters);
                } else if (sourceHeight == 1) {
                    lWRailWayB = EvaluateTrainSourceCnossos.evaluate(vehiculeParameters, trackParameters);
                }
                // TODO add ref calcul exemple LWRoll = CNOSSOS p.19 (2.3.7)
            }
         }
        /*
            double[] LWRolling = lWRailWayA.getLWRolling();
            double[] LWTractionA = lWRailWayA.getLWTraction();
            double[] LWTractionB = lWRailWayB.getLWRolling();
            double[] LWAerodynamicA = lWRailWayA.getLWAerodynamic();
            double[] LWAerodynamicB = lWRailWayB.getLWAerodynamic();

    }*/
}
