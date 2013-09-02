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
     * Root nodes for both local and remote (i.e. not imported) trees, indexed by the tree ids. The first key is either:
     * "localTreeId" or "remoteTreeId", and second key is the tree id string, which is the study id for the tree concatenated
     * via an underscore to either the incoming treeId from the original nexson, or an arbitrary local tree identifier string
     * if the incoming tree is missing (or if the tree is not from a nexson). Root nodes for trees that have been imported are
     * indexed under "localTreeId" and trees read from the remote repo are indexed under "remoteTreeId". To get a list of all
     * known trees, search this index on the key "*" (returns independent results for local and remote trees).
	 */
    TREE_ROOT_NODES_BY_TREE_ID ("treeRootNodesByTreeId"),
        
    /**
     * Root nodes for both local and remote (i.e. not imported) trees, indexed by their originating source id. Key 1 is either
     * "localSourceId" or "remoteSourceId", and key 2 is the source id (aka study id--a string).
     */
    TREE_ROOT_NODES_BY_OT_SOURCE_ID ("treeRootNodesBySourceId"),

    /**
     * Root nodes for trees including a taxon with the supplied name. Key 1 is "name", key 2 is taxon name.
     */
    TREE_ROOT_NODES_BY_INCLUDED_TAXON_NAME ("treeRootNodesByInclTaxonName"),

    /**
     * Root nodes for trees including a taxon with the supplied name. Spaces have been replaced with underscores
     * to facilitate whole-word matching. Key 1 is "name", key 2 is taxon name.
     */
    TREE_ROOT_NODES_BY_INCLUDED_TAXON_NAME_WHITESPACE_FILLED ("treeRootNodesByInclTaxonNameWhitespaceFilled"),
    
    /**
     * Root nodes for trees including a taxon with the supplied ott id. Key 1 is "uid", key 2 is ott id.
     */
    TREE_ROOT_NODES_BY_INCLUDED_TAXON_MAPPED_OTT_ID ("treeRootNodesByInclTaxonMappedOTTId"),

    // ===== indexes by ot: namespace properties
    // NOTE: these could all be collapsed into a single index where the field names used were the ot namespace properties
    // themselves. Advantage would be simpler code, more flexibility (especially non-proliferation of search methods).
    // Disadvantage would be a bigger index... Doesn't seem like much of a disadvantage!

    /**
     * Root nodes for trees indexed by the specified ot namespace property. Key 1 is the ot property name (e.g. "ot:curatorName")
     * and key 2 is the value for that property (e.g. "Romina Gazis").
     */
    TREE_ROOT_NODES_BY_OT_PROPERTY ("treeRootNodesByOTProperty"),
    
    // ===== source indexes

    /**
     * Study metadata nodes indexed by the specified ot namespace properties. Key 1 is the ot property name (e.g. "ot:curatorName")
     * and key 2 is the value for that property (e.g. "Bryan Drew").
     */
	SOURCE_METADATA_NODES_BY_OT_PROPERTY ("sourceMetaNodesByOTProperty"),
    
    /*
     * Study metadata nodes indexed by the ot:curatorName property. Field is "name" (string) and key is curator name.
     *
	SOURCE_METADATA_NODES_BY_OT_CURATOR_NAME ("sourceMetaNodesByOTCurator"),
	
	/*
     * Study metadata nodes indexed by the ot:dataDeposit property. Perhaps useful for, e.g. finding treebase studies? Or not.
     * Field is "dataDeposit" (string) and key is data deposit value (e.g. a URI to the location of the data).
	 *
    SOURCE_METADATA_NODES_BY_OT_DATA_DEPOSIT ("sourceMetaNodesByOTDataDep"),
    
    /*
     * Study metadata nodes indexed by the ot:studyPublicationReference property. Probably mainly useful for fuzzy matching and general
     * search. Field is "reference" (string) and key is publication reference value (e.g. a citation).
     *
    SOURCE_METADATA_NODES_BY_OT_STUDY_PUBLICATION_REFERENCE ("sourceMetaNodesByOTPubRef"), */

    /**
     * Source metadata nodes for both local and remote (i.e. not imported) sources, indexed by their originating source id.
     * Key 1 is either "localSourceId" or "remoteSourceId", and key 2 is the source id (aka study id--a string).
     */
    SOURCE_METADATA_NODES_BY_OT_SOURCE_ID ("sourceMetaNodesByOTSourceId");

    /*
     * Study metadata nodes indexed by the ot:studyPublication property. Field is "pub" (string) and key is
     * study publication value (e.g. a DOI pointing to the study).
     *
    SOURCE_METADATA_NODES_BY_OT_STUDY_PUBLICATION ("sourceMetaNodesByOTStudyPub"),

    /*
     * Study metadata nodes indexed by the ot:studyYear property. Field is "year" (int) and key is study year.
     *
    SOURCE_METADATA_NODES_BY_OT_STUDY_YEAR ("sourceMetaNodesByOTStudyYear"), */
    
    // ===== indexes for locally imported sources
    
	/*
     * Root nodes for IMPORTED TREES, indexed by the tree ids. This is a one-one index, each tree has a unique tree id.
     * field is "treeID", key is tree id.
	 *
    LOCAL_TREE_ROOT_NODES_BY_TREE_ID ("treeRootNodesByTreeIdIMPORTED"); */
    
//    /**
//     * Study metadata nodes FOR IMPORTED STUDIES, indexed by the ot:studyId property. Field is "sourceID" (string) and key is source id (aka study id).
//     */
//    LOCAL_SOURCE_METADATA_NODES_BY_OT_SOURCE_ID ("sourceMetaNodesByOTSourceIdIMPORTED"),

    // ===== other indexes
    
    /*
     * Study metadata nodes indexed by their author. Apparently this field doesn't exist... (yet?).
     * Field is "name" (string) and key is curator name.
     *
    SOURCE_METADATA_NODES_BY_AUTHOR ("sourceMetaNodesBySourceAuthorName"); */

    
    String name;
    
    NodeIndexDescription(String name) {
    	this.name = name;
    }
}
