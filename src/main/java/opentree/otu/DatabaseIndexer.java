package opentree.otu;

import jade.tree.JadeNode;
import jade.tree.JadeTree;

import java.util.HashSet;
import java.util.List;

import opentree.otu.constants.GeneralConstants;
import opentree.otu.constants.NodeProperty;
import opentree.otu.constants.RelType;
import opentree.otu.constants.SourceProperty;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;

public class DatabaseIndexer extends DatabaseAbstractBase {

	private TraversalDescription CHILDOF_TRAVERSAL = Traversal.description().relationships(RelType.CHILDOF, Direction.INCOMING);

	public final Index<Node> treeRootNodesByTreeId = getNodeIndex(NodeIndexDescription.TREE_ROOT_NODES_BY_TREE_ID);
	public final Index<Node> treeRootNodesBySourceId = getNodeIndex(NodeIndexDescription.TREE_ROOT_NODES_BY_SOURCE_ID);
	public final Index<Node> treeRootNodesByOTProperty = getNodeIndex(NodeIndexDescription.TREE_ROOT_NODES_BY_OT_PROPERTY);

	public final Index<Node> treeRootNodesByTaxonName = getNodeIndex(NodeIndexDescription.TREE_ROOT_NODES_BY_INCLUDED_TAXON_NAME);
	public final Index<Node> treeRootNodesByTaxonNameNoSpaces = getNodeIndex(NodeIndexDescription.TREE_ROOT_NODES_BY_INCLUDED_TAXON_NAME_WHITESPACE_FILLED);
	public final Index<Node> treeRootNodesByMappedTaxonOTTId = getNodeIndex(NodeIndexDescription.TREE_ROOT_NODES_BY_INCLUDED_TAXON_MAPPED_OTT_ID);
	
	public final Index<Node> sourceMetaNodesBySourceId = getNodeIndex(NodeIndexDescription.SOURCE_METADATA_NODES_BY_SOURCE_ID);
	public final Index<Node> sourceMetaNodesByOTProperty = getNodeIndex(NodeIndexDescription.SOURCE_METADATA_NODES_BY_OT_PROPERTY);
	
	public DatabaseIndexer(GraphDatabaseAgent gdba) {
		super(gdba);
	}
	
	public DatabaseIndexer(GraphDatabaseService gdbs) {
		super(gdbs);
	}
	
	public void addStudyToIndexes(List<JadeTree> trees, String sourceId) {
		
		Transaction tx = graphDb.beginTx();
		try {

			Node sourceMetadataNode = null;

			// if there is a graph node for this source already do not make a new one.
			IndexHits<Node> hits = null;
			try {
				hits = sourceMetaNodesBySourceId.get("sourceID", sourceId);
				if (hits.size() < 0) {
					sourceMetadataNode = hits.getSingle(); // TODO: there should never be more than one, right?
				} else {
					sourceMetadataNode = graphDb.createNode();
				}
				
				// TODO: sometimes the nexson reader returns null trees. this is a hack to avoid that.
				// really we should fix the nexson reader so it doesn't return null trees
				boolean foundValidTree = false;
				for (int i = 0; i < trees.size(); i++) {
					if (trees.get(i) == null) {
						continue;
					} else {
						setStudyMetadataNodePropertiesAndIndex(sourceMetadataNode, trees.get(i), sourceId);
						foundValidTree = true;
						break;
					}
				}
				
				if (!foundValidTree) { // in case all the trees in the list were null, there is nothing to index.
					throw new NullPointerException("Nexson reader returned a list of null trees, so study import has been aborted. "
							+ "This should be fixed in nexson reader...");
				}
				
			} finally {
				hits.close();
			}


			for (int i = 0; i < trees.size(); i++) {
				
				JadeTree tree = trees.get(i);
				
				// sometimes the nexson reader returns null trees. should be fixed in nexson reader.
				if (tree == null) {
					continue;
				}

				String treeId = null;
				if (tree.getObject("id") != null) {
					treeId = (String) tree.getObject("id");
				} else {
					// changed this to make it clearer that we're setting this manually... noted in case it breaks something...
					treeId = sourceId + GeneralConstants.LOCAL_TREEID_PREFIX.value + String.valueOf(i);
				}
				
				// get the tree root from the graph if it's already in there.
				IndexHits<Node> treesFound = null;
				Node treeRootNode = null;
				try {
					treesFound = treeRootNodesByTreeId.get("treeID", treeId);
					if (treesFound.size() > 0) {
						treeRootNode = treesFound.getSingle(); // there better only be one...
					} else {
						// just add the root node, not the whole tree. (we also don't set ingroup when we're just indexing)
						treeRootNode = graphDb.createNode();
						sourceMetadataNode.createRelationshipTo(treeRootNode, RelType.METADATAFOR);
					}
				} finally {
					treesFound.close();
				}

				// install the tree properties and index entries
				setTreeRootNodePropertiesAndIndex(treeRootNode, tree, treeId, sourceId);
			}
			tx.success();
		} finally {
			tx.finish();
		}
	}
	
