package nl.tudelft.lifetiles.graph.controller;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.shape.Rectangle;
import nl.tudelft.lifetiles.annotation.model.KnownMutation;
import nl.tudelft.lifetiles.annotation.model.KnownMutationMapper;
import nl.tudelft.lifetiles.annotation.model.KnownMutationParser;
import nl.tudelft.lifetiles.core.controller.AbstractController;
import nl.tudelft.lifetiles.core.controller.MenuController;
import nl.tudelft.lifetiles.core.util.Message;
import nl.tudelft.lifetiles.core.util.Timer;
import nl.tudelft.lifetiles.graph.model.DefaultGraphParser;
import nl.tudelft.lifetiles.graph.model.FactoryProducer;
import nl.tudelft.lifetiles.graph.model.Graph;
import nl.tudelft.lifetiles.graph.model.GraphContainer;
import nl.tudelft.lifetiles.graph.model.GraphFactory;
import nl.tudelft.lifetiles.graph.model.GraphParser;
import nl.tudelft.lifetiles.graph.model.StackedMutationContainer;
import nl.tudelft.lifetiles.graph.view.DiagramView;
import nl.tudelft.lifetiles.graph.view.TileView;
import nl.tudelft.lifetiles.graph.view.VertexView;
import nl.tudelft.lifetiles.notification.controller.NotificationController;
import nl.tudelft.lifetiles.notification.model.NotificationFactory;
import nl.tudelft.lifetiles.sequence.controller.SequenceController;
import nl.tudelft.lifetiles.sequence.model.Sequence;
import nl.tudelft.lifetiles.sequence.model.SequenceSegment;

/**
 * The controller of the graph view.
 *
 * @author Joren Hammudoglu
 * @author AC Langerak
 * @author Jos Winter
 *
 */
public class GraphController extends AbstractController {

    /**
     * The message to display when operations are attempted without a graph
     * being loaded.
     */
    private static final String NOT_LOADED_MSG = "Graph not loaded"
            + " while attempting to add known mutations.";

    /**
     * The pane that will be used to draw the scrollpane and toolbar on the
     * screen.
     */
    @FXML
    private BorderPane wrapper;

    /**
     * The scrollPane element.
     */
    @FXML
    private ScrollPane scrollPane;

    /**
     * The model of the graph.
     */
    private GraphContainer model;

    /**
     * The view of the graph.
     */
    private TileView view;

    /**
     * The model of the diagram.
     */
    private StackedMutationContainer diagram;

    /**
     * The view of the diagram.
     */
    private DiagramView diagramView;

    /**
     * The view controller.
     */
    private Graph<SequenceSegment> graph;

    /**
     * Current end position of a bucket.
     */
    private int currEndPosition = -1;

    /**
     * Current start position of a bucket.
     */
    private int currStartPosition = -1;

    /**
     * boolean to indicate if the controller must repaint the current position.
     */
    private boolean repaintNow;

    /**
     * The initial value for zoomlevel.
     */
    private static final int DEFAULTZOOMLEVEL = 45;

    /**
     * The current zoom level.
     */
    private int zoomLevel = DEFAULTZOOMLEVEL;

    /**
     * The current zoom level, used to only redraw if the zoomlevel changes.
     */
    private int currentZoomLevel;

    /**
     * Offset for initial scale.
     */
    private static final double SCALE_OFFSET = 5;
    /**
     * The current scale to resize the graph.
     */
    private double scale = Math.pow(ZOOM_OUT_FACTOR, zoomLevel - SCALE_OFFSET);

    /**
     * The currently inserted known mutations.
     */
    private Map<SequenceSegment, List<KnownMutation>> knownMutations;

    /**
     * The factor that each zoom in step that updates the current scale.
     */
    private static final double ZOOM_IN_FACTOR = 1.3125;

    /**
     * Visible sequences in the graph.
     */
    private Set<Sequence> visibleSequences;

    /**
     * The current reference in the graph, shouted by the sequence control.
     */
    private Sequence reference;

    /**
     * The mini map controller.
     */
    private MiniMapController miniMapController;

    /**
     * The factor that each zoom out step that updates the current scale.
     */
    private static final double ZOOM_OUT_FACTOR = 1 / ZOOM_IN_FACTOR;

    /**
     * Maximal zoomed in level.
     */
    private static final int MAX_ZOOM = 50;

    /**
     * {@inheritDoc}
     */
    @Override
    public final void initialize(final URL location,
            final ResourceBundle resources) {
        initListeners();
        initZoomToolBar();

        repaintNow = false;
        scrollPane = new ScrollPane();
    }

    /**
     * Initialize the zoom toolbar.
     */
    private void initZoomToolBar() {
        Zoombar toolbar = new Zoombar(zoomLevel, MAX_ZOOM);
        wrapper.setRight(toolbar.getToolBar());

        toolbar.getZoomlevel().addListener((observeVal, oldVal, newVal) -> {
            int diffLevel = oldVal.intValue() - newVal.intValue();
            zoomLevel = Math.abs(newVal.intValue());
            if (diffLevel < 0) {
                zoomGraph(Math.pow(ZOOM_OUT_FACTOR, diffLevel * -1));
            } else if (diffLevel > 0) {
                zoomGraph(Math.pow(ZOOM_IN_FACTOR, diffLevel));
            }
        });
    }

