/* Copyright 2002-2015 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.estimation.measurements;

import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.Parameter;
import org.orekit.estimation.measurements.RangeTroposphericDelayModifier.Derivatives;
import org.orekit.models.earth.SaastamoinenModel;
import org.orekit.models.earth.TroposphericDelayModel;
import org.orekit.propagation.SpacecraftState;

/** Class modifying theoretical range-rate measurements with tropospheric delay.
 * The effect of tropospheric correction on the range-rate is directly computed
 * through the computation of the tropospheric delay difference with respect to
 * time.
 *
 *
 * @author Joris Olympio
 * @since 7.1
 */
public class RangeRateTroposphericDelayModifier implements EvaluationModifier {
    /** Tropospheric delay model. */
    private final TroposphericDelayModel tropoModel;

    /**
     * Constructor.
     * @param model  Tropospheric delay model
     */
    public RangeRateTroposphericDelayModifier(final TroposphericDelayModel model) {
        tropoModel = model;
    }

    /**
     * Simple Constructor.
     */
    public RangeRateTroposphericDelayModifier() {
        tropoModel = SaastamoinenModel.getStandardModel();
    }

    /** Get the station height above mean sea level.
     *
     * @param station  ground station (or measuring station)
     * @return the measuring station height above sea level, m
     */
    private double getStationHeightAMSL(final GroundStation station) {
        // FIXME Il faut la hauteur par rapport au geoide WGS84+GUND = EGM2008 par exemple
        final double height = station.getBaseFrame().getPoint().getAltitude();
        return height;
    }

    /** Compute the measurement error due to Troposphere.
     * @param station station
     * @param state spacecraft state
     * @return the measurement error due to Troposphere
     * @throws OrekitException  if frames transformations cannot be computed
     */
    public double rangeRateErrorTroposphericModel(final GroundStation station,
                                                  final SpacecraftState state) throws OrekitException
    {
        // The effect of tropospheric correction on the range rate is
        // computed using finite differences.

        final double dt = 10; // s

        // station altitude AMSL in meters
        final double height = getStationHeightAMSL(station);

        // spacecraft position and elevation as seen from the ground station
        final Vector3D position = state.getPVCoordinates().getPosition();
        final double elevation1 = station.getBaseFrame().getElevation(position,
                                                                      state.getFrame(),
                                                                      state.getDate());

        // only consider measures above the horizon
        if (elevation1 > 0) {
            // tropospheric delay
            final double d1 = tropoModel.calculatePathDelay(elevation1, height);

            // propagate spacecraft state forward by dt
            final SpacecraftState state2 = state.shiftedBy(dt);

            // spacecraft position and elevation as seen from the ground station
            final Vector3D position2 = state2.getPVCoordinates().getPosition();
            final double elevation2 =
                    station.getBaseFrame().getElevation(position2,
                                                        state2.getFrame(),
                                                        state2.getDate());

            // tropospheric delay dt after
            final double d2 = tropoModel.calculatePathDelay(elevation2, height);

            return (d2 - d1) / dt;
        }

        return 0;
    }

    /** Compute the measurement error due to Ionosphere.
     *
     * @param station ground station
     * @param state spacecraft state
     * @param derivatives derivatives (dm/dElevation and dm/dHeight)
     *          where m is the time derivative of the tropospheric error.
     * @throws OrekitException if frames transformations cannot be computed
     *
     */
    public void rangeRateTropoErrorDerivatives(final GroundStation station,
                                                final SpacecraftState state,
                                                final double[] derivatives) throws OrekitException
    {
        // The effect of tropospheric correction on the range rate is
        // computed using finite differences.
        final double h = 1e-6; // finite difference step

        final double dt = 20; // s

        // station altitude AMSL in meters
        final double height = getStationHeightAMSL(station);
        final double stepH = h * FastMath.max(1., height);

        // spacecraft position and elevation as seen from the ground station
        final Vector3D position = state.getPVCoordinates().getPosition();
        // target's elevation at the current date
        final double elevation1 = station.getBaseFrame().getElevation(position,
                                                                      state.getFrame(),
                                                                      state.getDate());
        final double stepE = h * FastMath.max(1., elevation1);

        // tropospheric delay
        final double dDmt = tropoModel.calculatePathDelay(elevation1, height);
        final double dDmEmt = tropoModel.calculatePathDelay(elevation1 - stepE, height);
        final double dDmHmt = tropoModel.calculatePathDelay(elevation1, height - stepH);

        // propagate spacecraft state forward by dt
        final SpacecraftState state2 = state.shiftedBy(dt);

        // spacecraft position and elevation as seen from the ground station
        final Vector3D position2 = state2.getPVCoordinates().getPosition();
        // target's elevation at the new date
        final double elevation2 = station.getBaseFrame().getElevation(position2,
                                                                      state2.getFrame(),
                                                                      state2.getDate());

        // tropospheric delay dt after
        final double dDpt = tropoModel.calculatePathDelay(elevation2, height);
        final double dDpEpt = tropoModel.calculatePathDelay(elevation2 + stepE, height);
        final double dDpHpt = tropoModel.calculatePathDelay(elevation2, height + stepH);

        // d mdot / delevation
        derivatives[0] = (dDpEpt - dDpt - dDmEmt + dDmt) / (4 * dt * stepE);
        // d mdot / dheight
        derivatives[1] = (dDpHpt - dDpt - dDmHmt + dDmt) / (4 * dt * stepH);
    }


