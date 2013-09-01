package opentree.otu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import jade.tree.JadeNode;
import jade.tree.JadeTree;

import opentree.otu.GeneralUtils;
import opentree.otu.constants.RelType;

import org.json.simple.parser.JSONParser;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.kernel.Traversal;

public class DatabaseManager extends DatabaseAbstractBase {

// 	private GraphDatabaseAgent graphDb; // moved to abstract class for extensibility
//	private DatabaseIndexer indexer;

/*	protected static Index<Node> sourceMetaIndex; // the metadata node for the source given sourceID (aka studyID), for ALL indexed studies
//	protected static Index<Node> importedSourceMetaIndex; // the metadata node for the source given sourceID (aka studyID), ONLY FOR LOCAL studies
	protected static Index<Node> sourceTreeIndex; // all the trees for a source given sourceID (aka studyID)
	protected static Index<Node> allTreeRootIndex; // the root of the tree given treeID, for ALL indexed trees
	protected static Index<Node> importedTreeRootIndex; // the root of the tree given treeID, ONLY FOR LOCAL trees */
	
	protected Index<Node> sourceMetaIndex = getNodeIndex(NodeIndexDescription.SOURCE_METADATA_NODES_BY_OT_SOURCE_ID);
	protected Index<Node> allTreeRootIndex = getNodeIndex(NodeIndexDescription.TREE_ROOT_NODES_BY_TREE_ID);
	protected Index<Node> importedTreeRootIndex = getNodeIndex(NodeIndexDescription.LOCAL_TREE_ROOT_NODES_BY_TREE_ID);
	protected Index<Node> sourceTreeIndex = getNodeIndex(NodeIndexDescription.TREE_ROOT_NODES_BY_SOURCE_ID);

	// ===== constructors

	/*
	 * public DatabaseManager(String graphName) { // the GraphDatabaseAgent handles this case. Use DatabaseManager(new GraphDatabaseAgent(graphName));
	 * graphDb = new GraphDatabaseAgent(graphName); indexer = new DatabaseIndexer(graphDb);
	 * initNodeIndexes(); }
	 */

	/**
	 * Access the graph db through the given service object.
	 * 
	 * @param graphService
	 */
	public DatabaseManager(GraphDatabaseService graphService) {
		super(graphService);
//		indexer = new DatabaseIndexer(graphDb);
//		initNodeIndexes();
	}

	/**
	 * Access the graph db through the given embedded db object.
	 * 
	 * @param embeddedGraph
	 */
	public DatabaseManager(EmbeddedGraphDatabase embeddedGraph) {
		super(embeddedGraph);
//		indexer = new DatabaseIndexer(graphDb);
//		initNodeIndexes();
	}

	/**
	 * Open the graph db through the given agent object.
	 * 
	 * @param gdb
	 */
	public DatabaseManager(GraphDatabaseAgent gdb) {
		super(gdb);
//		indexer = new DatabaseIndexer(graphDb);
//		initNodeIndexes();
	}

	// ===== importing methods

	/**
	 * Just a placeholder for creating a minimal study
	 * 
	 * @param trees
	 * @param sourceID
	 * @return
	 */
	public boolean addStudyToDB(List<JadeTree> trees, String sourceID) {
//		IndexHits<Node> hits = importedSourceMetaIndex.get("sourceID", sourceID);
		IndexHits<Node> hits = sourceMetaIndex.get("sourceID", sourceID);
		try {
			// won't add an identical source id
			// TODO: return false from the REST so you know, just doesn't show up otherwise
			
			// TODO: update this so it does add any trees that haven't been imported from this study
			
			if (hits.size() > 0) {
				return false;
			}
		} finally {
			hits.close();
		}

		Transaction tx = graphDb.beginTx();
		try {
			Node sourceMeta = graphDb.createNode();
			// if there is info in the first tree as from a nexson we will get it
			addNodeMetaDataOnInput(trees.get(0), sourceMeta, "ot:curatorName");
			addNodeMetaDataOnInput(trees.get(0), sourceMeta, "ot:dataDeposit");
			addNodeMetaDataOnInput(trees.get(0), sourceMeta, "ot:studyPublication");
			addNodeMetaDataOnInput(trees.get(0), sourceMeta, "ot:studyPublicationReference");
			addNodeMetaDataOnInput(trees.get(0), sourceMeta, "ot:studyYear");
			sourceMeta.setProperty("sourceID", sourceID);
			sourceMetaIndex.add(sourceMeta, "sourceID", sourceID);
			for (int i = 0; i < trees.size(); i++) {
				boolean revalue;
				String tid = null;
				if (trees.get(i).getObject("id") != null)
					tid = (String) trees.get(i).getObject("id");
				else
					tid = sourceID + "___" + String.valueOf(i);
				revalue = addTreeToDB(trees.get(i), tid, sourceMeta);
				if (revalue == false) {
					// return false;
					continue;
				}
			}
			tx.success();
		} finally {
			tx.finish();
		}
		return true;
	}

