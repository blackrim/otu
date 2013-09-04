package opentree.otu;

import jade.tree.JadeNode;
import jade.tree.JadeTree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
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
	public static Set<Node> getDescendantTips(Node ancestor){ // does not appear to be used.
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
	public static String getMetadataJSONForSource(Node sourceMeta) {
		
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
	public static String getMetadataForTree(Node root) {

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
	
	/**
	 * Get the subtree of a given tree graph node. Does not perform error checks to make sure the tree exists.
	 * @param inRoot
	 * @param maxNodes
	 * @return
	 */
	public static JadeTree getTreeFromNode(Node inRoot, int maxNodes) {
		TraversalDescription CHILDOF_TRAVERSAL = Traversal.description().relationships(RelType.CHILDOF, Direction.INCOMING);
		JadeNode root = new JadeNode();
		HashMap<Node, JadeNode> traveledNodes = new HashMap<Node, JadeNode>();
		int maxtips = maxNodes;
		HashSet<Node> includednodes = new HashSet<Node>();
		HashSet<Node> parents = new HashSet<Node>();
		for (Node curGraphNode : CHILDOF_TRAVERSAL.breadthFirst().traverse(inRoot).nodes()) {
			if (includednodes.size() > maxtips && parents.size() > 1) {
				break;
			}
			JadeNode curNode = null;
			if (curGraphNode == inRoot) {
				curNode = root;
			} else {
				curNode = new JadeNode();
			}
			traveledNodes.put(curGraphNode, curNode);
			if (curGraphNode.hasProperty("name")) {
				curNode.setName(GeneralUtils.cleanName(String.valueOf(curGraphNode.getProperty("name"))));
				// curNode.setName(GeneralUtils.cleanName(curNode.getName()));
			}
			if (curGraphNode.hasProperty("ingroup")) {
				curNode.assocObject("ingroup", "true");
			}
			curNode.assocObject("nodeID", String.valueOf(curGraphNode.getId()));
			JadeNode parentJadeNode = null;
			Relationship incomingRel = null;
			if (curGraphNode.hasRelationship(Direction.OUTGOING, RelType.CHILDOF) && curGraphNode != inRoot) {
				Node parentGraphNode = curGraphNode.getSingleRelationship(RelType.CHILDOF, Direction.OUTGOING).getEndNode();
				if (includednodes.contains(parentGraphNode)) {
					includednodes.remove(parentGraphNode);
				}
				parents.add(parentGraphNode);
				if (traveledNodes.containsKey(parentGraphNode)) {
					parentJadeNode = traveledNodes.get(parentGraphNode);
					incomingRel = curGraphNode.getSingleRelationship(RelType.CHILDOF, Direction.OUTGOING);
				}
			}
			includednodes.add(curGraphNode);
			// add the current node to the tree we're building

			if (parentJadeNode != null) {
				parentJadeNode.addChild(curNode);
			}
			// get the immediate synth children of the current node
			LinkedList<Relationship> taxChildRels = new LinkedList<Relationship>();
			int numchild = 0;
			for (Relationship taxChildRel : curGraphNode.getRelationships(Direction.INCOMING, RelType.CHILDOF)) {
				taxChildRels.add(taxChildRel);
				numchild += 1;
			}
			if (numchild > 0) {
				// need to add a property of the jadenode if there are children, so if they aren't included, we can color it
				curNode.assocObject("haschild", true);
				curNode.assocObject("numchild", numchild);
			}
		}
		int curbackcount = 0;
		boolean going = true;
		JadeNode newroot = root;
		Node curRoot = inRoot;
		while (going && curbackcount < 5) {
			if (curRoot.hasRelationship(Direction.OUTGOING, RelType.CHILDOF)) {
				Node curGraphNode = curRoot.getSingleRelationship(RelType.CHILDOF, Direction.OUTGOING).getEndNode();
				JadeNode temproot = new JadeNode();
				if (curGraphNode.hasProperty("name")) {
					temproot.setName(GeneralUtils.cleanName(String.valueOf(curGraphNode.getProperty("name"))));
				}
				temproot.assocObject("nodeID", String.valueOf(curGraphNode.getId()));
				temproot.addChild(newroot);
				curRoot = curGraphNode;
				newroot = temproot;
				curbackcount += 1;
			} else {
				going = false;
				break;
			}
		}
		// (add a bread crumb)
		return new JadeTree(newroot);
	}
}