    /** Compute the Jacobian of the delay term wrt state.
     *
     * @param station station
     * @param state spacecraft state
     * @param delay current tropospheric delay
     * @return jacobian of the delay wrt state
     * @throws OrekitException  if frames transformations cannot be computed
     */
    private double[][] rangeRateErrorJacobianState(final GroundStation station,
                                               final SpacecraftState state,
                                               final double delay) throws OrekitException
    {
        // compute the derivatives of the tropospheric delay model wrt height and elevation.
        final double[] dDelayDot = new double[2];
        rangeRateTropoErrorDerivatives(station, state, dDelayDot);
        final double dDelayDotdElevation = dDelayDot[0];
        final double dDelayDotdHeight = dDelayDot[1];

        // derivatives of station's height and target elevation with respect to target's state
        final double[] dEdX = Derivatives.derivElevationWrtState(state.getDate(), station, state);
        final double[] dHdX = new double[] {0, 0, 0, 0, 0, 0};

        return new double[][]{
            {
                dDelayDotdHeight * dHdX[0] + dDelayDotdElevation * dEdX[0],
                dDelayDotdHeight * dHdX[1] + dDelayDotdElevation * dEdX[1],
                dDelayDotdHeight * dHdX[2] + dDelayDotdElevation * dEdX[2],
                dDelayDotdHeight * dHdX[3] + dDelayDotdElevation * dEdX[3],
                dDelayDotdHeight * dHdX[4] + dDelayDotdElevation * dEdX[4],
                dDelayDotdHeight * dHdX[5] + dDelayDotdElevation * dEdX[5]
            }
        };
    }

    /** Compute the Jacobian of the delay term wrt parameters.
     *
     * @param station station
     * @param state spacecraft state
     * @param delay current tropospheric delay
     * @return jacobian of the delay wrt state
     * @throws OrekitException  if frames transformations cannot be computed
     */
    private double[][] rangeRateErrorJacobianParameter(final GroundStation station,
                                                   final SpacecraftState state,
                                                   final double delay) throws OrekitException
    {
        // compute the derivatives of the tropospheric delay model wrt height and elevation.
        final double[] dDelayDot = new double[2];
        rangeRateTropoErrorDerivatives(station, state, dDelayDot);
        final double dDelayDotdElevation = dDelayDot[0];
        final double dDelayDotdHeight = dDelayDot[1];

        // derivatives of station's height and target elevation with respect to station's position vector
        final double[] dHdP = Derivatives.derivHeightWrtGroundstation(state.getDate(), station, state);
        final double[] dEdP = Derivatives.derivElevationWrtGroundstation(state.getDate(), station, state);

        return new double[][]{
            {dDelayDotdHeight * dHdP[0] + dDelayDotdElevation * dEdP[0],
             dDelayDotdHeight * dHdP[1] + dDelayDotdElevation * dEdP[1],
             dDelayDotdHeight * dHdP[2] + dDelayDotdElevation * dEdP[2]}
        };
    }

    @Override
    public List<Parameter> getSupportedParameters() {
        return null;
    }

    @Override
    public void modify(final Evaluation evaluation)
        throws OrekitException {
        final RangeRate measure = (RangeRate) evaluation.getMeasurement();
        final GroundStation station = measure.getStation();
        final SpacecraftState state = evaluation.getState();

        final double[] oldValue = evaluation.getValue();

        final double delay = rangeRateErrorTroposphericModel(station, state);

        // update measurement value taking into account the tropospheric delay.
        // The tropospheric delay is directly added to the range.
        final double[] newValue = oldValue.clone();
        newValue[0] = newValue[0] + delay;
        evaluation.setValue(newValue);

        // update measurement derivatives with jacobian of the measure wrt state
        final double[][] djac = rangeRateErrorJacobianState(station,
                                      state,
                                      delay);
        final double[][] stateDerivatives = evaluation.getStateDerivatives();
        for (int irow = 0; irow < stateDerivatives.length; ++irow) {
            for (int jcol = 0; jcol < stateDerivatives[0].length; ++jcol) {
                stateDerivatives[irow][jcol] += djac[irow][jcol];
            }
        }
        evaluation.setStateDerivatives(stateDerivatives);


        if (station.isEstimated()) {
            // update measurement derivatives with jacobian of the measure wrt station parameters
            // by simply adding the jacobian the delay term.
            final double[][] djacdp = rangeRateErrorJacobianParameter(station,
                                                                  state,
                                                                  delay);
            final double[][] parameterDerivatives = evaluation.getParameterDerivatives(station.getName());
            for (int irow = 0; irow < parameterDerivatives.length; ++irow) {
                for (int jcol = 0; jcol < parameterDerivatives[0].length; ++jcol) {
                    parameterDerivatives[irow][jcol] += djacdp[irow][jcol];
                }
            }

            evaluation.setParameterDerivatives(station.getName(), parameterDerivatives);
        }
    }
}
