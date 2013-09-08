package opentree.otu;

import opentree.otu.constants.OTUConstants;
import opentree.otu.constants.NodeProperty;
import opentree.otu.constants.RelType;
import opentree.otu.constants.SearchableProperty;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;

public class DatabaseIndexer extends DatabaseAbstractBase {

	// tree root indexes
	public final Index<Node> treeRootNodesByTreeId = getNodeIndex(NodeIndexDescription.TREE_ROOT_NODES_BY_TREE_ID);
	public final Index<Node> treeRootNodesBySourceId = getNodeIndex(NodeIndexDescription.TREE_ROOT_NODES_BY_SOURCE_ID);
	public final Index<Node> treeRootNodesByOtherProperty = getNodeIndex(NodeIndexDescription.TREE_ROOT_NODES_BY_OTHER_PROPERTY);
	
	public final Index<Node> treeRootNodesByOriginalTaxonName = getNodeIndex(NodeIndexDescription.TREE_ROOT_NODES_BY_ORIGINAL_TAXON_NAME);
	public final Index<Node> treeRootNodesByMappedTaxonName = getNodeIndex(NodeIndexDescription.TREE_ROOT_NODES_BY_MAPPED_TAXON_NAME);
	public final Index<Node> treeRootNodesByMappedTaxonNameNoSpaces = getNodeIndex(NodeIndexDescription.TREE_ROOT_NODES_BY_MAPPED_TAXON_NAME_WHITESPACE_FILLED);
	public final Index<Node> treeRootNodesByMappedTaxonOTTId = getNodeIndex(NodeIndexDescription.TREE_ROOT_NODES_BY_MAPPED_TAXON_OTT_ID);

	// source meta indexes
	public final Index<Node> sourceMetaNodesBySourceId = getNodeIndex(NodeIndexDescription.SOURCE_METADATA_NODES_BY_SOURCE_ID);
	public final Index<Node> sourceMetaNodesByOtherProperty = getNodeIndex(NodeIndexDescription.SOURCE_METADATA_NODES_BY_OTHER_PROPERTY);
	
	// ===== constructors
	
	public DatabaseIndexer(GraphDatabaseAgent gdba) {
		super(gdba);
	}
	
	public DatabaseIndexer(GraphDatabaseService gdbs) {
		super(gdbs);
	}

	// ===== indexing source metadata nodes
	
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
				(String) sourceMetaNode.getProperty(NodeProperty.LOCATION.name)+OTUConstants.SOURCE_ID,
				sourceMetaNode.getProperty(NodeProperty.SOURCE_ID.name));
		indexNodeBySearchableProperties(sourceMetaNode, OTUConstants.SOURCE_PROPERTIES_FOR_SIMPLE_INDEXING);
	}

	/**
	 * Remove the indicated node from all source metadata node indexes.
	 */
	public void removeSourceMetaNodeFromIndexes(Node sourceMetaNode) {
		sourceMetaNodesBySourceId.remove(sourceMetaNode);
		sourceMetaNodesByOtherProperty.remove(sourceMetaNode);
	}
		
	// ===== indexing tree root nodes

	/**
	 * Install the indicated tree root node into the indexes. Uses graph traversals and node properties set during study
	 * import, and thus should be called *after* the study has been added to the graph.
	 * 
	 * @param treeRootNode
	 */
	public void addTreeRootNodeToIndexes(Node treeRootNode) {

		treeRootNodesByTreeId.add(treeRootNode,
				(String) treeRootNode.getProperty(NodeProperty.LOCATION.name) + OTUConstants.TREE_ID,
				treeRootNode.getProperty(NodeProperty.TREE_ID.name));
		
		treeRootNodesBySourceId.add(treeRootNode,
				(String) treeRootNode.getProperty(NodeProperty.LOCATION.name) + OTUConstants.SOURCE_ID,
				treeRootNode.getSingleRelationship(RelType.METADATAFOR, Direction.INCOMING)
					.getEndNode().getProperty(NodeProperty.SOURCE_ID.name));
		
		// add to property indexes
		indexNodeBySearchableProperties(treeRootNode, OTUConstants.TREE_PROPERTIES_FOR_SIMPLE_INDEXING);

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
		treeRootNodesByOtherProperty.remove(treeRootNode);
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
		
		addStringArrayEntriesToIndex(root,
				treeRootNodesByOriginalTaxonName,
				NodeProperty.DESCENDANT_ORIGINAL_TAXON_NAMES.name,
				NodeProperty.ORIGINAL_LABEL.name);

		addStringArrayEntriesToIndex(root,
				treeRootNodesByMappedTaxonName,
				NodeProperty.DESCENDANT_MAPPED_TAXON_NAMES.name,
				NodeProperty.NAME.name);
		
		addStringArrayEntriesToIndex(root,
				treeRootNodesByMappedTaxonNameNoSpaces,
				NodeProperty.DESCENDANT_MAPPED_TAXON_NAMES_WHITESPACE_FILLED.name,
				NodeProperty.NAME.name);
		
		addLongArrayEntriesToIndex(root,
				treeRootNodesByMappedTaxonOTTId,
				NodeProperty.DESCENDANT_MAPPED_TAXON_OTT_IDS.name,
				NodeProperty.OT_OTT_ID.name);
	}
	
	// ===== generalized private methods used during indexing

	/**
	 * Index a node into the supplied index under all the specified properties.
	 * @param node
	 * @param index
	 */
	private void indexNodeBySearchableProperties(Node node, SearchableProperty[] searchablePoperties) {
		for (SearchableProperty search : searchablePoperties) {
			Index<Node> index = getNodeIndex(search.index);
			if (node.hasProperty(search.property.name)) {
				index.add(node, search.property.name, node.getProperty(search.property.name));
			}
		}
	}
	
	private void addStringArrayEntriesToIndex(Node node, Index<Node> index, String nodePropertyName, String indexProperty) {
		if (node.hasProperty(nodePropertyName)) {
			String[] array = (String[]) node.getProperty(nodePropertyName);
			for (int i = 0; i < array.length; i++) {
				index.add(node, indexProperty, array[i]);
			}
		}
	}

	private void addLongArrayEntriesToIndex(Node node, Index<Node> index, String nodePropertyName, String indexProperty) {
		if (node.hasProperty(nodePropertyName)) {
			long[] array = (long[]) node.getProperty(nodePropertyName);
			for (int i = 0; i < array.length; i++) {
				index.add(node, indexProperty, array[i]);
			}
		}
	}
}
