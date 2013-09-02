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
	 * A unique string used to identify this tree within the db. The convention is to use the study id concatenated
	 * by an underscore to an id unique for trees within studies, e.g. 10_1. For trees incoming from nexsons, we attempt
	 * to use any incoming tree id. If this is absent, or if the tree is not coming from a nexson, we assign an arbitrary
	 * id string that is unique for trees within the originating study, e.g. 10____local_id_1.
	 */
	TREE_ID (String.class),
			
	/**
	 * A unique string used to identify this source. For nexsons, this is the study id. For local sources, this is assigned
	 * on import.
	 */
	SOURCE_ID (String.class),
	
	/**
	 * A unique string identifying the repository to which tree and source nodes belong. Currently, the only options are
	 * "remote" and "local", although multiple repos could be indicated by using other values.
	 */
	LOCATION (String.class);

	public final Class<?> type; // indicates the datatype for this property
    
    NodeProperty(Class<?> T) {
        this.type = T;
    }
}
