package opentree.otu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import jade.tree.JadeNode;
import jade.tree.JadeTree;
import opentree.otu.GeneralUtils;
import opentree.otu.constants.GeneralConstants;
import opentree.otu.constants.NodeProperty;
import opentree.otu.constants.RelType;
import opentree.otu.exceptions.NoSuchTreeException;

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

	private DatabaseIndexer indexer;
	
	protected Index<Node> sourceMetaIndex = getNodeIndex(NodeIndexDescription.SOURCE_METADATA_NODES_BY_OT_SOURCE_ID);
	protected Index<Node> allTreeRootIndex = getNodeIndex(NodeIndexDescription.TREE_ROOT_NODES_BY_TREE_ID);
//	protected Index<Node> importedTreeRootIndex = getNodeIndex(NodeIndexDescription.LOCAL_TREE_ROOT_NODES_BY_TREE_ID);
	protected Index<Node> sourceTreeIndex = getNodeIndex(NodeIndexDescription.TREE_ROOT_NODES_BY_OT_SOURCE_ID);

	// ===== constructors

	/**
	 * Access the graph db through the given service object.
	 * 
	 * @param graphService
	 */
	public DatabaseManager(GraphDatabaseService graphService) {
		super(graphService);
		indexer = new DatabaseIndexer(graphDb);
//		initNodeIndexes();
	}

	/**
	 * Access the graph db through the given embedded db object.
	 * 
	 * @param embeddedGraph
	 */
	public DatabaseManager(EmbeddedGraphDatabase embeddedGraph) {
		super(embeddedGraph);
		indexer = new DatabaseIndexer(graphDb);
//		initNodeIndexes();
	}

	/**
	 * Open the graph db through the given agent object.
	 * 
	 * @param gdb
	 */
	public DatabaseManager(GraphDatabaseAgent gdb) {
		super(gdb);
		indexer = new DatabaseIndexer(graphDb);
//		initNodeIndexes();
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
			
			// check if the study already exists
			Node sourceMeta = null;
			IndexHits<Node> localStudiesFound = null;
			try {
				localStudiesFound = sourceMetaIndex.get("localSourceId", sourceID);
				if (localStudiesFound.size() == 1) {
					// already exists, so use the existing one
					sourceMeta = localStudiesFound.getSingle();
				
				} else if (localStudiesFound.size() > 1) {
					throw new IllegalStateException("More than one local source with the id: " + sourceID + ". The database is probably corrupt.");

				} else {
					// no existing local study, so install it. use study properties stored in the first tree
					// TODO: THIS WILL FAIL IF NEXSON READER RETURNS A NULL TREE. FIX NEXSON READER.
					sourceMeta = graphDb.createNode();
					sourceMeta.setProperty(NodeProperty.LOCATION.name(), "local");
					indexer.setStudyMetadataNodePropertiesAndIndex(sourceMeta, trees.get(0), sourceID);
				}
			} finally {
				localStudiesFound.close();
			}

			// attach local study meta node to corresponding remote study if it exists (and is not already attached)
			Node remoteSourceMeta = null;
			IndexHits<Node> remoteStudiesFound = null;
			try {
				remoteStudiesFound = sourceMetaIndex.get("remoteSourceId", sourceID);
				if (remoteStudiesFound.size() == 1) {
					// remote study known, so use it
					remoteSourceMeta = remoteStudiesFound.getSingle();
					sourceMeta.createRelationshipTo(remoteSourceMeta, RelType.ISLOCALCOPYOF);
					
				} else if (remoteStudiesFound.size() > 1) {
					throw new IllegalStateException("More than one local source with the id: " + sourceID + ". The database is probably corrupt.");

				} else { // no remote studies found, nothing to do here.
					;
				}
			} finally {
				remoteStudiesFound.close();
			}

			
			HashSet<String> existingTreeIds = new HashSet<String>();
			for (Relationship rel : sourceMeta.getRelationships(Direction.OUTGOING, RelType.METADATAFOR)) {
				existingTreeIds.add((String) rel.getEndNode().getProperty(NodeProperty.TREE_ID.name()));
			}
			
			// for each tree
			for (int i = 0; i < trees.size(); i++) {

				// get the tree id if there is one or create an arbitrary one if not
				String tid = null;
				if (trees.get(i).getObject("id") != null) {
					tid = (String) trees.get(i).getObject("id");
				} else {
					tid = sourceID + GeneralConstants.LOCAL_TREEID_PREFIX.value + String.valueOf(i);
				}

				// only add tree if it isn't there yet... should we support this or do we want to throw an exception when the tree already exists?
				if (!existingTreeIds.contains(tid)) {
					addTreeToDB(trees.get(i), tid, sourceMeta);
				}
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
	 * @param intree
	 * @return
	 */
	private boolean addTreeToDB(JadeTree intree, String tree_id, Node metadatanode) {

		// add the tree to the graph
		Node root = preorderAddTreeToDB(intree.getRoot(), null, tree_id);
		metadatanode.createRelationshipTo(root, RelType.METADATAFOR);

		// designate ingroup if known
		if (intree.getRoot().getObject("ingroup_start") != null) {
			designateIngroup(root);
		}

		// add node properties and add to indexes
		indexer.setTreeRootNodePropertiesAndIndex(root, intree, tree_id, (String) metadatanode.getProperty(NodeProperty.SOURCE_ID.name()));
		
		// also add to the imported tree index
		allTreeRootIndex.add(root, "localTreeId", tree_id);

		return true; // this doesn't mean anything... we never return false.
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
			processReroot(actualRoot);

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

	/**
	 * Get a tree root node from the graph. Will do error checking to make sure tree is available and unique, and will throw an exception if not.
	 * @param treeID
	 * @return
	 * @throws NoSuchTreeException
	 */
	public Node getRootNodeFromTreeIDValidated(String treeID) throws NoSuchTreeException {
		IndexHits<Node> importedTreesFound = allTreeRootIndex.get("localTreeId", treeID);
		if (importedTreesFound.size() < 1) {
			throw new NoSuchTreeException("The tree " + treeID + " has not been imported into the database.");
		} else if (importedTreesFound.size() > 1) {
			throw new IllegalStateException("More than one tree was found with the id " + treeID + ". Not a good sign.");
		}

		Node rootNode = importedTreesFound.getSingle();
		importedTreesFound.close();
		return rootNode;
	}

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
	 * 
	 * @param studyID
	 * @return
	 */
	public String getStudyMetaData(String studyID) { // TODO: getLocalStudyMetadata
		IndexHits<Node> sourcesFound = sourceMetaIndex.get("localSourceID", studyID);
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
	 * 
	 * @param studyID
	 * @param treeID
	 * @return
	 */
	public String getTreeMetaData(String studyID, String treeID) {
		
		// TODO: might need to update this to make sure it is only getting imported trees?
//		IndexHits<Node> treesFound = importedTreeRootIndex.get("treeID", treeID);
//		IndexHits<Node> sourcesFound = importedSourceMetaIndex.get("sourceID", studyID);

		IndexHits<Node> treesFound = allTreeRootIndex.get("localTreeId", treeID);
//		IndexHits<Node> sourcesFound = sourceMetaIndex.get("sourceID", studyID); // never used
		
//		Node sourcemeta = sourcesFound.getSingle(); // never used
		Node root = null;
		while (treesFound.hasNext()) {
			Node tnode = treesFound.next();
			if (((String) tnode.getProperty(NodeProperty.TREE_ID.name())).equals(treeID)) {
				root = tnode;
				break;
			}
		}
//		sourcesFound.close(); // never used
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

	/**
	 * Deletes a local tree
	 * @param treeID
	 * @throws NoSuchTreeException 
	 */
	public void deleteLocalTreeFromTreeID(String treeID) throws NoSuchTreeException {
		IndexHits<Node> treesFound = null;
		Node root = null;
		try {
			treesFound = allTreeRootIndex.get("localTreeId", treeID);
			if (treesFound.size() == 1) {
				root = treesFound.getSingle();
			} else if (treesFound.size() > 1) {
				throw new IllegalStateException("More than one tree found with id " + treeID + ". The database is probably corrupt.");
			} else {
				throw new NoSuchTreeException("Attempt to delete tree with id " + treeID + ", but there is no tree with this id");
			}
		} finally {
			treesFound.close();
		}

/*		try {
			while (treesFound.hasNext()) {
				Node tnode = treesFound.next();
				if (((String) tnode.getProperty(NodeProperty.TREE_ID.name())).equals(treeID)) {
					root = tnode;
					break;
				}
			}
		} finally {
			treesFound.close();
		} */

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
			allTreeRootIndex.remove(root);
			
			tx.success();
		} finally {
			tx.finish();
		}
	}

	/**
	 * Returns a JSON array string of all indexed studies.
	 * 
	 * Not used?
	 * 
	 * @return
	 */
	public String getJSONOfSourceIdsForAllTrees() {

		StringBuffer retstr = new StringBuffer("{ \"studies\" : [");
		IndexHits<Node> hits = sourceMetaIndex.query("*:*");
		while (hits.hasNext()) {
			Node x = hits.next();
			retstr.append("\"" + (String) x.getProperty(NodeProperty.SOURCE_ID.name()) + "\" ");
			if (hits.hasNext()) {
				retstr.append(",");
			}
		}
		hits.close();
		retstr.append("] }");
		return retstr.toString();
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
			importedTreesFound = allTreeRootIndex.query("localTreeId", "*");
			for (Node t : importedTreesFound) {
				studyIds.add((String) t.getSingleRelationship(RelType.METADATAFOR, Direction.INCOMING)
						.getStartNode().getProperty(NodeProperty.SOURCE_ID.name()));
			}
		} finally {
			importedTreesFound.close();
		}
		
		// write the string
		StringBuffer retstr = new StringBuffer("{ \"studies\" : [");
		Iterator<String> studyIdsIter = studyIds.iterator();
		while (studyIdsIter.hasNext()) {
			retstr.append("\"" + studyIdsIter.next() + "\" ");
			if (studyIdsIter.hasNext()) {
				retstr.append(",");
			}
		}
		retstr.append("] }");
		
		return retstr.toString();
	}

	/**
	 * 
	 * @return
	 */
	public String getJSONOfSourceIdsAndTreeIdsForImportedTrees() {

		// TODO: should this be returning all tree ids? Should it only be returning the sources that have imported trees?

		StringBuffer retstr = new StringBuffer("{ \"studies\" : [");
		IndexHits<Node> hits = allTreeRootIndex.query("localTreeId", "*");
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
	 * Remove a local study and all its trees.
	 * @param studyID
	 * @throws NoSuchTreeException 
	 */
	public void deleteStudyFromStudyID(String studyID) throws NoSuchTreeException { // TODO: deleteLocalSource(String studyId)

		// get the study
		Node sourceMeta = null;
		IndexHits<Node> studiesFound = null;
		try {
			studiesFound = sourceMetaIndex.get("localSourceId", studyID);
			sourceMeta = studiesFound.getSingle();
		} finally {
			studiesFound.close();
		}
		
		Transaction tx = graphDb.beginTx();
		try {
			// remove all trees
			for (Relationship rel : sourceMeta.getRelationships(RelType.METADATAFOR, Direction.OUTGOING)) {
				String treeID = (String) rel.getEndNode().getProperty(NodeProperty.TREE_ID.name());
				deleteLocalTreeFromTreeID(treeID);
			}
			
			// delete the source metadata node itself
			// TODO: once db structure has changed, will need to remove the rel pointing to the remote source node before we delete the local one
			sourceMeta.delete();			
			tx.success();
			
		} finally {
			tx.finish();
		}
	}

	/**
	 * 
	 * @param treeID
	 * @return
	 */
	public String getStudyIDFromTreeID(String treeID) {
		// TODO: update with new db structure
		IndexHits<Node> hits = allTreeRootIndex.get("treeID", treeID);
		Node rootNode = hits.getSingle();
		hits.close();
		String studyID = (String) rootNode.getSingleRelationship(RelType.METADATAFOR, Direction.INCOMING)
				.getStartNode().getProperty(NodeProperty.SOURCE_ID.name());
		return studyID;
	}
	
	/*
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
