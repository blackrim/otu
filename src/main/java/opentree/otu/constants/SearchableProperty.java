package opentree.otu.constants;

import opentree.otu.NodeIndexDescription;

/**
 * An enum containing mappings identifying node properties and the indexes for which they are searchable.
 */
public enum SearchableProperty {

	// ===== source meta nodes
	SOURCE_ID ("source id", NodeProperty.SOURCE_ID, NodeIndexDescription.SOURCE_METADATA_NODES_BY_SOURCE_ID),
	CURATOR_NAME("ot curator name", NodeProperty.OT_CURATOR_NAME, NodeIndexDescription.SOURCE_METADATA_NODES_BY_OT_PROPERTY),
    DATA_DEPOSIT("ot data deposit", NodeProperty.OT_DATA_DEPOSIT, NodeIndexDescription.SOURCE_METADATA_NODES_BY_OT_PROPERTY),
    PUBLICATION_REFERENCE ("text citation (ot pub ref)", NodeProperty.OT_PUBLICATION_REFERENCE, NodeIndexDescription.SOURCE_METADATA_NODES_BY_OT_PROPERTY),
    STUDY_PUBLICATION("ot study pub", NodeProperty.OT_STUDY_PUBLICATION, NodeIndexDescription.SOURCE_METADATA_NODES_BY_OT_PROPERTY),
    YEAR ("ot year", NodeProperty.OT_YEAR, NodeIndexDescription.SOURCE_METADATA_NODES_BY_OT_PROPERTY),

    // ===== tree root nodes
    DESCENDANT_ORIGINAL_TAXON_NAMES("original taxon name", NodeProperty.ORIGINAL_NAME, NodeIndexDescription.TREE_ROOT_NODES_BY_ORIGINAL_TAXON_NAME),
    DESCENDANT_MAPPED_TAXON_NAMES("current taxon name (mapped?)", NodeProperty.NAME, NodeIndexDescription.TREE_ROOT_NODES_BY_MAPPED_TAXON_NAME),
//    DESCENDANT_MAPPED_TAXON_NAMES_WHITESPACE_FILLED(NodeProperty.DESCENDANT_MAPPED_TAXON_NAMES_WHITESPACE_FILLED, NodeIndexDescription.TREE_ROOT_NODES_BY_MAPPED_TAXON_NAME_WHITESPACE_FILLED),
    DESCENDANT_MAPPED_TAXON_OTT_IDS("ott id", NodeProperty.OTT_ID, NodeIndexDescription.TREE_ROOT_NODES_BY_MAPPED_TAXON_OTT_ID);
    
	public final String shortName;
    public final NodeProperty property;
    public final NodeIndexDescription index;
    
    SearchableProperty(String shortName, NodeProperty property, NodeIndexDescription indexDesc) {
    	this.shortName = shortName;
        this.property = property;
        this.index = indexDesc;
    }
}
