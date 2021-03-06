package nl.tudelft.lifetiles.tree.view;

import javafx.geometry.Point2D;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Shape;
import nl.tudelft.lifetiles.sequence.SequenceColor;
import nl.tudelft.lifetiles.sequence.model.Sequence;
import nl.tudelft.lifetiles.tree.model.PhylogeneticTreeItem;

/**
 * A {@link SunburstRingSegment} represents a segment of the sunburst diagrams
 * rings.
 *
 * @author Albert Smit
 *
 */

public class SunburstRingSegment extends AbstractSunburstNode {
    /**
     * the max brightness for the color of a Node.
     */
    private static final double MAX_BRIGHTNESS = 0.8;
    /**
     * The default saturation of the color of a Node.
     */
    private static final double SATURATION = 0.9;
    /**
     * The default name for distance.
     */
    private static final String DISTANCE_NAME = "Branch Length: ";

    /**
     * Creates a SunburstRingSegment.
     *
     *
     * @param value
     *            the {@link PhylogeneticTreeItem} this part of the ring will
     *            represent
     * @param layer
     *            the layer at which it is located in the tree, layer 0 is the
     *            first layer
     * @param angle
     *            the start and end positions of this ringSegment, non null
     * @param center
     *            the coordinates of the center of the circle, non null
     * @param scale
     *            the scaling factor
     */
    public SunburstRingSegment(final PhylogeneticTreeItem value,
            final int layer, final DegreeRange angle,
            final Point2D center, final double scale) {
        // check input
        assert angle != null;
        assert center != null;
        // set the value, and create the text and semi-circle
        setValue(value);
        String name = getValue().getName();
        setDisplay(createRing(layer, angle, center, scale));
        double distance = getValue().getDistance();
        StringBuffer tooltip = new StringBuffer();
        if (name != null) {
            tooltip.append(name).append(System.lineSeparator());
        }
        tooltip.append(DISTANCE_NAME).append(distance);
        setName(new Tooltip(tooltip.toString()));
        setDisplay(createRing(layer, angle, center, scale));

        // add the text and semicircle to the group
        getChildren().add(getDisplay());
    }

    /**
     * Creates a semi-circle with in the specified location.
     *
     * layer starts at 0
     *
     * @param layer
     *            The layer to place this element at, the first child of root is
     *            layer 0.
     * @param angle
     *            The start and end points of this ring
     * @param center
     *            the coordinates of the center of the circle
     * @param scale
     *            the scaling factor
     * @return a semi-circle with the specified dimensions
     */
    private Shape createRing(final int layer, final DegreeRange angle,
            final Point2D center, final double scale) {

        Path result = new Path();

        result.setFill(createColor(angle.getStartAngle(), layer));
        result.setFillRule(FillRule.EVEN_ODD);

        // check if this is a large arc
        boolean largeArc = angle.angle() > AbstractSunburstNode.CIRCLEDEGREES / 2;

        // calculate the radii of the two arcs
        double innerRadius = scale * (CENTER_RADIUS + (layer * RING_WIDTH));
        double outerRadius = innerRadius + scale * RING_WIDTH;

        // convert degrees to radians for Math.sin and Math.cos
        double angleAlpha = Math.toRadians(angle.getStartAngle());
        double angleAlphaNext = Math.toRadians(angle.getEndAngle());

        // draw the semi-circle
        // first go to the start point
        double startX = center.getX() + innerRadius * Math.sin(angleAlpha);
        double startY = center.getY() - innerRadius * Math.cos(angleAlpha);
        MoveTo move1 = new MoveTo(startX, startY);

        // draw a line from point 1 to point 2
        LineTo line1To2 = createLine(outerRadius, center, angleAlpha);

        // draw an arc from point 2 to point 3
        ArcTo arc2To3 = createArc(outerRadius, center, angleAlphaNext, true,
                largeArc);

        // draw a line from point 3 to point 4
        LineTo line3To4 = createLine(innerRadius, center, angleAlphaNext);

        // draw an arc from point 4 back to point 1
        ArcTo arc4To1 = createArc(innerRadius, center, angleAlpha, false,
                largeArc);

        // add all elements to the path
        result.getElements()
                .addAll(move1, line1To2, arc2To3, line3To4, arc4To1);

        return result;
    }

    /**
     * Creates an {@link ArcTo} with the specified parameters.
     *
     * Coordinates of the end point of the arc are given in polar form relative
     * to the center of the arcs.
     *
     * @param radius
     *            The radius of the arc.
     * @param center
     *            The center coordinates of the arc.
     * @param angle
     *            The angle of the end point.
     * @param sweep
     *            The draw direction of the arc.
     * @param largeArc
     *            if true draw an arc larger than 180 degrees.
     * @return an ArcTo with the specified parameters.
     */
    private ArcTo createArc(final double radius, final Point2D center,
            final double angle, final boolean sweep, final boolean largeArc) {
        // calculate the end point of the arc
        double endX = center.getX() + radius * Math.sin(angle);
        double endY = center.getY() - radius * Math.cos(angle);

        // create the arc
        ArcTo result = new ArcTo();
        result.setRadiusX(radius);
        result.setRadiusY(radius);
        result.setX(endX);
        result.setY(endY);
        result.setSweepFlag(sweep);
        result.setLargeArcFlag(largeArc);

        return result;
    }

    /**
     * Creates a {@link LineTo} with the specified parameters.
     *
     * Coordinates of the end point of the arc are given in polar form relative
     * to the center of the arcs.
     *
     * @param radius
     *            The radius of the arc.
     * @param center
     *            The center coordinates of the arc.
     * @param angle
     *            The angle of the end point.
     * @return the LineTo with the specified parameters.
     */
    private LineTo createLine(final double radius, final Point2D center,
            final double angle) {
        // calculate the end point coordinates
        double endX = center.getX() + radius * Math.sin(angle);
        double endY = center.getY() - radius * Math.cos(angle);

        return new LineTo(endX, endY);
    }

    /**
     * Creates a {@link Color} for this node. the color will be red by default,
     * and the color associated with the sequence when the node has a sequence.
     *
     * @param degrees
     *            the location where the ringSeqment is drawn, will become the
     *            hue of the color.
     * @param layer
     *            the layer where the ringSegment is drawn, is used for the
     *            brightness
     * @return a Color object that specifies what color this node will be.
     */
    private Color createColor(final double degrees, final int layer) {
        Sequence sequence = getValue().getSequence();
        if (sequence == null) {
            double brightness = Math.min(MAX_BRIGHTNESS, 1d / layer);
            brightness = Math.abs(brightness - 1);
            return Color.hsb(degrees, SATURATION, brightness);
        } else {
            return SequenceColor.getColor(sequence);
        }
    }

}
