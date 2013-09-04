package opentree.otu;

import java.util.HashSet;
import java.util.LinkedList;

import opentree.otu.constants.NodeProperty;
import opentree.otu.constants.RelType;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

/**
 * Static methods for performing common tasks with the database.
 */
public class DatabaseUtils {

	// does not appear to be used
	// this is easy to do by getting the source meta node and getting the property.
	/* 
	 * Get the source id for the source containing the specified tree.
	 * @param root
	 * 		The root node of the tree
	 * @return
	 * 		The source id for the study containing this tree
	 *
	public static String getSourceIdForTree(Node root) {
		return (String) root.getSingleRelationship(RelType.METADATAFOR, Direction.INCOMING)
				.getStartNode().getProperty(NodeProperty.SOURCE_ID.name());
	} */
	
	/**
	 * Count the number of relationships of the specified type and direction connected to the node
	 * @param node
	 * @param relType
	 * @param direction
	 * @return
	 * 		number of relationships
	 */
	public static int getNumberOfRelationships(Node node, RelType relType, Direction direction) {
		int relCount = 0;
		for (Relationship r : node.getRelationships(relType, direction)) {
			relCount++;
		}
		return relCount;
	}

	/**
	 * Count the number of relationships of the specified type connected to the node
	 * @param node
	 * @param relType
	 * @return
	 * 		number of relationships
	 */
	public static int getNumberOfRelationships(Node node, RelType relType) {
		int relCount = 0;
		for (Relationship r : node.getRelationships(relType)) {
			relCount++;
		}
		return relCount;
	}

	/**
	 * Count the number of relationships of the specified direction connected to the node
	 * @param node
	 * @param direction
	 * @return
	 * 		number of relationships
	 */
	public static int getNumberOfRelationships(Node node, Direction direction) {
		int relCount = 0;
		for (Relationship r : node.getRelationships(direction)) {
			relCount++;
		}
		return relCount;
	}
	
	/**
	 * Return the root node from the graph for the tree containing the specified node.
	 * @param node
	 * 		The node to start traversing from
	 * @return
	 */
	public static Node getRootOfTreeContaining(Node node) {
		Node root = node;
		boolean going = true;
		while (going) {
			if (root.hasRelationship(RelType.CHILDOF, Direction.OUTGOING)) {
				root = root.getSingleRelationship(RelType.CHILDOF, Direction.OUTGOING).getEndNode();
			} else {
				break;
			}
		}
		return root;
	}
	
	/**
	 * A convenience wrapper for querying node indexes that validates a unique result. Returns null if no corresponding
	 * node is found.
	 * 
	 * @param index
	 * 		The index to query
	 * @param property
	 * 		The property to use for the query
	 * @param key
	 * 		The value to be searched for under the specified property
	 * @return
	 * 		A single node resulting from the query, or null if no such node exists
	 * @throws IllegalStateException
	 * 		If multiple nodes are found
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
	
	/**
	 * A convenience wrapper for querying node indexes that retrieves all results and then closes the IndexHits object.
	 * Returns an empty Iterable<Node> if no nodes are found. This will be inefficient when the list of results
	 * is large, as it must iterate over the results to store them in the iterator to be returned. For queries that
	 * could produce many hits, use direct access to the node indexes themselves.
	 * 
	 * @param index
	 * 		The index to query
	 * @param property
	 * 		The property to use for the query
	 * @param key
	 * 		The value to be searched for under the specified property
	 * @return
	 * 		An iterable over the nodes resulting from the query
	 */
	public static Iterable<Node> getMultipleNodeIndexHits(Index<Node> index, String property, Object key) {
		LinkedList<Node> result = new LinkedList<Node>();
		IndexHits<Node> hits = null;
		try {
			hits = index.get(property, key);
			for (Node hit : hits) {
				result.add(hit);
			}
		} finally {
			hits.close();
		}
		return result;
	}
	
	/**
	 * Switches all the properties of two nodes. If only one node has any property, it will be removed from that
	 * node and set on the other.
	 * @param n1
	 * @param n2
	 */
	public static void exchangeAllProperties(Node n1, Node n2) {

		HashSet<String> propertiesObserved = new HashSet<String>();
		
		for (String p : n1.getPropertyKeys()) {
			propertiesObserved.add(p);
		}
		
		for (String p : n2.getPropertyKeys()) {
			propertiesObserved.add(p);
		}
		
		for (String p : propertiesObserved) {
			exchangeNodeProperty(n1, n2, p);
		}
	}
	
	/**
	 * Switches the value of the specified property between two nodes. If only one node has the property, it will be
	 * removed from that node and set on the other. If neither node has the property, there is no effect.
	 * @param n1
	 * @param n2
	 * @param propertyName
	 */
	public static void exchangeNodeProperty(Node n1, Node n2, String propertyName) {
		
		if (n1.hasProperty(propertyName)) {
			Object n1Value = n1.getProperty(propertyName);

			if (n2.hasProperty(propertyName)) { // both nodes have the property
				Object n2Value = n2.getProperty(propertyName);
				n2.setProperty(propertyName, n1Value);
				n1.setProperty(propertyName, n2Value);

			} else { // only node 1 has the property
				n2.setProperty(propertyName, n1Value);
				n1.removeProperty(propertyName);	
			}
			
		} else if (n2.hasProperty(propertyName)) { // only node 2 has the property
			Object n2Value = n2.getProperty(propertyName);
			n1.setProperty(propertyName, n2Value);
			n2.removeProperty(propertyName);

		} else { // neither node has the property
			return;
		}
	}
}
