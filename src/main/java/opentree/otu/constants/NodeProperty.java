package opentree.otu.constants;

/**
 * Node properties as defined within the graph itself. These are stored in graph nodes. Different types
 * of nodes may have different properties. For more information see:
 * 
 * https://github.com/OpenTreeOfLife/treemachine/wiki/Vocabulary
 * 
 */
public enum NodeProperty {

	/**
	 * The taxon name associated with this node. TODO: NOT CLEAR IF THIS IS MAPPED, ORIGINAL?
	 */
	NAME ("name", String.class),
	
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
	ORIGINAL_TAXON_NAMES ("tip_original_names", String[].class),
	
	/**
	 * A primitive string array containing all the currently mapped taxon names applied to tip children of a given tree node.
	 * This is stored as a property of the root of each imported tree.
	 */
	MAPPED_TAXON_NAMES ("tip_mapped_names", String[].class),

	/**
	 * A primitive string array containing all the currently mapped taxon names applied to tip children of a given tree node,
	 * with whitespace replaced by the whitespace replacement substring specified in GeneralConstants. This is stored as a
	 * property of the root of each imported tree.
	 */
	MAPPED_TAXON_NAMES_WHITESPACE_FILLED ("tip_mapped_names_no_spaces", String[].class),
	
	/**
	 * A primitive string array containing all the ott ids for taxa mapped to the tip children of a given tree node.
	 * This is stored as a property of the root of each imported tree.
	 */
	MAPPED_TAXON_OTT_IDS ("tip_mapped_ottids", long[].class),
	
	// ===== ot namespace node properties
	
	/**
	 * The original label assigned to this node.
	 */
	OT_ORIGINAL_LABEL ("ot:originalLabel", String.class),	

	/**
	 * The ott id associated with the node. A property of tip nodes
	 */
	OT_OTTID ("ot:ottolid", Long.class),	
	
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
	IS_INGROUP ("ingroup_start", boolean.class);	

	public final Class<?> type; // indicates the datatype for this property
	public final String name;
    
    NodeProperty(String name, Class<?> T) {
    	this.name = name;
        this.type = T;
    }
}
