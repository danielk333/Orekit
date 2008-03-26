package fr.cs.orekit.frames;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import org.apache.commons.math.geometry.Rotation;

import fr.cs.orekit.errors.FrameAncestorException;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.time.AbsoluteDate;

/** Tridimensional references frames class.
 *
 * <p><h5> Frame Presentation </h5>
 * This class is the base class for all frames in OREKIT. The frames are
 * linked together in a tree with the J2000 frame as the root of the tree.
 * Each frame is defined by {@link Transform transforms} combining any number of translations and
 * rotations from a reference frame which is its parent frame in the tree
 * structure.</p>
 * <p>When we say a {@link Transform transform} t is <em>from frame<sub>A</sub>
 * to frame<sub>B</sub></em>, we mean that if the coordinates of some absolute
 * vector (say the direction of a distant star for example) has coordinates
 * u<sub>A</sub> in frame<sub>A</sub> and u<sub>B</sub> in frame<sub>B</sub>,
 * then u<sub>B</sub>={@link Transform#transformVector(Vector3D) t.transformVector(u<sub>A</sub>)}.
 * <p>The transforms may be constant or varying. For simple fixed transforms,
 * using this base class is sufficient. For varying transforms (time-dependant
 * or telemetry-based for example), it may be useful to define specific subclasses
 * that will implement {@link #updateFrame(AbsoluteDate)} or that will
 * add some specific <code>updateFromTelemetry(telemetry)</code>
 * methods that will compute the transform and call internally
 * the {@link #updateTransform(Transform)} method.</p>
 *
 * <p>  <h5> Reference Frames </h5>
 *  Several Reference frames are implemented in OREKIT. The user can
 *  {@link #getReferenceFrame(Frame.FrameType, AbsoluteDate) use them}
 *  by specifying the {@link FrameType} (type enum) he wants.
 *
 *    <h5> International Terrestrial Reference Frame 2000 </h5>
 * This frame is the current (as of 2006) reference realization of
 * the International Terrestrial Reference System produced by IERS.
 * It is described in <a
 * href="http://www.iers.org/documents/publications/tn/tn32/tn32.pdf">
 * IERS conventions (2003)</a>. It replaces the Earth Centered Earth Fixed
 * frame which is the reference frame for GPS satellites.
 * <p>This frame is used to define position on solid Earth. It rotates with
 * the Earth and includes the pole motion with respect to Earth crust as
 * provided by {@link fr.cs.orekit.iers.IERSDirectoryCrawler IERS data}.
 * Its pole axis is the IERS Reference Pole (IRP).</p>
 *  OREKIT proposes all the intermediate frames used to build this specific frame.
 *  Here is a shematical representation of the ITRF frame tree :
 *
 * <pre>
 *
 *       - J2000 -
 *        /     \   Precession and Nutation effects
 *       /       \   (the complexity of the parameters changes between A and B models)
 *      /         \
 *  {@link #IRF2000A}    {@link #IRF2000B}    (intermediate reference frame : true equinox and equator of date)
 *      |          |
 *      |          |   Earth natural rotation
 *      |          |
 *  {@link #TIRF2000A}   {@link #TIRF2000B}   (terrestrial intermediate reference frame : Pseudo Earth Fixed Frame)
 *      |          |
 *      |          |   Pole motion
 *      |          |
 *  {@link #ITRF2000A}  {@link #ITRF2000B}   (international terrestrial reference frame)
 *
 * </pre>
 * <p> This implementation follows the new non-rotating origin paradigm
 * mandated by IAU 2000 resolution B1.8. It is therefore based on
 * Celestial Ephemeris Origin (CEO-based) and Earth Rotating Angle. Depending
 * on user choice at construction, it is either consistent to the complete IAU
 * 2000A precession-nutation model with an accuracy level of 0.2 milliarcsecond
 * or consistent to the reduced IAU 2000B precession-nutation model with an
 * accuracy level of 1.0 milliarcsecond. The IAU2000B is recommended for most
 * applications since it is <strong>far less</strong> computation intensive than
 * the IAU2000A model and its accuracy is only slightly degraded.
 * </p>
 * <p>Other implementations of the ITRF 2000 are possible by
 * ignoring the B1.8 resolution and using the classical paradigm which
 * is equinox-based and relies on a specifically tuned Greenwich Sidereal Time.
 * They are not yet available in the OREKIT library.</p>
 * </p>
 *
 * @author G. Prat
 * @author L. Maisonobe
 */