	// TODO: also need methods to remove tree root and source meta nodes from all indexes, which
	// we will need to do when updating from the repo after a tree or study has been erased
	
	/**
	 * Remove the indicated node from the local source metadata node indexes.
	 */
	public void removeSourceMetaNodeFromLocalIndexes(Node sourceMetaNode) {
		sourceMetaNodesBySourceId.remove(sourceMetaNode);
		// TODO: finish this
	}

	/**
	 * Remove the indicated node from the local tree root node indexes.
	 */
	public void removeTreeRootNodeFromLocalIndexes(Node treeRootNode) {
		treeRootNodesByTreeId.remove(treeRootNode);
		treeRootNodesBySourceId.remove(treeRootNode);
		// TODO: finish this
	}

	/**
	 * Index the indicated node as a local source metadata node.
	 * @param sourceMetaNode
	 */
	public void addSourceMetaNodeToLocalIndexes(Node sourceMetaNode) {
		sourceMetaNodesBySourceId.add(sourceMetaNode, "localSourceId", sourceMetaNode.getProperty(NodeProperty.SOURCE_ID.name()));
		// TODO: finish this
	}
	
	/**
	 * Index the indicated node as a local source tree root node.
	 * @param treeRootNode
	 */
	public void addTreeRootNodeToLocalIndexes(Node treeRootNode) {
		treeRootNodesByTreeId.add(treeRootNode, "localTreeId", treeRootNode.getProperty(NodeProperty.TREE_ID.name()));
		// TODO: finish this
	}
	
	public void setStudyMetadataNodePropertiesAndIndex(Node sourceMetadataNode, JadeTree tree, String sourceId) {
		
		sourceMetadataNode.setProperty("sourceID", sourceId);

		// set study metadata stored in the provided jadetree
		setNodePropertyFromJadeTreeIfExists(tree, sourceMetadataNode, SourceProperty.CURATOR_NAME.propertyName);
		setNodePropertyFromJadeTreeIfExists(tree, sourceMetadataNode, SourceProperty.DATA_DEPOSIT.propertyName);
		setNodePropertyFromJadeTreeIfExists(tree, sourceMetadataNode, SourceProperty.STUDY_PUBLICATION.propertyName);
		setNodePropertyFromJadeTreeIfExists(tree, sourceMetadataNode, SourceProperty.PUBLICATION_REFERENCE.propertyName);
		setNodePropertyFromJadeTreeIfExists(tree, sourceMetadataNode, SourceProperty.YEAR.propertyName);

		// add to indexes
		sourceMetaNodesBySourceId.add(sourceMetadataNode, "sourceID", sourceId);
		indexNodeByOTProperties(sourceMetadataNode, sourceMetaNodesByOTProperty);
	}
	
	/**
	 * Sets initial tree root node properties for the supplied `treeRootNode`, using values from the supplied JadeTree
	 * and the `treeId` and `sourceId`, and also adds the tree to all the basic indexes.
	 * @param treeRootNode
	 * @param tree
	 * @param treeId
	 * @param sourceId
	 */
	public void setTreeRootNodePropertiesAndIndex(Node treeRootNode, JadeTree tree, String treeId, String sourceId) {

		treeRootNode.setProperty("treeID", treeId);
		treeRootNode.setProperty("isroot", true);

		// set tree metadata from the attached jadetree
		setNodePropertyFromJadeTreeIfExists(tree, treeRootNode, "ot:branchLengthMode");
		setNodePropertyFromJadeTreeIfExists(tree, treeRootNode, "ingroup"); // should this be in the ot namespace?
		setNodePropertyFromJadeTreeIfExists(tree, treeRootNode, "ot:inGroupClade");
		setNodePropertyFromJadeTreeIfExists(tree, treeRootNode, "ot:focalClade");
		setNodePropertyFromJadeTreeIfExists(tree, treeRootNode, "ot:tag");

		// add to indexes
		treeRootNodesByTreeId.add(treeRootNode, "treeID", treeId);
		treeRootNodesBySourceId.add(treeRootNode, "sourceID", sourceId);
		indexNodeByJadeTreeTipTaxa(treeRootNode, tree);
		indexNodeByOTProperties(treeRootNode, treeRootNodesByOTProperty);
		
	}
	
