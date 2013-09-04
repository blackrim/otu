package opentree.otu;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import opentree.otu.constants.NodeProperty;
import opentree.otu.constants.RelType;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.kernel.Traversal;

public class DatabaseBrowser extends DatabaseAbstractBase {

	public final Index<Node> treeRootNodesByTreeId = getNodeIndex(NodeIndexDescription.TREE_ROOT_NODES_BY_TREE_ID);
	public final Index<Node> sourceMetaNodesBySourceId = getNodeIndex(NodeIndexDescription.SOURCE_METADATA_NODES_BY_SOURCE_ID);
	
	public DatabaseBrowser(EmbeddedGraphDatabase embeddedGraph) {
		super(embeddedGraph);
	}

	public DatabaseBrowser(GraphDatabaseService gdbs) {
		super(gdbs);
	}

	public DatabaseBrowser(GraphDatabaseAgent gdba) {
		super(gdba);
	}
	
	/**
	 * Returns a JSON array string of studies that have imported trees.
	 * @return
	 */
	public String getJSONOfSourceIdsForImportedTrees() {

		// find all imported trees, get their study ids from the attached metadata nodes
		HashSet<String> studyIds = new HashSet<String>();
		IndexHits<Node> importedTreesFound = null;
		try {
			importedTreesFound = treeRootNodesByTreeId.query(LOCAL_LOCATION + "TreeId", "*");
			for (Node t : importedTreesFound) {
				studyIds.add((String) t.getSingleRelationship(RelType.METADATAFOR, Direction.INCOMING)
						.getStartNode().getProperty(NodeProperty.SOURCE_ID.name()));
			}
		} finally {
			importedTreesFound.close();
		}
		
		// write the string
		StringBuffer json = new StringBuffer("{ \"studies\" : [");
		Iterator<String> studyIdsIter = studyIds.iterator();
		while (studyIdsIter.hasNext()) {
			json.append("\"" + studyIdsIter.next() + "\" ");
			if (studyIdsIter.hasNext()) {
				json.append(",");
			}
		}
		json.append("] }");
		
		return json.toString();
	}

	/**
	 * Return a JSON string of source ids and corresponding tree ids for all locally-imported sources.
	 * @return
	 */
	public String getJSONOfSourceIdsAndTreeIdsForImportedTrees() {

		StringBuffer retstr = new StringBuffer("{ \"studies\" : [");
		IndexHits<Node> hits = treeRootNodesByTreeId.query(LOCAL_LOCATION + "TreeId", "*");
		while (hits.hasNext()) {
			retstr.append("[ \"");
			Node x = hits.next();
			retstr.append((String) x.getSingleRelationship(RelType.METADATAFOR, Direction.INCOMING)
					.getStartNode().getProperty(NodeProperty.SOURCE_ID.name()));
			retstr.append("\", \"");
			retstr.append((String) x.getProperty(NodeProperty.TREE_ID.name()));
			retstr.append("\"]");
			if (hits.hasNext()) {
				retstr.append(",");
			}
		}
		hits.close();
		retstr.append("] }");
		return retstr.toString();
	}
	
	/* // Does not appear to do what it sounds like... Is it used?
	 * get a list of otu nodes based on a study metadatanode
	 *
	public HashSet<Node> getOTUsFromMetadataNode(Node sourceMeta){
		HashSet<Node> reths =  new HashSet<Node>();
		for (Relationship rel: sourceMeta.getRelationships(Direction.OUTGOING, RelType.METADATAFOR)){
			Node treeroot = rel.getEndNode();
			reths.addAll(getDescendantTips(treeroot));
		}
		return reths;
	} */
	 
	/**
	 * Get the set of tip nodes descended from a tree node
	 * 
	 * @param ancestor
	 * 		The start node for the traversal. All tip nodes descended from this node will be included in the result.
	 * @return
	 * 		A set containing the nodes found by the tree traversal. Returns an empty set if no nodes are found.
	 */
	@Deprecated
	public Set<Node> getDescendantTips(Node ancestor){ // does not appear to be used.
		HashSet<Node> descendantTips = new HashSet<Node>();
		TraversalDescription CHILDOF_TRAVERSAL = Traversal.description().relationships(RelType.CHILDOF, Direction.INCOMING);
		for(Node curGraphNode: CHILDOF_TRAVERSAL.breadthFirst().traverse(ancestor).nodes()){
			if(curGraphNode.hasProperty("oty")){ // what is this? should this be "otu"
				descendantTips.add(curGraphNode);
			}
		}
		return descendantTips;
	}
	