public class Frame implements Serializable {

    /** International Terrestrial Reference Frame 2000 A.
     * <p> Replaces the old ECEF representation. <p>
     */
    public static final FrameType ITRF2000A = new FrameType("ITRF2000A");

    /** International Terrestrial Reference Frame 2000 B.
     * <p> Replaces the old ECEF representation. <p>
     */
    public static final FrameType ITRF2000B = new FrameType("ITRF2000B");

    /** Intermediate Reference Frame 2000 A : true equinox and equator of date.
     * <p> Precession and nutation effects with maximal precision and no
     * earth rotation. <p>
     */
    public static final FrameType IRF2000A = new FrameType("IRF2000A");

    /** Intermediate Reference Frame 2000 B : true equinox and equator of date.
     * <p> Precession and nutation effects with less precision and no
     * earth rotation. <p>
     */
    public static final FrameType IRF2000B = new FrameType("IRF2000B");

    /** Terrestrial Intermediate Reference Frame 2000 A.
     * <p> The pole motion is not considered.</p> */
    public static final FrameType TIRF2000A = new FrameType("TIRF2000A");

    /** Terrestrial Intermediate Reference Frame 2000 B.
     * <p> The pole motion is not considered.</p> */
    public static final FrameType TIRF2000B = new FrameType("TIRF2000B");

    /** Veis 1950 frame.
     * <p>This frame is sometimes refered to as
     * <em>&gamma;<sub>50</sub> CNES</em></p> */
    public static final FrameType VEIS1950 = new FrameType("VEIS1950");

    /** Serialiazable UID. */
    private static final long serialVersionUID = 2071889292905823128L;

    /** Reference earth ITRF2000 A frame singleton. */
    private static ITRF2000Frame itrf2000AFrame = null;

    /** Reference earth ITRF2000 B frame singleton. */
    private static ITRF2000Frame itrf2000BFrame = null;

    /** True earth TIRF2000 A frame singleton. */
    private static TIRF2000Frame tirf2000AFrame = null;

    /** True earth TIRF2000 B frame singleton. */
    private static TIRF2000Frame tirf2000BFrame = null;

    /** True equator IRF2000 A frame singleton. */
    private static IRF2000Frame irf2000AFrame = null;

    /** True equator IRF2000 B frame singleton. */
    private static IRF2000Frame irf2000BFrame = null;

    /** Mean equator 1950 frame singleton. */
    private static Frame veis1950Frame = null;

    /** J2000 root frame. */
    private static Frame j2000 = null;

    /**  parent frame (only J2000 doesn't have a parent). */
    private final Frame parent;

    /** Transform from parent frame to instance. */
    private Transform transform;

    /** Map of deepest frames commons with other frames. */
    private final HashMap commons;

    /** Instance name. */
    private final String name;

    /** Private constructor used only for the J2000 root frame.
     * @param name name of the frame
     */
    private Frame(String name) {
        parent    = null;
        transform = new Transform();
        commons   = new HashMap();
        this.name = name;
    }

    /** Build a frame from its transform with respect to its parent.
     * <p>The convention for the transform is that it is from parent
     * frame to instance. This means that the two following frames
     * are similar:</p>
     * <pre>
     * Frame frame1 = new Frame(Frame.getJ2000(), new Transform(t1, t2));
     * Frame frame2 = new Frame(new Frame(Frame.getJ2000(), t1), t2);
     * </pre>
     * @param parent parent frame (must be non-null)
     * @param transform transform from parent frame to instance
     * @param name name of the frame
     * @exception IllegalArgumentException if the parent frame is null
     */
    public Frame(Frame parent, Transform transform,
                 String name) throws IllegalArgumentException {

        if (parent == null) {
            OrekitException.throwIllegalArgumentException("null parent for frame {0}",
                                                          new Object[] { name });
        }
        this.name      = name;
        this.parent    = parent;
        this.transform = transform;
        commons        = new HashMap();

    }

