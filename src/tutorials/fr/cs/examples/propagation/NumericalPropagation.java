/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.cs.examples.propagation;

import org.apache.commons.math.ode.FirstOrderIntegrator;
import org.apache.commons.math.ode.nonstiff.GraggBulirschStoerIntegrator;
import org.orekit.errors.OrekitException;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.CunninghamAttractionModel;
import org.orekit.frames.Frame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.UTCScale;

import fr.cs.examples.Autoconfiguration;

/** The aim of this tutorial is to manipulate the Numerical propagator
 * and the force models.
 * @author Fabien Maussion
 * @version $Revision$ $Date$
 */
public class NumericalPropagation {

    public static void main(String[] args) {
        try {

            // configure Orekit
            Autoconfiguration.configureOrekit();

            // physical constants :

            double mu =  3.9860064e+14; // gravitation coefficient
            double ae =  6378136.460; // equatorial radius in meter
            double c20 = -1.08262631303e-3; // J2 potential coefficent
            Frame ITRF2005 = Frame.getITRF2005(); // terrestrial frame at an arbitrary date


            //  Initial state definition :

            // parameters :
            double a = 24396159; // semi major axis in meters
            double e = 0.72831215; // eccentricity
            double i = Math.toRadians(7); // inclination
            double omega = Math.toRadians(180); // perigee argument
            double raan = Math.toRadians(261); // right ascencion of ascending node
            double lv = 0; // mean anomaly

            double mass = 2500; // mass of the spacecraft in Kg

            // date and frame

            AbsoluteDate initialDate = new AbsoluteDate(new DateComponents(2004, 01, 01),
                                                        new TimeComponents(23, 30, 00.000),
                                                        UTCScale.getInstance());

            Frame inertialFrame = Frame.getEME2000();

            // OREKIT objects construction:

            Orbit initialOrbit = new KeplerianOrbit(a, e, i, omega, raan, lv,
                                                    KeplerianOrbit.MEAN_ANOMALY,
                                                    inertialFrame, initialDate, mu);

            SpacecraftState initialState = new SpacecraftState(initialOrbit, mass);

            /* ***************** */
            /*   Extrapolation   */
            /* ***************** */

            // Integrator

            FirstOrderIntegrator integrator = new GraggBulirschStoerIntegrator(1, 1000, 0, 1.0e-8);
            // adaptive step integrator with a minimum step of 1 and a maximum step of 1000, and a relative precision of 1.0e-8
 
            NumericalPropagator propagator = new NumericalPropagator(integrator);

            // Perturbing gravity field :

            double[][] c = new double[3][1];
            c[0][0] = 0.0;
            c[2][0] = c20;
            double[][] s = new double[3][1]; // potential coeffs arrays (only J2 is considered here)

            ForceModel cunningham = new CunninghamAttractionModel(ITRF2005, ae, mu, c, s);
            propagator.addForceModel(cunningham);

            // propagation with storage of the results in an integrated ephemeris

            AbsoluteDate finalDate = new AbsoluteDate(initialDate, 500);
            propagator.setInitialState(initialState);
            propagator.setEphemerisMode();
            SpacecraftState finalState = propagator.propagate(finalDate);
            BoundedPropagator ephemeris = propagator.getGeneratedEphemeris();
            System.out.println(" Final state  : " + finalState.getOrbit());
            AbsoluteDate intermediateDate = new AbsoluteDate(initialDate, 214);
            SpacecraftState intermediateState = ephemeris.propagate(intermediateDate);
            System.out.println("  intermediate state  :  " + intermediateState.getOrbit());

        } catch (OrekitException oe) {
            System.err.println(oe.getMessage());
        }
    }
}