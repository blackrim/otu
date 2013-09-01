package opentree.otu;

import jade.tree.JadeTree;

import java.util.List;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;

public class DatabaseIndexer extends DatabaseAbstractBase {

	Index<Node> treeRootNodesByTreeId = getNodeIndex(NodeIndexDescription.TREE_ROOT_NODES_BY_TREE_ID);
	Index<Node> treeRootNodesBySourceId = getNodeIndex(NodeIndexDescription.TREE_ROOT_NODES_BY_SOURCE_ID);
	Index<Node> treeRootNodesByTaxonName = getNodeIndex(NodeIndexDescription.TREE_ROOT_NODES_BY_INCLUDED_TAXON_NAME);
	Index<Node> treeRootNodesByTaxonNameUnderscores = getNodeIndex(NodeIndexDescription.TREE_ROOT_NODES_BY_INCLUDED_TAXON_NAME_UNDERSCORES);
	Index<Node> treeRootNodesByMappedTaxonOTTIds = getNodeIndex(NodeIndexDescription.TREE_ROOT_NODES_BY_INCLUDED_TAXON_MAPPED_OTT_ID);
	Index<Node> sourceMetaNodesByAuthor = getNodeIndex(NodeIndexDescription.SOURCE_METADATA_NODES_BY_AUTHOR);
	Index<Node> sourceMetaNodesByCurator = getNodeIndex(NodeIndexDescription.SOURCE_METADATA_NODES_BY_OT_CURATOR_NAME);
	Index<Node> sourceMetaNodesByOTDataDeposit = getNodeIndex(NodeIndexDescription.SOURCE_METADATA_NODES_BY_OT_DATA_DEPOSIT);
	Index<Node> sourceMetaNodesByOTPubRef = getNodeIndex(NodeIndexDescription.SOURCE_METADATA_NODES_BY_OT_PUBLICATION_REFERENCE);
	Index<Node> sourceMetaNodesByOTSourceId = getNodeIndex(NodeIndexDescription.SOURCE_METADATA_NODES_BY_OT_SOURCE_ID);
	Index<Node> sourceMetaNodesByOTStudyPub = getNodeIndex(NodeIndexDescription.SOURCE_METADATA_NODES_BY_OT_STUDY_PUBLICATION);
	Index<Node> sourceMetaNodesByOTStudyYear = getNodeIndex(NodeIndexDescription.SOURCE_METADATA_NODES_BY_OT_STUDY_YEAR);
	
	public DatabaseIndexer(GraphDatabaseAgent gdba) {
		super(gdba);
	}
	
	public DatabaseIndexer(GraphDatabaseService gdbs) {
		super(gdbs);
	}
	
	public void indexStudy(List<JadeTree> trees, String sourceID) {
		
		// populate the indexes. the source info is in the first tree
	}
}