    /** Get the unique J2000 frame.
     * @return the unique instance of the J2000 frame
     */
    public static Frame getJ2000() {
        if (j2000 == null) {
            j2000 = new Frame("J2000");
        }
        return j2000;
    }

    /** Get the name.
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /** New definition of the java.util toString() method.
     * @return the name
     */
    public String toString() {
        return this.name;
    }

    /** Get the parent frame
     * @return parent frame
     */
    public Frame getParent() {
        return parent;
    }

    /** Update the transform from the parent frame to the instance.
     * @param transform new transform from parent frame to instance
     */
    public void updateTransform(Transform transform) {
        this.transform = transform;
    }

    /** Get the transform from the instance to another frame.
     * @param destination destination frame to which we want to transform vectors
     * @param date the date (can be null if it is sure than no date dependant frame is used)
     * @return transform from the instance to the destination frame
     * @throws OrekitException if some frame specific error occurs
     */
    public Transform getTransformTo(Frame destination,
                                    AbsoluteDate date) throws OrekitException {

        // common ancestor to both frames in the frames tree
        final Frame common = findCommon(this, destination);

        // transform from common to instance
        Transform commonToInstance = new Transform();
        for (Frame frame = this; frame != common; frame = frame.parent) {
            frame.updateFrame(date);
            commonToInstance =
                new Transform(frame.transform, commonToInstance);
        }

        // transform from destination up to common
        Transform commonToDestination = new Transform();
        for (Frame frame = destination; frame != common; frame = frame.parent) {
            frame.updateFrame(date);
            commonToDestination =
                new Transform(frame.transform, commonToDestination);
        }

        // transform from instance to destination via common
        return new Transform(commonToInstance.getInverse(), commonToDestination);

    }

    /** Update the frame to the given date.
     * <p>This method is called each time {@link #getTransformTo(Frame, AbsoluteDate)}
     * is called. Default behaviour is to do nothing. The proper way to build
     * a date-dependant frame is to extend {@link Frame} and implement this method which
     * will have to call {@link #updateTransform(Transform)} with the new transform </p>
     * @param date new value of the  date
     * @exception OrekitException if some frame specific error occurs
     */
    protected void updateFrame(AbsoluteDate date) throws OrekitException {

    }

