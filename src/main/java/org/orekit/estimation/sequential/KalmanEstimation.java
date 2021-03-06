/* Copyright 2002-2018 CS Systèmes d'Information
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
package org.orekit.estimation.sequential;

import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriversList;

/** Interface for accessing {@link KalmanEstimator Kalman filter} estimations.
 * @author Luc Maisonobe
 * @since 9.2
 */
public interface KalmanEstimation {

    /** Get the list of estimated orbital parameters.
     * @return the list of estimated orbital parameters
     */
    ParameterDriversList getEstimatedOrbitalParameters();

    /** Get the list of estimated propagation parameters.
     * @return the list of estimated propagation parameters
     */
    ParameterDriversList getEstimatedPropagationParameters();

    /** Get the list of estimated measurements parameters.
     * @return the list of estimated measurements parameters
     */
    ParameterDriversList getEstimatedMeasurementsParameters();

    /** Get the predicted spacecraft states.
     * @return predicted spacecraft states
     */
    SpacecraftState[] getPredictedSpacecraftStates();

    /** Get the corrected spacecraft states.
     * @return corrected spacecraft states
     */
    SpacecraftState[] getCorrectedSpacecraftStates();

    /** Get the "physical" estimated state (i.e. not normalized)
     * @return the "physical" estimated state
     */
    RealVector getPhysicalEstimatedState();

    /** Get the "physical" estimated covariance matrix (i.e. not normalized)
     * @return the "physical" estimated covariance matrix
     */
    RealMatrix getPhysicalEstimatedCovarianceMatrix();

    /** Get the current measurement number.
     * @return current measurement number
     */
    int getCurrentMeasurementNumber();

    /** Get the current date.
     * @return current date
     */
    AbsoluteDate getCurrentDate();

    /** Get the predicted measurement.
     * <p>
     * This estimation has been evaluated on the last predicted orbits
     * </p>
     * @return predicted measurement
     */
    EstimatedMeasurement<?> getPredictedMeasurement();

    /** Get the estimated measurement.
     * <p>
     * This estimation has been evaluated on the last corrected orbits
     * </p>
     * @return corrected measurement
     */
    EstimatedMeasurement<?> getCorrectedMeasurement();

}
