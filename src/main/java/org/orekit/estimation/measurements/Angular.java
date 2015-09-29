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

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.geometry.euclidean.threed.FieldVector3D;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Transform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.PVCoordinates;

/** Class modeling an Azimuth-Elevation measurement from a ground station.
 * @author Thierry Ceolin
 * @since 7.1
 */
public class Angular extends AbstractMeasurement<Angular> {

    /** Ground station from which measurement is performed. */
    private final GroundStation station;

    /** Simple constructor.
     * @param station ground station from which measurement is performed
     * @param date date of the measurement
     * @param angular observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @exception OrekitException if a {@link org.orekit.estimation.Parameter}
     * name conflict occurs
     */
    public Angular(final GroundStation station, final AbsoluteDate date,
                final double[] angular, final double[] sigma, final double[] baseWeight)
        throws OrekitException {
        super(date, angular, sigma, baseWeight);
        this.station = station;
        addSupportedParameter(station);
    }

    /** {@inheritDoc} */
    @Override
    protected Evaluation<Angular> theoreticalEvaluation(final int iteration, final SpacecraftState state)
        throws OrekitException {

        // station position in inertial frame at signal arrival
        final Transform topoToInert =
                station.getOffsetFrame().getTransformTo(state.getFrame(), getDate());
        final PVCoordinates stationArrival = topoToInert.transformPVCoordinates(PVCoordinates.ZERO);

        // take propagation time into account
        // (if state has already been set up to pre-compensate propagation delay,
        //  we will have offset == downlinkDelay and transitState will be
        //  the same as state)
        final double          tauD         = station.downlinkTimeOfFlight(state, getDate());
        final double          delta        = getDate().durationFrom(state.getDate());
        final SpacecraftState transitState = state.shiftedBy(delta - tauD);
        final Vector3D        transit      = transitState.getPVCoordinates().getPosition();

        // station frame in inertial frame
        final Vector3D east = topoToInert.transformVector(station.getBaseFrame().getEast());
        final Vector3D north = topoToInert.transformVector(station.getBaseFrame().getNorth());
        final Vector3D zenith = topoToInert.transformVector(station.getBaseFrame().getZenith());

        // satellite vector expressed in inertial frame
        final FieldVector3D<DerivativeStructure> P = new FieldVector3D<DerivativeStructure> (new DerivativeStructure(6, 1, 0, transit.getX()),
                                                                                             new DerivativeStructure(6, 1, 1, transit.getY()),
                                                                                             new DerivativeStructure(6, 1, 2, transit.getZ()));

        // station vector expressed in inertial frame
        final Vector3D  stationPosition      = topoToInert.transformVector(stationArrival.getPosition());
        final FieldVector3D<DerivativeStructure> Q = new FieldVector3D<DerivativeStructure> (new DerivativeStructure(6, 1, 3, stationPosition.getX()),
                                                                                             new DerivativeStructure(6, 1, 4, stationPosition.getY()),
                                                                                             new DerivativeStructure(6, 1, 5, stationPosition.getZ()));

        // station-satellite vector expressed in inertial frame
        final FieldVector3D<DerivativeStructure> QP = P.subtract(Q);

        final DerivativeStructure azimuth   = DerivativeStructure.atan2(QP.dotProduct(east), QP.dotProduct(north));
        final DerivativeStructure elevation = QP.dotProduct(zenith).divide(QP.getNorm());

        // prepare the evaluation
        final Evaluation<Angular> evaluation = new Evaluation<Angular>(this, iteration, transitState);

        // Azimuth - Elevation values
        evaluation.setValue(azimuth.getValue(), elevation.getValue());

        // partial derivatives with respect to state
        // partial derivatives of Azimuth with respect to state
        final double[] dAzOndP = new double[] {
            azimuth.getPartialDerivative(0),
            azimuth.getPartialDerivative(1),
            azimuth.getPartialDerivative(2),
            0.0,
            0.0,
            0.0
        };

        // partial derivatives of Elevation with respect to state
        final double[] dElOndP = new double[] {
            elevation.getPartialDerivative(0),
            elevation.getPartialDerivative(1),
            elevation.getPartialDerivative(2),
            0.0,
            0.0,
            0.0
        };

        evaluation.setStateDerivatives(dAzOndP, dElOndP);

        if (station.isEstimated()) {

            // partial derivatives with respect to parameters
            // partial derivatives of Azimuth with respect to parameters in inertial frame
            final Vector3D dAzOndQI = new Vector3D(azimuth.getPartialDerivative(3),
                                                   azimuth.getPartialDerivative(4),
                                                   azimuth.getPartialDerivative(5));

            // partial derivatives of Elevation with respect to parameters in inertial frame
            final Vector3D dElOndQI = new Vector3D(elevation.getPartialDerivative(3),
                                                   elevation.getPartialDerivative(4),
                                                   elevation.getPartialDerivative(5));

            // convert to topocentric frame, as the station position
            // offset parameter is expressed in this frame
            final AngularCoordinates ac = topoToInert.getAngular().revert();

            final Vector3D dAzOndQT = ac.getRotation().applyTo(dAzOndQI);
            final Vector3D dElOndQT = ac.getRotation().applyTo(dElOndQI);

            evaluation.setParameterDerivatives(station.getName(), dAzOndQT.toArray(), dElOndQT.toArray());
        }

        return evaluation;
    }

}