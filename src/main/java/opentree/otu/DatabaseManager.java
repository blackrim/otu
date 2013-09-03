package opentree.otu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import jade.tree.JadeNode;
import jade.tree.JadeTree;
import opentree.otu.GeneralUtils;
import opentree.otu.constants.GeneralConstants;
import opentree.otu.constants.NodeProperty;
import opentree.otu.constants.RelType;
import opentree.otu.exceptions.NoSuchTreeException;

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

	private DatabaseIndexer indexer;
	
	protected Index<Node> sourceMetaNodesBySourceId = getNodeIndex(NodeIndexDescription.SOURCE_METADATA_NODES_BY_SOURCE_ID);
	protected Index<Node> treeRootNodesByTreeId = getNodeIndex(NodeIndexDescription.TREE_ROOT_NODES_BY_TREE_ID);
//	protected Index<Node> treeRootNodesBySourceId = getNodeIndex(NodeIndexDescription.TREE_ROOT_NODES_BY_OT_SOURCE_ID);

	// ===== constructors

	/**
	 * Access the graph db through the given service object.
	 * 
	 * @param graphService
	 */
	public DatabaseManager(GraphDatabaseService graphService) {
		super(graphService);
		indexer = new DatabaseIndexer(graphDb);
	}

	/**
	 * Access the graph db through the given embedded db object.
	 * 
	 * @param embeddedGraph
	 */
	public DatabaseManager(EmbeddedGraphDatabase embeddedGraph) {
		super(embeddedGraph);
		indexer = new DatabaseIndexer(graphDb);
	}

	/**
	 * Open the graph db through the given agent object.
	 * 
	 * @param gdb
	 */
	public DatabaseManager(GraphDatabaseAgent gdb) {
		super(gdb);
		indexer = new DatabaseIndexer(graphDb);
	}

	// ===== importing methods

	/**
	 * Installs a study into the db, including loading all included trees.
	 * 
	 * @param trees
	 * @param sourceID
	 * @return
	 */
	public void addLocalStudy(List<JadeTree> trees, String sourceID) {
		
		// TODO: return meaningful information about the result to the rest query that calls this method

		Transaction tx = graphDb.beginTx();
		try {
			
			// don't add a study if it already exists
			Node sourceMeta = getSingleNodeIndexHit(sourceMetaNodesBySourceId, "localSourceId", sourceID);

			if (sourceMeta != null) {
				throw new UnsupportedOperationException("Attempt to add a source with the same source id as an "
						+ "existing local source. This would require merging, but merging is not (yet?) supported.");
			}
			
			// install the study. use study properties stored in the first tree
			// TODO: THIS WILL FAIL IF NEXSON READER RETURNS A NULL TREE. FIX NEXSON READER.
			sourceMeta = graphDb.createNode();
			sourceMeta.setProperty(NodeProperty.LOCATION.name(), "local");
			indexer.setStudyMetadataNodePropertiesAndIndex(sourceMeta, trees.get(0), sourceID);

			// attach to corresponding remote study if exists
			Node remoteSourceMeta = (Node) getSingleNodeIndexHit(sourceMetaNodesBySourceId, "remoteSourceId", sourceID);
			if (remoteSourceMeta != null) {
				sourceMeta.createRelationshipTo(remoteSourceMeta, RelType.ISLOCALCOPYOF);
			}
			
			// for each tree
			for (int i = 0; i < trees.size(); i++) {

				// get the tree from the nexson if there is one or create an arbitrary one if not
				String tid_suffix = (String) trees.get(i).getObject("id");
				if (tid_suffix ==  null) {
					tid_suffix = GeneralConstants.LOCAL_TREEID_PREFIX.value + String.valueOf(i);
				}
				String treeId = sourceID + "_" + tid_suffix;

				// add the tree
				addTreeToDB(trees.get(i), treeId, sourceMeta);

			}
			tx.success();
		} finally {
			tx.finish();
		}
	}

	/* moved to DatabaseIndexer to reduce duplication
	private boolean addNodeMetaDataOnInput(JadeTree tree, Node dbroot, String propname) {
		if (tree.getObject(propname) != null) {
			dbroot.setProperty(propname, String.valueOf(tree.getObject(propname)));
			return true;
		}
		return false;
	} */

	/**
	 * Adds a tree in a JadeTree format into the database.
	 * 
	 * @param tree
	 * @return
	 */
	private boolean addTreeToDB(JadeTree tree, String treeId, Node sourceMetaNode) {

		// add the tree to the graph
		Node root = preorderAddTreeToDB(tree.getRoot(), null);
		sourceMetaNode.createRelationshipTo(root, RelType.METADATAFOR);

		// designate the root as the ingroup this is specified in the nexson
		if (tree.getRoot().getObject("ingroup_start") != null) {
			designateIngroup(root);
		}

		// add node properties and add to indexes
		indexer.setTreeRootNodePropertiesAndIndex(root, tree, treeId, (String) sourceMetaNode.getProperty(NodeProperty.SOURCE_ID.name()));
		indexer.addTreeRootNodeToLocalIndexes(root);

		return true; // this doesn't mean anything... we never return false.
	}

	/**
	 * Reroot the tree containing the `newroot` node on that node. Returns the root node of the rerooted tree.
	 * @param newroot
	 * @return
	 */
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
		treeID = (String) oldRoot.getProperty(NodeProperty.TREE_ID.name());
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
			processReroot(actualRoot); // TODO: redundant with the indexer property-switching methods?

			// switch the METADATA_FOR relationship to the new root node
			Relationship prevStudyToTreeRootLinkRel = oldRoot.getSingleRelationship(RelType.METADATAFOR, Direction.INCOMING);
			Node metadata = prevStudyToTreeRootLinkRel.getStartNode();
			prevStudyToTreeRootLinkRel.delete();
			actualRoot.setProperty(NodeProperty.TREE_ID.name(), treeID);
			metadata.createRelationshipTo(actualRoot, RelType.METADATAFOR);
			
			// clean up metadata and index entries
			indexer.exchangeRootPropertiesAndUpdateIndexes(oldRoot, actualRoot);

			tx.success();
		} finally {
			tx.finish();
		}
		return actualRoot;
	}

	/**
	 * Used by the rerooting function
	 * @param oldRoot
	 * @param newRoot
	 * @return
	 */
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

	// TODO: redundant with the indexer property-switching methods?
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

	// TODO: redundant with the indexer property-switching methods?
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

	/**
	 * A recursive function used to replicate the upstream tree structure of the passed in JadeNode in the graph.
	 * @param curJadeNode
	 * @param parentGraphNode
	 * @param tree_id
	 * @return
	 */
	private Node preorderAddTreeToDB(JadeNode curJadeNode, Node parentGraphNode) {

		Node curGraphNode = graphDb.createNode();
		
		// add properties
		if (curJadeNode.getName() != null) {
			curGraphNode.setProperty("name", curJadeNode.getName());
		}

		// TODO: add bl
		// dbnode.setProperty("bl", innode.getBL());
		// TODO: add support
		
		if (parentGraphNode != null) {
			curGraphNode.createRelationshipTo(parentGraphNode, RelType.CHILDOF);
		}

		for (JadeNode childJadeNode : curJadeNode.getChildren()) {
			preorderAddTreeToDB(childJadeNode, curGraphNode);
		}

		return curGraphNode;
	}

	
	/* THIS SHOULD JUST BE AN INDEX CALL. No need to have a special function for this.
	 * 
	 * Get a tree root node from the graph. Will do error checking to make sure tree is available and unique, and will throw an exception if not.
	 * @param treeId
	 * @return
	 * @throws NoSuchTreeException
	 *
	public Node getRootNodeForLocalTree(String treeId) throws NoSuchTreeException {

		Node rootNode = getSingleNodeIndexHit(treeRootNodesByTreeId, "localTreeId", treeId);

		if (rootNode == null) {
			throw new NoSuchTreeException("The tree " + treeId + " has not been imported into the database.");
		}
		return rootNode;
	} */

	/**
	 * Get the subtree of a given tree graph node. Does not perform error checks to make sure the tree exists.
	 * @param inRoot
	 * @param maxNodes
	 * @return
	 */
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

	/**
	 * Return the root graph node for the tree containing `innode`.
	 * @param innode
	 * @return
	 */
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

	/**
	 * Set the ingroup for the tree containing `innode` to `innode`.
	 * @param innode
	 */
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

	/**
	 * Get JSON metadata for a the local source indicated by `sourceId`.
	 * @param sourceId
	 * @return
	 */
	public String getMetadataForLocalSource(String sourceId) {
		return getMetadataForSource(sourceId, "localSourceId");
	}
	
	/**
	 * Abstracted method to allow the same code to deal remote or local sources.
	 * @param sourceId
	 * @param indexPropertyName
	 * @return
	 */
	private String getMetadataForSource(String sourceId, String indexProperty) {

		Node sourceMeta = getSingleNodeIndexHit(sourceMetaNodesBySourceId, indexProperty, sourceId);
		
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
	 * Return a JSON containing tree metadata for the specified local tree.
	 * @param treeId
	 * @return
	 */
	public String getMetadataForLocalTree(String treeId) {
		return getMetadataForTree(treeId, "localTreeId");
	}

	/**
	 * Abstracted method to allow the same code to be used for both local and remote trees.
	 * @param treeId
	 * @param indexProperty
	 * @return
	 */
	public String getMetadataForTree(String treeId, String indexProperty) {

		Node root = getSingleNodeIndexHit(treeRootNodesByTreeId, indexProperty, treeId);

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
	 * Deletes a local tree
	 * @param treeId
	 * @throws NoSuchTreeException 
	 */
	public void deleteLocalTree(String treeId) throws NoSuchTreeException {

		// TODO: update indexes
		
		Node root = getSingleNodeIndexHit(treeRootNodesByTreeId, "localTreeId", treeId);

		if (root == null) {
			throw new NoSuchTreeException("Attempt to delete local tree with id " + treeId + ", but there is no local tree with this id");
		}

		indexer.removeTreeRootNodeFromLocalIndexes(root);
		
		Transaction tx = graphDb.beginTx();
		try {
			HashSet<Node> todelete = new HashSet<Node>();
			TraversalDescription CHILDOF_TRAVERSAL = Traversal.description().relationships(RelType.CHILDOF, Direction.INCOMING);
			todelete.add(root);
			for (Node curGraphNode : CHILDOF_TRAVERSAL.breadthFirst().traverse(root).nodes()) {
				todelete.add(curGraphNode);
			}
			for (Node nd : todelete) {
				for (Relationship rel : nd.getRelationships()) {
					rel.delete();
				}
				nd.delete();
			}
			treeRootNodesByTreeId.remove(root);
			
			tx.success();
		} finally {
			tx.finish();
		}
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
			importedTreesFound = treeRootNodesByTreeId.query("localTreeId", "*");
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
		IndexHits<Node> hits = treeRootNodesByTreeId.query("localTreeId", "*");
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

	/**
	 * Remove a local source and all its trees.
	 * @param sourceId
	 * @throws NoSuchTreeException 
	 */
	public void deleteLocalSource(String sourceId) throws NoSuchTreeException {

		Node sourceMeta = getSingleNodeIndexHit(sourceMetaNodesBySourceId, "localSourceId", sourceId);
		if (sourceMeta == null) {
			throw new NoSuchElementException("Attempt to delete local study with id " + sourceId + ", but no such study exists.");
		}
		
		Transaction tx = graphDb.beginTx();
		try {
			
			// remove all trees
			for (Relationship rel : sourceMeta.getRelationships(RelType.METADATAFOR, Direction.OUTGOING)) {
				String treeID = (String) rel.getEndNode().getProperty(NodeProperty.TREE_ID.name());
				deleteLocalTree(treeID); // will also remove the METADATAFOR rels pointing at this metadata node
			}

			indexer.removeSourceMetaNodeFromLocalIndexes(sourceMeta);

			// delete remaining relationships
			for (Relationship rel : sourceMeta.getRelationships()) {
				rel.delete();
			}
			
			// delete the node itself
			sourceMeta.delete();			
			
			tx.success();
			
		} finally {
			tx.finish();
		}
	}

	/**
	 * Get the source id for the local tree corresponding to the specified tree id.
	 * @param treeID
	 * @return
	 */
	public String getSourceIdFromLocalTreeId(String treeId) {
		return getStudyIdFromTreeId(treeId, "localTreeId");
	}

	/**
	 * An abstracted method to all the same code to access local and remote studies. Requires that there is only
	 * a single entry in the index for the specified propertyName and treeId values.
	 * @param treeId
	 * @param propertyName
	 * @return
	 */
	private String getStudyIdFromTreeId(String treeId, String propertyName) {
		
		Node rootNode = getSingleNodeIndexHit(treeRootNodesByTreeId, propertyName, treeId);

		return (String) rootNode.getSingleRelationship(RelType.METADATAFOR, Direction.INCOMING)
				.getStartNode().getProperty(NodeProperty.SOURCE_ID.name());
	}
	
	/**
	 * get a list of otu nodes based on a study metadatanode
	 */
	public HashSet<Node> getOTUsFromMetadataNode(Node metadata){
		HashSet<Node> reths =  new HashSet<Node>();
		for (Relationship rel: metadata.getRelationships(Direction.OUTGOING, RelType.METADATAFOR)){
			Node treeroot = rel.getEndNode();
			reths.addAll(getOTUsFromTreeRootNode(treeroot));
		}
		return reths;
	}
	 
	/*
	 * get a list of otu nodes starting from a treeroot node
	 */
	public HashSet<Node> getOTUsFromTreeRootNode(Node treeroot){
		HashSet<Node> reths = new HashSet<Node>();
		TraversalDescription CHILDOF_TRAVERSAL = Traversal.description().relationships(RelType.CHILDOF, Direction.INCOMING);
		for(Node curGraphNode: CHILDOF_TRAVERSAL.breadthFirst().traverse(treeroot).nodes()){
			if(curGraphNode.hasProperty("oty")){
				reths.add(curGraphNode);
			}
		}
		return reths;
	}
}
