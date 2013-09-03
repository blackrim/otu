package opentree.otu;

import opentree.otu.constants.RelType;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.kernel.EmbeddedGraphDatabase;

/**
 * This class is just extended by classes that interact with the db. It provides access to the basic constructors for dealing
 * with different db access methods, and contains a few essential methods that should be available to all tools interacting
 * with the db.
 * 
 * @author cody
 *
 */
public class DatabaseAbstractBase {

	protected GraphDatabaseAgent graphDb;
	
	/**
	 * Access the graph db through the given service object. 
	 * @param graphService
	 */
    public DatabaseAbstractBase(GraphDatabaseService graphService) {
		graphDb = new GraphDatabaseAgent(graphService);
    }

    /**
     * Access the graph db through the given embedded db object.
     * @param embeddedGraph
     */
    public DatabaseAbstractBase(EmbeddedGraphDatabase embeddedGraph) {
    	graphDb = new GraphDatabaseAgent(embeddedGraph);
    }

    /**
     * Access the graph db through the given agent object.
     * @param gdb
     */
    public DatabaseAbstractBase(GraphDatabaseAgent gdb) {
    	graphDb = gdb;
    }
    
    /**
     * Return an Index<Node> object pointing at the index specified by `indexDesc`
     * @param indexDesc
     * @return node index
     */
	public Index<Node> getNodeIndex(NodeIndexDescription indexDesc) {
		return graphDb.getNodeIndex(indexDesc.name);
	}
    
	/**
	 * Return the GraphDatabaseAgent so that you can do things with the graph
	 * @return GraphDatabaseAgent
	 */
	public GraphDatabaseAgent getGraphDatabaseAgent(){
		return graphDb;
	}
	/**
	 * Just close the db.
	 */
	public void shutdownDB(){
		graphDb.shutdownDb();
	}
	
	/**
	 * A convenience wrapper for querying node indexes that validates a unique result. Returns null if no corresponding
	 * node is found.
	 * @param index
	 * @param property
	 * @param key
	 * @return
	 */
	public static Node getSingleNodeIndexHit(Index<Node> index, String property, Object key) {
		Node result = null;
		IndexHits<Node> hits = null;
		try {
			hits = index.get(property, key);
			if (hits.size() == 1) {
				result = hits.getSingle();
				
			} else if (hits.size() > 1) {
				throw new IllegalStateException("More than one hit found for " + property + " == " + key + ". "
						+ "The database is probably corrupt.");
			}
		} finally {
			hits.close();
		}
		return result;
	}
	
}