	/**
	 * Retrieve a source metadata node from the graph.
	 * @param treeId
	 * 		The id of the tree to get
	 * @param location
	 * 		The location of the study containing the tree to get. For local trees, use DatabaseBrowser.LOCAL_LOCATION
	 * @return
	 * 		The metadata node for this source, or null if no such source exists
	 */
	public Node getSourceMetaNode(String sourceId, String location) {
		return DatabaseUtils.getSingleNodeIndexHit(sourceMetaNodesBySourceId, location + "SourceId", sourceId);
	}
	
	/**
	 * Retrieve a tree root node from the graph.
	 * @param treeId
	 * 		The id of the tree to get
	 * @param location
	 * 		The location of the study containing the tree to get. For local trees, use DatabaseBrowser.LOCAL_LOCATION
	 * @return
	 * 		The root node for this tree, or null if no such tree exists
	 */
	public Node getTreeRootNode(String treeId, String location) {
		return DatabaseUtils.getSingleNodeIndexHit(treeRootNodesByTreeId, location + "TreeId", treeId);
	}
	
	/**
	 * Return a JSON string containing the metadata for the corresponding source. Will fail if the provided node
	 * is not a source metadata node.
	 * 
	 * @param sourceMeta
	 * 		The metadata node for the source
	 * @return
	 * 		A JSON string containing metadata for this source
	 */
	public String getMetadataJSONForSource(Node sourceMeta) {
		
		StringBuffer bf = new StringBuffer("{ \"metadata\": {\"ot:curatorName\": \"");
		if (sourceMeta.hasProperty("ot:curatorName")) {
			bf.append((String) sourceMeta.getProperty("ot:curatorName"));
		}
		bf.append("\", \"ot:dataDeposit\": \"");
		if (sourceMeta.hasProperty("ot:dataDeposit")) {
			bf.append((String) sourceMeta.getProperty("ot:dataDeposit"));
		}
		bf.append("\", \"ot:studyPublication\": \"");
		if (sourceMeta.hasProperty("ot:studyPublication")) {
			bf.append((String) sourceMeta.getProperty("ot:studyPublication"));
		}
		bf.append("\", \"ot:studyPublicationReference\": \"");
		if (sourceMeta.hasProperty("ot:studyPublicationReference")) {
			bf.append(GeneralUtils.escapeString((String) sourceMeta.getProperty("ot:studyPublicationReference")));
		}
		bf.append("\", \"ot:studyYear\": \"");
		if (sourceMeta.hasProperty("ot:studyYear")) {
			bf.append((String) sourceMeta.getProperty("ot:studyYear"));
		}
		bf.append("\"}, \"trees\" : [");
		// add the trees
		ArrayList<String> trees = new ArrayList<String>();
		for (Relationship rel : sourceMeta.getRelationships(RelType.METADATAFOR, Direction.OUTGOING)) {
			trees.add((String) rel.getEndNode().getProperty(NodeProperty.TREE_ID.name()));
		}
		for (int i = 0; i < trees.size(); i++) {
			bf.append("\"" + trees.get(i));
			if (i != trees.size() - 1) {
				bf.append("\",");
			} else {
				bf.append("\"");
			}
		}
		bf.append("]  }");
		return bf.toString();
	}

	/**
	 * Get a JSON string containing tree metadata for the specified tree root node. Will fail if this node is not the
	 * root node of a tree.
	 * 
	 * @param root
	 * 		The root node of a tree
	 * @return
	 * 		A JSON string containing of the metadata for this tree
	 */
	public String getMetadataForTree(Node root) {

		StringBuffer bf = new StringBuffer("{ \"metadata\": {\"ot:branchLengthMode\": \"");
		if (root.hasProperty("ot:branchLengthMode")) {
			bf.append((String) root.getProperty("ot:branchLengthMode"));
		}
		bf.append("\", \"ot:inGroupClade\": \"");
		if (root.hasProperty("ot:inGroupClade")) {
			bf.append((String) root.getProperty("ot:inGroupClade"));
		}
		bf.append("\", \"ot:focalClade\": \"");
		if (root.hasProperty("ot:focalClade")) {
			bf.append((String) root.getProperty("ot:focalClade"));
		}
		bf.append("\", \"ot:tag\": \"");
		if (root.hasProperty("ot:tag")) {
			bf.append((String) root.getProperty("ot:tag"));
		}
		bf.append("\", \"rooting_set\": \"");
		if (root.hasProperty("rooting_set")) {
			bf.append((String) root.getProperty("rooting_set"));
		}
		bf.append("\", \"ingroup_set\": \"");
		if (root.hasProperty("ingroup_set")) {
			bf.append((String) root.getProperty("ingroup_set"));
		}
		bf.append("\"} }");
		return bf.toString();
	}
}