	private boolean addNodeMetaDataOnInput(JadeTree tree, Node dbroot, String propname) {
		if (tree.getObject(propname) != null) {
			dbroot.setProperty(propname, String.valueOf(tree.getObject(propname)));
			return true;
		}
		return false;
	}

	/**
	 * Adds a tree in a JadeTree format into the database.
	 * 
	 * @param intree
	 * @return
	 */
	private boolean addTreeToDB(JadeTree intree, String tree_id, Node metadatanode) {
		Node root = preorderAddTreeToDB(intree.getRoot(), null, tree_id);
		root.setProperty("treeID", tree_id);
		root.setProperty("isroot", true);
		if (intree.getRoot().getObject("ingroup_start") != null) {
			designateIngroup(root);
		}
		// add tree metadata if it is there
		addNodeMetaDataOnInput(intree, root, "ot:branchLengthMode");
		addNodeMetaDataOnInput(intree, root, "ingroup");
		addNodeMetaDataOnInput(intree, root, "ot:inGroupClade");
		addNodeMetaDataOnInput(intree, root, "ot:focalClade");
		addNodeMetaDataOnInput(intree, root, "ot:tag");
		metadatanode.createRelationshipTo(root, RelType.METADATAFOR);
		importedTreeRootIndex.add(root, "treeID", tree_id);
		allTreeRootIndex.add(root, "treeID", tree_id);
		sourceTreeIndex.add(root, "sourceID", (String) metadatanode.getProperty("sourceID"));
		return true;
	}

	public Node rerootTree(Node newroot) {
		// first get the root of the old tree
		Node oldRoot = getRootFromNode(newroot);
		// not rerooting
		if (oldRoot == newroot) {
			Transaction tx1 = graphDb.beginTx();
			try {
				oldRoot.setProperty("rooting_set", "true");
				tx1.success();
			} finally {
				tx1.finish();
			}
			return oldRoot;
		}
		Node actualRoot = null;
		String treeID = null;
		treeID = (String) oldRoot.getProperty("treeID");
		Transaction tx = graphDb.beginTx();
		try {
			// tritomy the root
			int oldrootchildcount = 0;
			for (Relationship rel : oldRoot.getRelationships(RelType.CHILDOF, Direction.INCOMING)) {
				oldrootchildcount += 1;
			}
			if (oldrootchildcount == 2) {
				boolean retvalue = tritomyRoot(oldRoot, newroot);
				if (retvalue == false) {
					tx.success();
					tx.finish();
					return oldRoot;
				}
			}
			// process the reroot

			actualRoot = graphDb.createNode();
			Relationship nrprel = newroot.getSingleRelationship(RelType.CHILDOF, Direction.OUTGOING);
			Node tempParent = nrprel.getEndNode();
			actualRoot.createRelationshipTo(tempParent, RelType.CHILDOF);
			nrprel.delete();
			newroot.createRelationshipTo(actualRoot, RelType.CHILDOF);
			processReroot(actualRoot);

			// clean up metadata
			Relationship trel = oldRoot.getSingleRelationship(RelType.METADATAFOR, Direction.INCOMING);
			oldRoot.setProperty("rooting_set", "true");
			exchangeRootMetaData(oldRoot, actualRoot, "isroot");
			exchangeRootMetaData(oldRoot, actualRoot, "rooting_set");
			exchangeRootMetaData(oldRoot, actualRoot, "ot:branchLengthMode");
			exchangeRootMetaData(oldRoot, actualRoot, "ingroup");
			exchangeRootMetaData(oldRoot, actualRoot, "ot:inGroupClade");
			exchangeRootMetaData(oldRoot, actualRoot, "ot:focalClade");
			exchangeRootMetaData(oldRoot, actualRoot, "ot:tag");
			Node metadata = trel.getStartNode();
			trel.delete();
			actualRoot.setProperty("treeID", treeID);
			metadata.createRelationshipTo(actualRoot, RelType.METADATAFOR);

			// update the indexes
			importedTreeRootIndex.remove(oldRoot, "treeID", treeID);
			allTreeRootIndex.remove(oldRoot, "treeID", treeID);

			importedTreeRootIndex.add(actualRoot, "treeID", treeID);
			allTreeRootIndex.add(actualRoot, "treeID", treeID);

			sourceTreeIndex.remove(oldRoot, "sourceID", (String) metadata.getProperty("sourceID"));
			sourceTreeIndex.add(actualRoot, "sourceID", (String) metadata.getProperty("sourceID"));
			tx.success();
		} finally {
			tx.finish();
		}
		return actualRoot;
	}

