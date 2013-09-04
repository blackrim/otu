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

//	private TraversalDescription CHILDOF_TRAVERSAL = Traversal.description().relationships(RelType.CHILDOF, Direction.INCOMING);

	// tree root indexes
	public final Index<Node> treeRootNodesByTreeId = getNodeIndex(NodeIndexDescription.TREE_ROOT_NODES_BY_TREE_ID);
	public final Index<Node> treeRootNodesBySourceId = getNodeIndex(NodeIndexDescription.TREE_ROOT_NODES_BY_SOURCE_ID);
	public final Index<Node> treeRootNodesByOTProperty = getNodeIndex(NodeIndexDescription.TREE_ROOT_NODES_BY_OT_PROPERTY);
	
	public final Index<Node> treeRootNodesByOriginalTaxonName = getNodeIndex(NodeIndexDescription.TREE_ROOT_NODES_BY_ORIGINAL_TAXON_NAME);
	public final Index<Node> treeRootNodesByMappedTaxonName = getNodeIndex(NodeIndexDescription.TREE_ROOT_NODES_BY_MAPPED_TAXON_NAME);
	public final Index<Node> treeRootNodesByMappedTaxonNameNoSpaces = getNodeIndex(NodeIndexDescription.TREE_ROOT_NODES_BY_MAPPED_TAXON_NAME_WHITESPACE_FILLED);
	public final Index<Node> treeRootNodesByMappedTaxonOTTId = getNodeIndex(NodeIndexDescription.TREE_ROOT_NODES_BY_MAPPED_TAXON_OTT_ID);

	// source meta indexes
	public final Index<Node> sourceMetaNodesBySourceId = getNodeIndex(NodeIndexDescription.SOURCE_METADATA_NODES_BY_SOURCE_ID);
	public final Index<Node> sourceMetaNodesByOTProperty = getNodeIndex(NodeIndexDescription.SOURCE_METADATA_NODES_BY_OT_PROPERTY);
	
	// ===== constructors
	
	public DatabaseIndexer(GraphDatabaseAgent gdba) {
		super(gdba);
	}
	
	public DatabaseIndexer(GraphDatabaseService gdbs) {
		super(gdbs);
	}

	// ===== indexing source metadata nodes
	
