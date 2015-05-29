package nl.tudelft.lifetiles.graph.model;


/**
 * Factory interface for Graphs.
 *
 * @author Rutger van den Berg
 * @param <V>
 *            the vertex type.
 */
public interface GraphFactory<V> {
    /**
     * @return a new empty graph.
     */
    Graph<V> getGraph();
}