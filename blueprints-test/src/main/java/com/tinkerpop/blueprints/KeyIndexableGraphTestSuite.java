package com.tinkerpop.blueprints;

import com.tinkerpop.blueprints.impls.GraphTest;
import com.tinkerpop.blueprints.util.PropertyFilteredIterable;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class KeyIndexableGraphTestSuite extends TestSuite {

    public KeyIndexableGraphTestSuite() {
    }

    public KeyIndexableGraphTestSuite(final GraphTest graphTest) {
        super(graphTest);
    }

    public void testAutoIndexKeyManagementWithPersistence() {
        KeyIndexableGraph graph = (KeyIndexableGraph) graphTest.generateGraph();
        if (graph.getFeatures().supportsVertexKeyIndex) {
            assertEquals(graph.getIndexedKeys(Vertex.class).size(), 0);
            this.stopWatch();
            graph.createKeyIndex("name", Vertex.class);
            graph.createKeyIndex("location", Vertex.class);
            printPerformance(graph.toString(), 2, "automatic index keys added", this.stopWatch());
            assertEquals(graph.getIndexedKeys(Vertex.class).size(), 2);
            assertTrue(graph.getIndexedKeys(Vertex.class).contains("name"));
            assertTrue(graph.getIndexedKeys(Vertex.class).contains("location"));
        }
        if (graph.getFeatures().supportsEdgeKeyIndex) {
            assertEquals(graph.getIndexedKeys(Edge.class).size(), 0);
            this.stopWatch();
            graph.createKeyIndex("weight", Edge.class);
            graph.createKeyIndex("since", Edge.class);
            printPerformance(graph.toString(), 2, "automatic index keys added", this.stopWatch());
            assertEquals(graph.getIndexedKeys(Edge.class).size(), 2);
            assertTrue(graph.getIndexedKeys(Edge.class).contains("weight"));
            assertTrue(graph.getIndexedKeys(Edge.class).contains("since"));
        }
        graph.shutdown();

        if (graph.getFeatures().isPersistent) {
            graph = (KeyIndexableGraph) graphTest.generateGraph();
            if (graph.getFeatures().supportsVertexKeyIndex) {
                assertEquals(graph.getIndexedKeys(Vertex.class).size(), 2);
                assertTrue(graph.getIndexedKeys(Vertex.class).contains("name"));
                assertTrue(graph.getIndexedKeys(Vertex.class).contains("location"));
            }
            if (graph.getFeatures().supportsEdgeKeyIndex) {
                assertEquals(graph.getIndexedKeys(Edge.class).size(), 2);
                assertTrue(graph.getIndexedKeys(Edge.class).contains("weight"));
                assertTrue(graph.getIndexedKeys(Edge.class).contains("since"));
            }
            graph.shutdown();
        }
    }

    public void testAutoIndexKeyDroppingWithPersistence() {
        testAutoIndexKeyManagementWithPersistence();
        KeyIndexableGraph graph = (KeyIndexableGraph) graphTest.generateGraph();
        if (graph.getFeatures().isPersistent) {
            if (graph.getFeatures().supportsVertexKeyIndex) {
                graph.dropKeyIndex("name", Vertex.class);
            }
            if (graph.getFeatures().supportsEdgeKeyIndex) {
                graph.dropKeyIndex("weight", Edge.class);
            }
            graph.shutdown();

            graph = (KeyIndexableGraph) graphTest.generateGraph();
            if (graph.getFeatures().supportsVertexKeyIndex) {
                assertEquals(graph.getIndexedKeys(Vertex.class).size(), 1);
                assertTrue(graph.getIndexedKeys(Vertex.class).contains("location"));
                graph.dropKeyIndex("location", Vertex.class);
            }
            if (graph.getFeatures().supportsEdgeKeyIndex) {
                assertEquals(graph.getIndexedKeys(Edge.class).size(), 1);
                assertTrue(graph.getIndexedKeys(Edge.class).contains("since"));
                graph.dropKeyIndex("since", Edge.class);
            }
            graph.shutdown();
            graph = (KeyIndexableGraph) graphTest.generateGraph();
            if (graph.getFeatures().supportsVertexKeyIndex) {
                assertEquals(graph.getIndexedKeys(Vertex.class).size(), 0);
            }
            if (graph.getFeatures().supportsEdgeKeyIndex) {
                assertEquals(graph.getIndexedKeys(Edge.class).size(), 0);
            }
        }
        graph.shutdown();
    }

    public void testGettingVerticesAndEdgesWithKeyValue() {
        KeyIndexableGraph graph = (KeyIndexableGraph) graphTest.generateGraph();
        if (graph.getFeatures().supportsVertexIteration && graph.getFeatures().supportsVertexKeyIndex) {
            graph.createKeyIndex("name", Vertex.class);
            assertEquals(graph.getIndexedKeys(Vertex.class).size(), 1);
            assertTrue(graph.getIndexedKeys(Vertex.class).contains("name"));
            Vertex v1 = graph.addVertex(null);
            v1.setProperty("name", "marko");
            v1.setProperty("location", "everywhere");
            Vertex v2 = graph.addVertex(null);
            v2.setProperty("name", "stephen");
            v2.setProperty("location", "everywhere");


            assertEquals(count(graph.getVertices("name", "marko")), 1);
            assertEquals(count(graph.getVertices("name", "stephen")), 1);
            assertEquals(graph.getVertices("name", "marko").iterator().next(), v1);
            assertEquals(graph.getVertices("name", "stephen").iterator().next(), v2);
            try {
                graph.getVertices("location", "everywhere");
                fail();
            } catch (Exception e) {
            }
            try {
                graph.getVertices("bloop", 50);
                fail();
            } catch (Exception e) {
            }

            assertFalse(graph.getVertices("name", "marko") instanceof PropertyFilteredIterable);
            assertFalse(graph.getVertices("name", "rodriguez") instanceof PropertyFilteredIterable);
            assertFalse(graph.getVertices("name", 768) instanceof PropertyFilteredIterable);
        }

        if (graph.getFeatures().supportsEdgeIteration && graph.getFeatures().supportsEdgeKeyIndex) {
            graph.createKeyIndex("location", Edge.class);
            assertEquals(graph.getIndexedKeys(Edge.class).size(), 1);
            assertTrue(graph.getIndexedKeys(Edge.class).contains("location"));

            Edge e1 = graph.addEdge(null, graph.addVertex(null), graph.addVertex(null), "knows");
            e1.setProperty("name", "marko");
            e1.setProperty("location", "everywhere");
            Edge e2 = graph.addEdge(null, graph.addVertex(null), graph.addVertex(null), "knows");
            e2.setProperty("name", "stephen");
            e2.setProperty("location", "everywhere");

            assertEquals(count(graph.getEdges("location", "everywhere")), 2);
            try {
                graph.getEdges("name", "marko");
                fail();
            } catch (IllegalStateException e) {
            }
            try {
                graph.getEdges("bloop", 50);
                fail();
            } catch (IllegalStateException e) {
            }
        }
        graph.shutdown();
    }

    public void testReIndexingOfElements() {
        KeyIndexableGraph graph = (KeyIndexableGraph) graphTest.generateGraph();
        if (graph.getFeatures().supportsVertexKeyIndex) {
            Vertex vertex = graph.addVertex(null);
            vertex.setProperty("name", "marko");

            try {
                graph.getVertices("name", "marko");
                fail();
            } catch (IllegalStateException e) {
            }
            graph.createKeyIndex("name", Vertex.class);
            assertEquals(count(graph.getVertices("name", "marko")), 1);
            assertEquals(graph.getVertices("name", "marko").iterator().next(), vertex);
        }

        if (graph.getFeatures().supportsEdgeKeyIndex) {
            Edge edge = graph.addEdge(null, graph.addVertex(null), graph.addVertex(null), "knows");
            edge.setProperty("date", 2012);

            try {
                graph.getEdges("date", 2012);
                fail();
            } catch (IllegalStateException e) {
            }
            graph.createKeyIndex("date", Edge.class);
            assertEquals(count(graph.getEdges("date", 2012)), 1);
            assertEquals(graph.getEdges("date", 2012).iterator().next(), edge);
        }
        graph.shutdown();
    }

    public void testNoConcurrentModificationException() {
        KeyIndexableGraph graph = (KeyIndexableGraph) graphTest.generateGraph();
        if (graph.getFeatures().supportsEdgeKeyIndex) {
            graph.createKeyIndex("key", Edge.class);
            for (int i = 0; i < 25; i++) {
                graph.addEdge(null, graph.addVertex(null), graph.addVertex(null), "test").setProperty("key", "value");
            }
            assertEquals(count(graph.getVertices()), 50);
            assertEquals(count(graph.getEdges()), 25);
            int counter = 0;
            for (final Edge edge : graph.getEdges("key", "value")) {
                graph.removeEdge(edge);
                counter++;
            }
            assertEquals(counter, 25);
            assertEquals(count(graph.getVertices()), 50);
            assertEquals(count(graph.getEdges()), 0);

        }
        graph.shutdown();
    }
}