    /** Update the transform from parent frame implicitly according to two other
     * frames.

     * <p>This method allows to control the relative position of two parts
     * of the global frames tree using any two frames in each part as
     * control handles. Consider the following simplified frames tree as an
     * example:</p>
     * <pre>
     *               J<sub>2000</sub>
     *                 |
     *  --------------------------------
     *  |             |                |
     * Sun        satellite          Earth
     *                |                |
     *        on-board antenna   ground station
     *                                 |
     *                          tracking antenna
     * </pre>
     * <p>Tracking measurements really correspond to the link between the ground
     * and on-board antennas. This is tightly linked to the transform between
     * these two frames, however neither frame is the direct parent frame of the
     * other ones: the path involves four intermediate frames. When we process a
     * measurement, what we really want to update is the transform that defines
     * the satellite frame with respect to its parent J<sub>2000</sub> frame. This
     * is the purpose of this method. This update is done by the following call,
     * where <code>measurementTransform</code> represent the measurement as a
     * simple translation transform between the two antenna frames:</p>
     * <pre><code>
     * satellite.updateTransform(onBoardAntenna, trackingAntenna,
     *                           measurementTransform, date);
     * </code></pre>
     * <p>One way to represent the behavior of the method is to consider the
     * sub-tree rooted at the instance on one hand (satellite and on-board antenna
     * in the example above) and the tree containing all the other frames on the
     * other hand (J<sub>2000</sub>, Sun, Earth, ground station, tracking antenna).
     * Both tree are considered as solid sets linked by a flexible spring, which is
     * the transform we want to update. The method stretches the spring to make
     * sure the transform between the two specified frames (one in each tree part)
     * matches the specified transform.</p>
     * @param f1 first control frame (may be the instance itself)
     * @param f2 second control frame (may be the instance itself)
     * @param f1Tof2 desired transform from first to second control frame
     * @param date date of the transform
     * @exception OrekitException if the path between the two control frames does
     * not cross the link between instance and its parent frame or if some
     * intermediate transform fails
     * @see #updateTransform(Transform)
     */
    public void updateTransform(Frame f1, Frame f2, Transform f1Tof2,
                                AbsoluteDate date) throws OrekitException {

      // make sure f1 is not a child of the instance
        if (f1.isChildOf(this) || (f1 == this)) {

            if (f2.isChildOf(this) || (f2 == this)) {
                throw new FrameAncestorException("both frames {0} and {1} are child of {2}",
                                                 new Object[] {
                                                     f1.getName(), f2.getName(), getName()
                                                 });
            }

            // swap f1 and f2 to make sure the child is f2
            final Frame tmp = f1;
            f1 = f2;
            f2 = tmp;
            f1Tof2 = f1Tof2.getInverse();

        } else  if (! (f2.isChildOf(this) || (f2 == this))) {
            throw new FrameAncestorException("neither frames {0} nor {1} have {2} as ancestor",
                                             new Object[] {
                                                 f1.getName(), f2.getName(), getName()
                                             });
        }

        // rebuild the transform by traveling from parent to self
        // WITHOUT using the existing this.transform that will be updated
        final Transform parentToF1 = parent.getTransformTo(f1, date);
        final Transform f2ToSelf   = f2.getTransformTo(this, date);
        final Transform f1ToSelf   = new Transform(f1Tof2, f2ToSelf);
        updateTransform(new Transform(parentToF1, f1ToSelf));

    }

    /** Find the deepest common ancestor of two frames in the frames tree.
     * @param from origin frame
     * @param to destination frame
     * @return an ancestor frame of both <code>from</code> and <code>to</code>
     */
    private static Frame findCommon(Frame from, Frame to) {

        // have we already computed the common frame for this pair ?
        Frame common = (Frame) from.commons.get(to);
        if (common != null) {
            return common;
        }

        // definitions of the path up to the head tree for each frame
        final LinkedList pathFrom = from.pathToRoot();
        final LinkedList pathTo   = to.pathToRoot();

        if (pathFrom.isEmpty()||pathTo.contains(from)) { // handle root case and same branch case
            common = from;
        }
        if (pathTo.isEmpty()||pathFrom.contains(to)) { // handle root case and same branch case
            common = to;
        }
        if (common != null) {
            from.commons.put(to, common);
            to.commons.put(from, common);
            return common;
        }

        // at this stage pathFrom contains at least one frame
        Frame lastFrom = (Frame) pathFrom.removeLast();
        common = lastFrom; // common must be one of the instance of Frame already defined

        // at the beginning of the loop pathTo contains at least one frame
        for (Frame lastTo = (Frame) pathTo.removeLast();
             (lastTo == lastFrom) && (lastTo != null) && (lastFrom != null);
             lastTo = (Frame) (pathTo.isEmpty() ? null : pathTo.removeLast())) {
            common = lastFrom;
            lastFrom = (Frame) (pathFrom.isEmpty() ? null : pathFrom.removeLast());
        }

        from.commons.put(to, common);
        to.commons.put(from, common);
        return common;

    }

    /** Determine if a Frame is a child of another one.
     * @param potentialAncestor supposed ancestor frame
     * @return true if the potentialAncestor belongs to the
     * path from instance to the root frame
     */
    public boolean isChildOf(Frame potentialAncestor) {
        for (Frame frame = parent; frame != null; frame = frame.parent) {
            if (frame == potentialAncestor) {
                return true;
            }
        }
        return false;
    }

