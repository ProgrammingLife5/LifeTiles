package nl.tudelft.lifetiles.graph.model;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import nl.tudelft.lifetiles.core.util.Logging;
import nl.tudelft.lifetiles.sequence.model.DefaultSequence;
import nl.tudelft.lifetiles.sequence.model.SegmentEmpty;
import nl.tudelft.lifetiles.sequence.model.Sequence;
import nl.tudelft.lifetiles.sequence.model.SequenceSegment;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class BucketCacheTest {
    GraphFactory<SequenceSegment> gf;
    SequenceSegment v1, v2;
    Graph<SequenceSegment> gr;

    @BeforeClass
    public static void before() {
        Logging.setLevel(Level.SEVERE);
    }

    @Before
    public void setUp() {
        gf = FactoryProducer.getFactory("JGraphT");
        gr = gf.getGraph();

        Set<Sequence> s = new HashSet<Sequence>();
        s.add(new DefaultSequence("reference"));
        v1 = new SequenceSegment(s, 1, 11, new SegmentEmpty(10));
        v1.setUnifiedStart(1);
        v1.setUnifiedEnd(11);

        v2 = new SequenceSegment(s, 31, 41, new SegmentEmpty(10));
        v2.setUnifiedStart(31);
        v2.setUnifiedEnd(41);
    }

    @Test
    public void testFullBucketCache() {
        gr.addVertex(v1);
        gr.addVertex(v2);
        gr.addEdge(v1, v2);
        BucketCache bc = new BucketCache(1, gr);
        assertEquals(1, bc.getNumberBuckets());
        assertEquals(2, bc.getBuckets().get(0).size());
    }

    @Test
    public void testDivisionBucketCache() {
        gr.addVertex(v1);
        gr.addVertex(v2);
        gr.addEdge(v1, v2);
        BucketCache bc = new BucketCache(2, gr);
        assertEquals(2, bc.getNumberBuckets());
        assertEquals(1, bc.getBuckets().get(0).size());
        assertEquals(1, bc.getBuckets().get(1).size());
    }

    @Test
    public void testEmptyBucketCache() {
        BucketCache bc = new BucketCache(1, gr);
        assertEquals(0, bc.getBuckets().get(0).size());
    }

    @Test
    public void testEmptyNumberBuckets() {
        BucketCache bc = new BucketCache(1, gr);
        assertEquals(1, bc.getNumberBuckets());
    }

}
