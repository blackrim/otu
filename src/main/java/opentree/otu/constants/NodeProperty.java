package opentree.otu.constants;

import org.neo4j.graphdb.Node;

/**
 * Node properties as defined within the graph itself. These are stored in graph nodes. Different types
 * of nodes may have different properties. For more information see:
 * 
 * https://github.com/OpenTreeOfLife/treemachine/wiki/Vocabulary
 * 
 */
public enum NodeProperty {

	/**
	 * The original taxon name associated with this node.
	 */
	ORIGINAL_NAME ("original_name", String.class),	
	
	/**
	 * The taxon name associated with this node. SHOULD BE THE MAPPED NAME, not the original. Should not be set if the
	 * node has not been mapped (although I have no idea if this is actually the case).
	 */
	NAME ("name", String.class),
	
	/*
	 * The ott id associated with the taxon mapped to this node. Should not be set if the node has not been mapped.
	 *
	OTT_ID ("uid", String.class), */
	
	/**
	 * A unique string used to identify this tree within the db. The convention is to use the study id concatenated
	 * by an underscore to an id unique for trees within studies, e.g. 10_1. For trees incoming from nexsons, we attempt
	 * to use any incoming tree id. If this is absent, or if the tree is not coming from a nexson, we assign an arbitrary
	 * id string that is unique for trees within the originating study, e.g. 10____local_id_1.
	 */
	TREE_ID ("tree_id", String.class),
			
	/**
	 * A unique string used to identify this source. For nexsons, this is the study id. For local sources, this is assigned
	 * on import.
	 */
	SOURCE_ID ("source_id", String.class),
	
	/**
	 * A unique string identifying the repository to which tree and source nodes belong. Currently, the only options are
	 * "remote" and "local", although multiple repos could be indicated by using other values.
	 */
	LOCATION ("location", String.class),
	
	/**
	 * A primitive string array containing all the original taxon names applied to tip children of a given tree node.
	 * This is stored as a property of the root of each imported tree.
	 */
	DESCENDANT_ORIGINAL_TAXON_NAMES ("tip_original_names", String[].class),
	
	/**
	 * A primitive string array containing all the currently mapped taxon names applied to tip children of a given tree node.
	 * This is stored as a property of the root of each imported tree.
	 */
	DESCENDANT_MAPPED_TAXON_NAMES ("tip_mapped_names", String[].class),

	/**
	 * A primitive string array containing all the currently mapped taxon names applied to tip children of a given tree node,
	 * with whitespace replaced by the whitespace replacement substring specified in GeneralConstants. This is stored as a
	 * property of the root of each imported tree.
	 */
	DESCENDANT_MAPPED_TAXON_NAMES_WHITESPACE_FILLED ("tip_mapped_names_no_spaces", String[].class),
	
	/**
	 * A primitive string array containing all the ott ids for taxa mapped to the tip children of a given tree node.
	 * This is stored as a property of the root of each imported tree.
	 */
	DESCENDANT_MAPPED_TAXON_OTT_IDS ("tip_mapped_ottids", long[].class),
	
	/**
	 * A boolean indicating whether this tree has been rooted. Stored as a property of the root node. If the tree root lacks
	 * this property then it can be inferred that the tree has not been rooted.
	 */
	ROOTING_IS_SET ("is_rooted", boolean.class),
	
	/**
	 * A boolean indicating that this node is the root for its tree. Should always (and only) be set to true for the root
	 * node. All other tree nodes should lack this property entirely.
	 */
	IS_ROOT ("is_root", boolean.class),
	
	/**
	 * A boolean indicating whether the ingroup is set for this tree. Stored as a property of the tree root node. If the
	 * tree root lacks this property, then the ingroup can be inferred not to be set.
	 */
	INGROUP_IS_SET ("ingroup_set", boolean.class),
	
	/**
	 * A flag specifying that the clade represented by the node is the ingroup for the tree. Is only set on ingroup nodes.
	 * Thus, any node without this property can be inferred not to be the ingroup on a tree for which the ingroup is set.
	 */
	IS_INGROUP ("ingroup_start", boolean.class),
	
	// ===== ot namespace node properties
	
	/**
	 * The type of branch lengths for this tree. A property of tree root nodes.
	 */
	OT_BRANCH_LENGTH_MODE("ot:branchLengthMode", String.class),

	/**
	 * The name of curator who uploaded the source. Used for study sources.
	 */
	OT_CURATOR_NAME("ot:curatorName", String.class),

	/**
	 * A URI (or other identifier) for the published data. Used for study sources.
	 */
	OT_DATA_DEPOSIT("ot:dataDeposit", String.class),

	/**
	 * The OTT id of the focal clade for this source. A property of source meta nodes.
	 */
	OT_FOCAL_CLADE("", String.class),
	
	/**
	 * The node that is the designated ingroup for this tree. A property of tree root nodes.
	 */
	OT_INGROUP_CLADE("", Node.class),
	
	/**
	 * The original label assigned to this node.
	 */
	OT_ORIGINAL_LABEL ("ot:originalLabel", String.class),	

	/**
	 * The ott id associated with the node. A property of tip nodes. Should not be set if the node has not been mapped.
	 */
	OT_OTT_ID ("ot:ottolid", Long.class),	
	
	/**
	 * The citation string for published studies. A property of source meta nodes.
	 */
    OT_PUBLICATION_REFERENCE ("ot:studyPublicationReference", String.class),

    /**
     * The phylografter study id. A property of source meta nodes.
     */
    OT_STUDY_ID ("ot:studyId", String.class),
    
    /**
     * A URI (or other identifier) for published studies. A property of source meta nodes.
     */
    OT_STUDY_PUBLICATION("ot:studyPublication", String.class),
    
    /**
     * The tag field exported from phylografter. A property of source meta nodes.
     */
    OT_TAG ("ot:tag", String.class),
    
    /**
     * The year the study was published. A property of source meta nodes.
     */
    OT_YEAR ("ot:studyYear", int.class);
	
	public final String name; // the property name. Used to identify this property in indexes and on nodes
	public final Class<?> type; // indicates the datatype for this property
    
    NodeProperty(String name, Class<?> type) {
    	this.name = name;
        this.type = type;
    }
}