	/**
	 * Updates relevant properties and index entries when a tree is rerooted. Assumes that the actual graph restructuring for the
	 * reroot has already been done. Must be called from within a transaction.
	 * @param oldRoot
	 * @param newRoot
	 */
	public void exchangeRootPropertiesAndUpdateIndexes(Node oldRoot, Node newRoot) {
		String treeID = (String) oldRoot.getProperty("treeID");

		// exchange relevant properties
		exchangeNodeProperty(oldRoot, newRoot, "isroot");
		exchangeNodeProperty(oldRoot, newRoot, "ot:branchLengthMode");
		exchangeNodeProperty(oldRoot, newRoot, "ingroup");
		exchangeNodeProperty(oldRoot, newRoot, "ot:inGroupClade");
		exchangeNodeProperty(oldRoot, newRoot, "ot:focalClade");
		exchangeNodeProperty(oldRoot, newRoot, "ot:tag");

		// this seems odd... not sure why we aren't just setting this manually
		oldRoot.setProperty("rooting_set", "true");
		exchangeNodeProperty(oldRoot, newRoot, "rooting_set");

		// get the source id for updating the indexes
		String sourceId = (String) newRoot.getSingleRelationship(RelType.METADATAFOR, Direction.INCOMING)
				.getStartNode().getProperty("sourceID");

		// remove the old root from the indexes
//		importedTreeRootNodesByTreeId.remove(oldRoot, "treeID", treeID);
		treeRootNodesByTreeId.remove(oldRoot, "localTreeId", treeID);
		treeRootNodesBySourceId.remove(oldRoot, "localSourceId", sourceId);

		// add the new one
//		importedTreeRootNodesByTreeId.add(newRoot, "treeID", treeID);
		treeRootNodesByTreeId.add(newRoot, "localTreeId", treeID);
		treeRootNodesBySourceId.add(newRoot, "localSourceId", sourceId);

	}
	
	/**
	 * Switches the value of the specified property between two nodes. If only one node has the property, it will be
	 * removed from that node and set on the other. If neither node has the property, there is no effect.
	 * @param n1
	 * @param n2
	 * @param propertyName
	 * @return
	 */
	public static void exchangeNodeProperty(Node n1, Node n2, String propertyName) {
		
		// TODO: as a static method, this should probably go in a utils class or something. but not necessary for now.

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
	
	/**
	 * Index a node into the taxonomic indexes for all the taxon names and ott ids found in the tips of the `tree`.
	 * Must be called from inside a transaction.
	 * @param node
	 * @param tree
	 */
	public void indexNodeByJadeTreeTipTaxa(Node node, JadeTree tree) {
		for (JadeNode treeNode : tree.getRoot().getDescendantLeaves()) {

			// by name
			String name = treeNode.getName();
			treeRootNodesByTaxonName.add(node, "name", name);
			treeRootNodesByTaxonNameNoSpaces.add(node, "name", name.
					replace("\\s+", (String)GeneralConstants.WHITESPACE_SUBSTITUTE_FOR_SEARCH.value));

			// by ottid
			Long ottId = (Long) treeNode.getObject("ot:ottolid");
			if (ottId != null) {
				treeRootNodesByMappedTaxonOTTId.add(node, "uid", ottId);
			}
		}
	}

	/**
	 * Index a node into the supplied index under all its ot:* namespace properties. Must be called from inside a transaction.
	 * @param node
	 * @param index
	 */
	public void indexNodeByOTProperties(Node node, Index<Node> index) {
		for (String propertyName : node.getPropertyKeys()) {
			if (propertyName.substring(0, 3).equals("ot:")) {
				index.add(node, propertyName, node.getProperty(propertyName));
			}
		}
	}
	
	private boolean setNodePropertyFromJadeTreeIfExists(JadeTree tree, Node node, String propertyName) {
		if (tree.hasAssocObject(propertyName)) {
			node.setProperty(propertyName, String.valueOf(tree.getObject(propertyName)));
			return true;
		} else {
			return false;
		}
	}
}
