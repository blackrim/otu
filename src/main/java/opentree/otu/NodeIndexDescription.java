package opentree.otu;

/**
 * An enum to make keeping track of indexes easier. Please document new indexes here.
 * 
 * @author federation of botanist hackers
 *
 */
public enum NodeIndexDescription {

	// ===== tree indexes
	
	/**
     * Root nodes for imported AND REMOTE (i.e. not imported) trees, indexed by the tree ids. This is a one-one index, each tree has a unique tree id.
     * field is "treeID", key is tree id.
	 */
    TREE_ROOT_NODES_BY_TREE_ID ("treeRootNodesByTreeId"),
        
    /**
     * Root nodes for trees from the specified source. Field is "sourceID", key is source id (aka study id).
     */
    TREE_ROOT_NODES_BY_SOURCE_ID ("treeRootNodesBySourceId"),

    /**
     * Root nodes for trees including a taxon with the supplied name. Field is "name", key is taxon name.
     */
    TREE_ROOT_NODES_BY_INCLUDED_TAXON_NAME ("treeRootNodesByInclTaxonName"),

    /**
     * Root nodes for trees including a taxon with the supplied name. Spaces have been replaced with underscores
     * to facilitate whole-word matching. Field is "name", key is taxon name.
     */
    TREE_ROOT_NODES_BY_INCLUDED_TAXON_NAME_UNDERSCORES ("treeRootNodesByInclTaxonNameUNDERSCORES"),
    
    /**
     * Root nodes for trees including a taxon with the supplied ott id. Field is "uid", key is ott id.
     */
    TREE_ROOT_NODES_BY_INCLUDED_TAXON_MAPPED_OTT_ID ("treeRootNodesByInclTaxonMappedOTTId"),
    
    // ===== source indexes
    
    // ===== indexes by ot: namespace properties
    
    /**
     * Study metadata nodes indexed by the ot:curatorName property. Field is "name" (string) and key is curator name.
     */
	SOURCE_METADATA_NODES_BY_OT_CURATOR_NAME ("sourceMetaNodesByOTCurator"),
	
	/**
     * Study metadata nodes indexed by the ot:dataDeposit property. Perhaps useful for, e.g. finding treebase studies? Or not.
     * Field is "dataDeposit" (string) and key is data deposit value (e.g. a URI to the location of the data).
	 */
    SOURCE_METADATA_NODES_BY_OT_DATA_DEPOSIT ("sourceMetaNodesByOTDataDep"),
    
    /**
     * Study metadata nodes indexed by the ot:studyPublicationReference property. Probably mainly useful for fuzzy matching and general
     * search. Field is "reference" (string) and key is publication reference value (e.g. a citation).
     */
    SOURCE_METADATA_NODES_BY_OT_PUBLICATION_REFERENCE ("sourceMetaNodesByOTPubRef"),

    /**
     * Study metadata nodes for imported AND REMOTE (i.e. not imported) studies, indexed by the ot:studyId property. Field is "sourceID" (string) and key is source id (aka study id).
     */
    SOURCE_METADATA_NODES_BY_OT_SOURCE_ID ("sourceMetaNodesByOTSourceId"),

    /**
     * Study metadata nodes indexed by the ot:studyPublication property. Field is "pub" (string) and key is
     * study publication value (e.g. a DOI pointing to the study).
     */
    SOURCE_METADATA_NODES_BY_OT_STUDY_PUBLICATION ("sourceMetaNodesByOTStudyPub"),

    /**
     * Study metadata nodes indexed by the ot:studyYear property. Field is "year" (int) and key is study year.
     */
    SOURCE_METADATA_NODES_BY_OT_STUDY_YEAR ("sourceMetaNodesByOTStudyYear"),
    
    // ===== indexes for locally imported sources
    
	/**
     * Root nodes for IMPORTED TREES, indexed by the tree ids. This is a one-one index, each tree has a unique tree id.
     * field is "treeID", key is tree id.
	 */
    LOCAL_TREE_ROOT_NODES_BY_TREE_ID ("treeRootNodesByTreeIdIMPORTED"),
    
//    /**
//     * Study metadata nodes FOR IMPORTED STUDIES, indexed by the ot:studyId property. Field is "sourceID" (string) and key is source id (aka study id).
//     */
//    LOCAL_SOURCE_METADATA_NODES_BY_OT_SOURCE_ID ("sourceMetaNodesByOTSourceIdIMPORTED"),

    // ===== other indexes
    
    /**
     * Study metadata nodes indexed by their author. Apparently this field doesn't exist... (yet?).
     * Field is "name" (string) and key is curator name.
     */
    SOURCE_METADATA_NODES_BY_AUTHOR ("sourceMetaNodesBySourceAuthorName");

    
    String name;
    
    NodeIndexDescription(String name) {
    	this.name = name;
    }
}