	private boolean exchangeRootMetaData(Node oldroot, Node newroot, String property) {
		if (oldroot.hasProperty(property)) {
			newroot.setProperty(property, oldroot.getProperty(property));
			oldroot.removeProperty(property);
			return true;
		}
		return false;
	}

	private boolean tritomyRoot(Node oldRoot, Node newRoot) {
		Node thisNode = null;// this will be the node that is sunk
		// find the first child that is not a tip
		for (Relationship rel : oldRoot.getRelationships(RelType.CHILDOF, Direction.INCOMING)) {
			Node tnode = rel.getStartNode();
			if (tnode.hasRelationship(Direction.INCOMING, RelType.CHILDOF) && tnode.getId() != newRoot.getId()) {
				thisNode = tnode;
				break;
			}
		}
		if (thisNode == null) {
			return false;
		}
		for (Relationship rel : thisNode.getRelationships(RelType.CHILDOF, Direction.INCOMING)) {
			Node eNode = rel.getStartNode();
			eNode.createRelationshipTo(oldRoot, RelType.CHILDOF);
			rel.delete();
		}
		thisNode.getSingleRelationship(RelType.CHILDOF, Direction.OUTGOING).delete();
		thisNode.delete();
		return true;
	}

	private void processReroot(Node innode) {
		if (innode.hasProperty("isroot") || innode.hasRelationship(Direction.INCOMING, RelType.CHILDOF) == false) {
			return;
		}
		Node parent = null;
		if (innode.hasRelationship(Direction.OUTGOING, RelType.CHILDOF)) {
			parent = innode.getSingleRelationship(RelType.CHILDOF, Direction.OUTGOING).getEndNode();
			processReroot(parent);
		}
		// Exchange branch label, length et cetera
		exchangeInfoReroot(parent, innode);
		// Rearrange topology
		innode.getSingleRelationship(RelType.CHILDOF, Direction.OUTGOING).delete();
		parent.createRelationshipTo(innode, RelType.CHILDOF);
	}

	private void exchangeInfoReroot(Node innode1, Node innode2) {
		String swaps = null;
		double swapd;
		if (innode1.hasProperty("name"))
			swaps = (String) innode1.getProperty("name");
		if (innode2.hasProperty("name"))
			innode1.setProperty("name", (String) innode2.getProperty("name"));
		if (swaps != null)
			innode2.setProperty("name", swaps);

		// swapd = node1.getBL();
		// node1.setBL(node2.getBL());
		// node2.setBL(swapd);
	}

	private Node preorderAddTreeToDB(JadeNode innode, Node parent, String tree_id) {
		Node dbnode = graphDb.createNode();
		// add properties
		if (innode.getName() != null) {
			dbnode.setProperty("name", innode.getName());
		}
		// add bl
		// dbnode.setProperty("bl", innode.getBL());
		// add support
		if (parent != null) {
			Relationship trel = dbnode.createRelationshipTo(parent, RelType.CHILDOF);
		}
		for (int i = 0; i < innode.getChildCount(); i++) {
			preorderAddTreeToDB(innode.getChild(i), dbnode, tree_id);
		}
		return dbnode;
	}

	public Node getRootNodeFromTreeID(String treeID) {
//		IndexHits<Node> hits = importedTreeRootIndex.get("treeID", treeID);
		IndexHits<Node> hits = allTreeRootIndex.get("treeID", treeID);
		Node rootNode = hits.getSingle();
		hits.close();
		return rootNode;
	}