    /** Get the path from instance frame to the root frame.
     * @return path from instance to root, excluding instance itself
     * (empty if instance is root)
     */
    private LinkedList pathToRoot() {
        final LinkedList path = new LinkedList();
        for (Frame frame = parent; frame != null; frame = frame.parent) {
            path.add(frame);
        }
        return path;
    }

    /** Frame Type enum for the
     * {@link Frame#getReferenceFrame(Frame.FrameType, AbsoluteDate)} method.
     */
    public static class FrameType implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = -7876565578577219160L;

        /** Name of the frame type. */
        private final String name;

        /** Build a frame type.
         * @param name name of the frame type
         */
        private FrameType(String name) {
            this.name = name;
        }

        /** Return a string representation of this type.
         * @return string representation of this type (i.e. its name)
         */
        public String toString() {
            return name;
        }

    }

    /** Get one of the 7 unique reference frames.
     * Must be one of {@link #VEIS1950}, {@link #ITRF2000A}, {@link #ITRF2000B},
     * {@link #TIRF2000A}, {@link #TIRF2000B}, {@link #IRF2000A}, {@link #IRF2000B}.
     * @param type the frame type.
     * @param date the current date
     * @return the selected reference frame singleton.
     * @throws OrekitException if the nutation model data embedded in the
     * library cannot be read.
     */
    public static Frame getReferenceFrame(FrameType type, AbsoluteDate date) throws OrekitException {
        if (type == ITRF2000A) {
            if (itrf2000AFrame == null) {
                itrf2000AFrame = new ITRF2000Frame(getReferenceFrame(TIRF2000A, date), date, type.name);
            }
            return itrf2000AFrame;
        }
        if (type == ITRF2000B) {
            if (itrf2000BFrame == null) {
                itrf2000BFrame = new ITRF2000Frame(getReferenceFrame(TIRF2000B, date), date, type.name);
            }
            return itrf2000BFrame;
        }
        if (type == TIRF2000A) {
            if (tirf2000AFrame == null) {
                tirf2000AFrame = new TIRF2000Frame(getReferenceFrame(IRF2000A, date), date, type.name);
            }
            return tirf2000AFrame;
        }
        if (type == TIRF2000B) {
            if (tirf2000BFrame == null) {
                tirf2000BFrame = new TIRF2000Frame(getReferenceFrame(IRF2000B, date), date, type.name);
            }
            return tirf2000BFrame;
        }
        if (type == IRF2000A) {
            if (irf2000AFrame == null) {
                irf2000AFrame = new IRF2000Frame(date, false, type.name);
            }
            return irf2000AFrame;
        }
        if (type == IRF2000B) {
            if (irf2000BFrame == null) {
                irf2000BFrame = new IRF2000Frame(date, true, type.name);
            }
            return irf2000BFrame;
        }
        if (type == VEIS1950) {
            return getVeis1950();
        }

        OrekitException.throwIllegalArgumentException("unknown frame type {0}, known types: " +
                                                      "{1}, {2}, {3}, {4}, {5}, {6} and {7}",
                                                      new Object[] {
                                                          type, ITRF2000A, ITRF2000B, TIRF2000A,
                                                          TIRF2000B, IRF2000A, IRF2000B, VEIS1950
                                                      });

        // in fact, this is never reached
        return null;

    }

    /** Get the unique Veis 1950 frame.
     * <p>This frame is sometimes refered to as
     * <em>&gamma;<sub>50</sub> CNES</em></p>
     * @return the uniq instance of the Veis 1950 frame
     */
    private static Frame getVeis1950() {
        if (veis1950Frame == null) {
            final double q1 = -2.01425201682020570e-5;
            final double q2 = -2.43283773387856897e-3;
            final double q3 =  5.59078052583013584e-3;
            final double q0 = Math.sqrt(1.0 - q1 * q1 - q2 * q2 - q3 * q3);
            final Transform t = new Transform(new Rotation(q0, q1, q2, q3, true));
            veis1950Frame = new Frame(getJ2000(), t, "Veis1950");
        }
        return veis1950Frame;
    }

}