    /**
     * @return the mini map controller
     */
    private MiniMapController getMiniMapController() {
        if (miniMapController == null) {
            miniMapController = new MiniMapController(scrollPane, model);
        }
        return miniMapController;
    }

    /**
     * Initialize the listeners.
     */
    private void initListeners() {

        NotificationFactory notFact = new NotificationFactory();

        listen(Message.OPENED,
                (controller, subject, args) -> {

                    assert controller instanceof MenuController;
                    if (!"graph".equals(subject)) {
                        return;
                    }
                    assert args.length == 2;
                    assert args[0] instanceof File && args[1] instanceof File;

                    try {
                        loadGraph((File) args[0], (File) args[1]);
                    } catch (IOException exception) {
                        shout(NotificationController.NOTIFY, "",
                                notFact.getNotification(exception));
                    }
                });

        listen(Message.FILTERED, (controller, subject, args) -> {
            assert args.length == 1;
            assert (args[0] instanceof Set<?>);
            // unfortunately java doesn't really let us typecheck generics :(
                visibleSequences = (Set<Sequence>) args[0];
                model.setVisible(visibleSequences);
                diagram = new StackedMutationContainer(model.getBucketCache(),
                        visibleSequences);
                repaintNow = true;
                repaint();
            });

        listen(SequenceController.REFERENCE_SET,
                (controller, subject, args) -> {
                    assert args.length == 1;
                    assert args[0] instanceof Sequence;
                    reference = (Sequence) args[0];
                    model = new GraphContainer(graph, reference);
                    model.setVisible(visibleSequences);
                    diagram = new StackedMutationContainer(model
                            .getBucketCache(), visibleSequences);
                    repaintNow = true;
                    repaint();
                });

        listen(Message.OPENED,
                (controller, subject, args) -> {
                    assert controller instanceof MenuController;
                    if (!subject.equals("known mutations")) {
                        return;
                    }
                    assert args[0] instanceof File;

                    if (graph == null) {
                        shout(NotificationController.NOTIFY, "", notFact
                                .getNotification(new IllegalStateException(
                                        NOT_LOADED_MSG)));
                    } else {
                        try {
                            insertAnnotations((File) args[0]);
                        } catch (IOException exception) {
                            shout(NotificationController.NOTIFY, "",
                                    notFact.getNotification(exception));
                        }
                    }
                });
    }

    /**
     *
     * @return the currently loaded graph.
     */
    public final Graph<SequenceSegment> getGraph() {
        if (graph == null) {
            throw new IllegalStateException("Graph not loaded.");
        }
        return graph;
    }

    /**
     * Load a new graph from the specified file.
     *
     *
     * @param vertexfile
     *            The file to get vertices for.
     * @param edgefile
     *            The file to get edges for.
     * @throws IOException
     *             When an IO error occurs while reading one of the files.
     */
    private void loadGraph(final File vertexfile, final File edgefile)
            throws IOException {
        // create the graph
        GraphFactory<SequenceSegment> factory = FactoryProducer.getFactory();
        GraphParser parser = new DefaultGraphParser();
        graph = parser.parseGraph(vertexfile, edgefile, factory);
        knownMutations = new HashMap<>();

        model = new GraphContainer(graph, reference);
        diagram = new StackedMutationContainer(model.getBucketCache(),
                visibleSequences);

        shout(Message.LOADED, "sequences", parser.getSequences());
        repaintNow = true;
        repaint();
    }

    /**
     * Inserts a list of annotations onto the graph from the specified file.
     *
     * @param file
     *            The file to get annotations from.
     * @throws IOException
     *             When an IO error occurs while reading one of the files.
     */
    private void insertAnnotations(final File file) throws IOException {
        Timer timer = Timer.getAndStart();
        knownMutations = KnownMutationMapper.mapAnnotations(graph,
                KnownMutationParser.parseKnownMutations(file), reference);

        timer.stopAndLog("Inserting annotations");
        repaintNow = true;
        repaintPosition(scrollPane.hvalueProperty().doubleValue());
    }

    /**
     * Repaints the view.
     */
    private void repaint() {
        if (graph != null) {
            if (model == null) {
                model = new GraphContainer(graph, reference);
            }
            if (diagram == null) {
                diagram = new StackedMutationContainer(model.getBucketCache(),
                        visibleSequences);
            }
            view = new TileView(this);
            diagramView = new DiagramView();

            scrollPane.hvalueProperty().addListener(
                    (observable, oldValue, newValue) -> {
                        repaintPosition(newValue.doubleValue());
                    });

            repaintPosition(scrollPane.hvalueProperty().doubleValue());
        }
        getMiniMapController().drawMiniMap();
    }