/*	public void addStudyToIndexes(/*List<JadeTree> trees,* Node sourceMeta /*, String sourceId, String location*) {
		
		Transaction tx = graphDb.beginTx();
		try {

//			for (int i = 0; i < trees.size(); i++) {
				
//				JadeTree tree = trees.get(i);
			
			
				
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
//						sourceMetadataNode.createRelationshipTo(treeRootNode, RelType.METADATAFOR);
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
	} */

	/*
	 * Index the indicated node as a local source metadata node. Uses properties stored in the node when adding, so requires that
	 * this study has already been added to the graph.
	 * @param sourceMetaNode
	 *
	public void addSourceMetaNodeToLocalIndexes(Node sourceMetaNode) {
		addSourceMetaNodeToIndexes(sourceMetaNode, "localSourceId");
	}
	
	/*
	 * Index the indicated node as a remote source tree root node. This could be modified to support multiple
	 * remotes if that is a direction we take. Uses properties stored in the node when adding, so requires that
	 * this study has already been added to the graph.
	 * 
	 * @param treeRootNode
	 *
	public void addSourceMetaNodeToRemoteIndexes(Node sourceMetaNode) {
		
		String location = ""; // allow this to be set to support multiple remotes.
		// this would also require other changes elsewhere to keep track of remotes

		String propertyNameForIdIndexes = "remote" + location + "TreeId";
		addSourceMetaNodeToIndexes(sourceMetaNode, propertyNameForIdIndexes);
	} */

	/**
	 * Generalized method for adding source metadata nodes to indexes. This method uses properties stored in
	 * the graph during study import, and thus should be called *after* a study has been added to the graph.
	 * 
	 * requires the study to 
	 * @param sourceMetaNode
	 * @param property
	 */
	public void addSourceMetaNodeToIndexes(Node sourceMetaNode) {
		sourceMetaNodesBySourceId.add(sourceMetaNode,
				(String) sourceMetaNode.getProperty(NodeProperty.LOCATION.name()),
				sourceMetaNode.getProperty(NodeProperty.SOURCE_ID.name()));
		indexNodeByOTProperties(sourceMetaNode, sourceMetaNodesByOTProperty);
	}

	/**
	 * Remove the indicated node from all source metadata node indexes.
	 */
	public void removeSourceMetaNodeFromAllIndexes(Node sourceMetaNode) {
		sourceMetaNodesBySourceId.remove(sourceMetaNode);
		sourceMetaNodesByOTProperty.remove(sourceMetaNode);
	}
		
	// ===== indexing tree root nodes
	
	/*
	 * Index the indicated node as a local source tree root node. Uses graph traversals and node properties to
	 * install the node in the indexes, so requires that this study has already been set up in the graph.
	 * 
	 * @param treeRootNode
	 *
	public void addTreeRootNodeToLocalIndexes(Node treeRootNode) {
		addTreeRootNodeToIndexes(treeRootNode, "localTreeId");
	}

	/*
	 * Index the indicated node as a remote source tree root node. This could be modified to support multiple
	 * remotes if that is a direction we take. Uses graph traversals and node properties to install the node in the
	 * indexes, so requires that this node has already been set up in the graph.
	 * 
	 * @param treeRootNode
	 *
	public void addTreeRootNodeToRemoteIndexes(Node treeRootNode) {
		
		String location = ""; // allow this to be set to support multiple remotes.
		// this would also require other changes elsewhere to keep track of remotes

		String propertyNameForIdIndexes = "remote" + location + "TreeId";
		addTreeRootNodeToIndexes(treeRootNode, propertyNameForIdIndexes);
	} */
	
	/**
	 * Install the indicated tree root node into the indexes. Uses graph traversals and node properties set during study
	 * import, and thus should be called *after* the study has been added to the graph.
	 * 
	 * @param treeRootNode
	 */
	public void addTreeRootNodeToIndexes(Node treeRootNode) {

		treeRootNodesByTreeId.add(treeRootNode,
				(String) treeRootNode.getProperty(NodeProperty.LOCATION.name()),
				treeRootNode.getProperty(NodeProperty.TREE_ID.name()));
		
		treeRootNodesBySourceId.add(treeRootNode,
				(String) treeRootNode.getProperty(NodeProperty.LOCATION.name()),
				treeRootNode.getSingleRelationship(RelType.METADATAFOR, Direction.INCOMING)
					.getEndNode().getProperty(NodeProperty.SOURCE_ID.name()));
		
		// add to ot property indexes
		indexNodeByOTProperties(treeRootNode, treeRootNodesByOTProperty);

		// add to taxonomy indexes
		addTreeToTaxonomicIndexes(treeRootNode);
	}
	
	/**
	 * Remove the indicated node from the tree root node indexes.
	 *
	 * @param treeRootNode
	 */
	public void removeTreeRootNodeFromIndexes(Node treeRootNode) {
		treeRootNodesByTreeId.remove(treeRootNode);
		treeRootNodesBySourceId.remove(treeRootNode);
		treeRootNodesByOTProperty.remove(treeRootNode);
		treeRootNodesByMappedTaxonName.remove(treeRootNode);
		treeRootNodesByMappedTaxonNameNoSpaces.remove(treeRootNode);
		treeRootNodesByMappedTaxonOTTId.remove(treeRootNode);
	}
	
	// === private methods used during tree root indexing
	
	/**
	 * Add the tree to the taxonomic indexes
	 * @param treeRootNode
	 */
	private void addTreeToTaxonomicIndexes(Node root) {
		addArrayEntriesToIndex(root, treeRootNodesByOriginalTaxonName, NodeProperty.ORIGINAL_TAXON_NAMES, "name");
		addArrayEntriesToIndex(root, treeRootNodesByMappedTaxonName, NodeProperty.MAPPED_TAXON_NAMES, "name");
		addArrayEntriesToIndex(root, treeRootNodesByMappedTaxonNameNoSpaces, NodeProperty.MAPPED_TAXON_NAMES_WHITESPACE_FILLED, "name");
		addArrayEntriesToIndex(root, treeRootNodesByMappedTaxonOTTId, NodeProperty.MAPPED_TAXON_OTT_IDS, "uid");
	}
	
	// ===== generalized private methods used during indexing

	/**
	 * Index a node into the supplied index under all its ot:* namespace properties.
	 * @param node
	 * @param index
	 */
	private void indexNodeByOTProperties(Node node, Index<Node> index) {
		
		// TODO: modify this to use an enum/accept an array (that can be populated from an enum) that specifies all the properties to be set.
		
		for (String propertyName : node.getPropertyKeys()) {
			if (propertyName.substring(0, 3).equals("ot:")) {
				index.add(node, propertyName, node.getProperty(propertyName));
			}
		}
	}
	
	/**
	 * A generic method for adding a node to an index under a specified property using every element of an array
	 * as a key. Currently the array must already be stored as a property of the node, but this could be extended to allow
	 * arbitrary arrays. Used to add root nodes to the indexes for taxon names and ott ids.
	 * 
	 * @param node
	 * @param dataType
	 * @param nodeProperty
	 * @param indexProperty
	 */
	private void addArrayEntriesToIndex(Node node, Index<Node> index, NodeProperty nodeProperty, String indexProperty) {
		if (node.hasProperty(nodeProperty.name())) {
			Object[] array = (Object[]) node.getProperty(nodeProperty.name());
			for (int i = 0; i < array.length; i++) {
				index.add(node, indexProperty, (nodeProperty.type.getComponentType()).cast(array[i]));
			}
		}
	}
}