	public JadeTree getTreeFromNode(Node inRoot, int maxNodes) {
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

	public Node getRootFromNode(Node innode) {
		// first get the root of the old tree
		Node root = innode;
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

	public void designateIngroup(Node innode) {
		Node root = getRootFromNode(innode);
		TraversalDescription CHILDOF_TRAVERSAL = Traversal.description().relationships(RelType.CHILDOF, Direction.INCOMING);
		Transaction tx = graphDb.beginTx();
		try {
			root.setProperty("ingroup_set", "true");
			if (root != innode) {
				for (Node node : CHILDOF_TRAVERSAL.breadthFirst().traverse(root).nodes()) {
					if (node.hasProperty("ingroup"))
						node.removeProperty("ingroup");
				}
			}
			innode.setProperty("ingroup", "true");
			for (Node node : CHILDOF_TRAVERSAL.breadthFirst().traverse(innode).nodes()) {
				node.setProperty("ingroup", "true");
			}
			tx.success();
		} finally {
			tx.finish();
		}
	}

	public String getStudyMetaData(String studyID) {
//		IndexHits<Node> hits1 = importedSourceMetaIndex.get("sourceID", studyID);
		IndexHits<Node> sourcesFound = sourceMetaIndex.get("sourceID", studyID);
		Node sourcemeta = sourcesFound.getSingle();
		sourcesFound.close();
		StringBuffer bf = new StringBuffer("{ \"metadata\": {\"ot:curatorName\": \"");
		if (sourcemeta.hasProperty("ot:curatorName")) {
			bf.append((String) sourcemeta.getProperty("ot:curatorName"));
		}
		bf.append("\", \"ot:dataDeposit\": \"");
		if (sourcemeta.hasProperty("ot:dataDeposit")) {
			bf.append((String) sourcemeta.getProperty("ot:dataDeposit"));
		}
		bf.append("\", \"ot:studyPublication\": \"");
		if (sourcemeta.hasProperty("ot:studyPublication")) {
			bf.append((String) sourcemeta.getProperty("ot:studyPublication"));
		}
		bf.append("\", \"ot:studyPublicationReference\": \"");
		if (sourcemeta.hasProperty("ot:studyPublicationReference")) {
			bf.append(GeneralUtils.escapeString((String) sourcemeta.getProperty("ot:studyPublicationReference")));
		}
		bf.append("\", \"ot:studyYear\": \"");
		if (sourcemeta.hasProperty("ot:studyYear")) {
			bf.append((String) sourcemeta.getProperty("ot:studyYear"));
		}
		bf.append("\"}, \"trees\" : [");
		// add the trees
		ArrayList<String> trees = new ArrayList<String>();
		for (Relationship rel : sourcemeta.getRelationships(RelType.METADATAFOR, Direction.OUTGOING)) {
			trees.add((String) rel.getEndNode().getProperty("treeID"));
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

	public String getTreeMetaData(String studyID, String treeID) {
		
		// TODO: might need to update this to make sure it is only getting imported trees?
//		IndexHits<Node> treesFound = importedTreeRootIndex.get("treeID", treeID);
//		IndexHits<Node> sourcesFound = importedSourceMetaIndex.get("sourceID", studyID);

		IndexHits<Node> treesFound = allTreeRootIndex.get("treeID", treeID);
		IndexHits<Node> sourcesFound = sourceMetaIndex.get("sourceID", studyID); // TODO: does not seem to be used. remove?
		
		Node sourcemeta = sourcesFound.getSingle();
		Node root = null;
		while (treesFound.hasNext()) {
			Node tnode = treesFound.next();
			if (((String) tnode.getProperty("treeID")).equals(treeID)) {
				root = tnode;
				break;
			}
		}
		sourcesFound.close();
		treesFound.close();
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

	public void deleteTreeFromTreeID(String studyID, String treeID) {
		
		// TODO: update this so it only deletes the downstream part of the tree (not the root)
		// and updates the indexes to reflect that the tree is no longer local

		IndexHits<Node> treesFound = importedTreeRootIndex.get("treeID", treeID);
//		IndexHits<Node> sourcesFound = importedSourceMetaIndex.get("sourceID", studyID); // apparently unused
//		Node sourcemeta = sourcesFound.getSingle();
		Node root = null;

		try {
			while (treesFound.hasNext()) {
				Node tnode = treesFound.next();
				if (((String) tnode.getProperty("treeID")).equals(treeID)) {
					root = tnode;
					break;
				}
			}
		} finally {
//			sourcesFound.close();
			treesFound.close();
		}

		Transaction tx = graphDb.beginTx();
		try {
			HashSet<Node> todelete = new HashSet<Node>();
			TraversalDescription CHILDOF_TRAVERSAL = Traversal.description().relationships(RelType.CHILDOF, Direction.INCOMING);
//			todelete.add(root);
			for (Node curGraphNode : CHILDOF_TRAVERSAL.breadthFirst().traverse(root).nodes()) {
				if (!curGraphNode.equals(root)) {
					todelete.add(curGraphNode);
				}
			}
			for (Node nd : todelete) {
				for (Relationship rel : nd.getRelationships()) {
					rel.delete();
				}
				nd.delete();
			}
			importedTreeRootIndex.remove(root);
			
			tx.success();
		} finally {
			tx.finish();
		}
	}

	public String getJSONOfSourceIDs() {

		// TODO: Should this only be returning the sources that have imported trees?

		StringBuffer retstr = new StringBuffer("{ \"studies\" : [");
		IndexHits<Node> hits = sourceMetaIndex.query("*:*");
		while (hits.hasNext()) {
			Node x = hits.next();
			retstr.append("\"" + (String) x.getProperty("sourceID") + "\" ");
			if (hits.hasNext()) {
				retstr.append(",");
			}
		}
		hits.close();
		retstr.append("] }");
		return retstr.toString();
	}

	public String getJSONOfSourceIDsWithImportedTreeIDs() {

		// TODO: should this be returning all tree ids? Should it only be returning the sources that have imported trees?

		StringBuffer retstr = new StringBuffer("{ \"studies\" : [");
		IndexHits<Node> hits = importedTreeRootIndex.query("*:*");
		while (hits.hasNext()) {
			retstr.append("[ \"");
			Node x = hits.next();
			retstr.append((String) x.getSingleRelationship(RelType.METADATAFOR, Direction.INCOMING).getStartNode().getProperty("sourceID"));
			retstr.append("\", \"");
			retstr.append((String) x.getProperty("treeID"));
			retstr.append("\"]");
			if (hits.hasNext()) {
				retstr.append(",");
			}
		}
		hits.close();
		retstr.append("] }");
		return retstr.toString();
	}

	public void deleteStudyFromStudyID(String studyID) {
//		IndexHits<Node> hits1 = importedSourceMetaIndex.get("sourceID", studyID);
		IndexHits<Node> hits1 = sourceMetaIndex.get("sourceID", studyID);
		Node root = hits1.getSingle();
		hits1.close();
		Transaction tx = graphDb.beginTx();
		try {
			for (Relationship rel : root.getRelationships(RelType.METADATAFOR, Direction.OUTGOING)) {
				String treeID = (String) rel.getEndNode().getProperty("treeID");
				deleteTreeFromTreeID(studyID, treeID);
			}

			/* CHANGED: don't delete the study itself, since it will still be searchable from the indexes
			for (Relationship rel : root.getRelationships()) {
				rel.delete();
			}
			root.delete();
			importedSourceMetaIndex.remove(root); */

			tx.success();
		} finally {
			tx.finish();
		}
	}

	public String getStudyIDFromTreeID(String treeID) {
//		IndexHits<Node> hits = importedTreeRootIndex.get("treeID", treeID);
		IndexHits<Node> hits = allTreeRootIndex.get("treeID", treeID);
		Node rootNode = hits.getSingle();
		hits.close();
		String studyID = (String) rootNode.getSingleRelationship(RelType.METADATAFOR, Direction.INCOMING).getStartNode().getProperty("sourceID");
		return studyID;
	}

	/*
	 * Just initialize the indexes we need during import
	 *
	private void initNodeIndexes() {
		sourceMetaIndex = getNodeIndex(NodeIndexDescription.SOURCE_METADATA_NODES_BY_OT_SOURCE_ID);
//		importedSourceMetaIndex = getNodeIndex(NodeIndexDescription.LOCAL_SOURCE_METADATA_NODES_BY_OT_SOURCE_ID);
		allTreeRootIndex = getNodeIndex(NodeIndexDescription.TREE_ROOT_NODES_BY_TREE_ID);
		importedTreeRootIndex = getNodeIndex(NodeIndexDescription.LOCAL_TREE_ROOT_NODES_BY_TREE_ID);
		sourceTreeIndex = getNodeIndex(NodeIndexDescription.TREE_ROOT_NODES_BY_SOURCE_ID);
	} */
}