    /**
     * Find the start and end bucket on the screenm given the position of the
     * scrollbar.
     *
     * @param position
     *            horizontal position of the scrollbar
     * @return an array where the first element is the start bucket and the last
     *         one is the end bucket
     */
    private int[] getStartandEndBucket(final double position) {
        double scaledVertex = scale * VertexView.HORIZONTALSCALE;
        double graphWidth = getMaxUnifiedEnd(graph) * scaledVertex;
        double screenWidth = scrollPane.getViewportBounds().getWidth();
        double scaledScreenWidth = 2d * screenWidth * scale;

        double relativePosition = (position * screenWidth) / 2d + position
                * graphWidth;

        double start = (relativePosition - scaledScreenWidth) / scaledVertex;
        double end = (relativePosition + scaledScreenWidth) / scaledVertex;
        int[] buckets = new int[] {
                getStartBucketPosition(start), getEndBucketPosition(end) + 1
        };

        return buckets;
    }

    /**
     * Repaints the view indicated by the bucket in the given position.
     *
     * @param position
     *            Position in the scrollPane.
     */
    private void repaintPosition(final double position) {
        int zoomSwitchLevel = MAX_ZOOM - diagram.getLevel();
        if (zoomLevel > zoomSwitchLevel) {
            if (currentZoomLevel != zoomLevel || repaintNow) {
                Group diagramDrawing = new Group();
                double width = getMaxUnifiedEnd(graph) * scale
                        * VertexView.HORIZONTALSCALE;
                int diagramLevel = zoomLevel - zoomSwitchLevel;
                diagramDrawing.getChildren().add(
                        diagramView.drawDiagram(diagram, diagramLevel, width));
                diagramDrawing.getChildren().add(new Rectangle(width, 0));
                scrollPane.setContent(diagramDrawing);
                wrapper.setCenter(scrollPane);

                currentZoomLevel = zoomLevel;
                repaintNow = false;
            }
        } else {
            int[] bucketLocations = getStartandEndBucket(position);

            int startBucket = bucketLocations[0];
            int endBucket = bucketLocations[1];

            if (currEndPosition != endBucket
                    && currStartPosition != startBucket || repaintNow) {
                Group graphDrawing = new Group();
                graphDrawing.setManaged(false);
                graphDrawing.getChildren().add(
                        drawGraph(startBucket, endBucket));
                graphDrawing.getChildren().add(
                        new Rectangle(getMaxUnifiedEnd(graph) * scale
                                * VertexView.HORIZONTALSCALE, 0));

                scrollPane.setContent(graphDrawing);
                wrapper.setCenter(scrollPane);

                currEndPosition = endBucket;
                currStartPosition = startBucket;

                repaintNow = false;
            }
        }
    }

    /**
     * Zoom on the current graph given a zoomFactor.
     *
     * @param zoomFactor
     *            factor bigger than 1 makes the graph bigger
     *            between 0 and 1 makes the graph smaller
     */
    private void zoomGraph(final double zoomFactor) {
        scale *= zoomFactor;
        repaintNow = true;
        repaint();
    }

    /**
     * Get the maximal unified end position based on the sinks of the graph.
     *
     * @param graph
     *            Graph for which the width must be calculated.
     * @return the maximal unified end position.
     */
    private long getMaxUnifiedEnd(final Graph<SequenceSegment> graph) {
        long max = 0;
        for (SequenceSegment vertex : graph.getSinks()) {
            if (max < vertex.getUnifiedEnd()) {
                max = vertex.getUnifiedEnd();
            }
        }
        return max;
    }

    /**
     * Return the start position in the bucket.
     *
     * @param position
     *            Position in the scrollPane.
     * @return position in the bucket.
     */
    private int getStartBucketPosition(final double position) {
        return model.getBucketCache().bucketStartPosition(position);
    }

    /**
     * Return the end position in the bucket.
     *
     * @param position
     *            Position in the scrollPane.
     * @return position in the bucket.
     */
    private int getEndBucketPosition(final double position) {
        return model.getBucketCache().bucketEndPosition(position);
    }

    /**
     * Creates a drawable object of the graph from the model.
     *
     * It will draw from the startBucket all the way to the endBucket.
     *
     * @param startBucket
     *            the first buket
     * @param endBucket
     *            the last bucket
     * @return Group object to be drawn on the screen
     */
    public final Group drawGraph(final int startBucket, final int endBucket) {
        Group test = view.drawGraph(
                model.getVisibleSegments(startBucket, endBucket), graph,
                knownMutations, scale);

        return test;
    }

    /**
     * Set that this segment is selected and set those sequences visible.
     *
     * @param segment
     *            The selected segment
     */
    public void clicked(final SequenceSegment segment) {
        shout(Message.FILTERED, "", segment.getSources());
    }

    /**
     * Set that this segment is hovered over.
     *
     * @param segment
     *            the hovered element
     * @param hovering
     *            set if mouse is entering this segment or leaving
     */
    public void hovered(final SequenceSegment segment, final Boolean hovering) {
        // TODO: Message to say that a segment is hovered over
    }
}
