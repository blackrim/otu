package opentree.otu.constants;

import opentree.otu.NodeIndexDescription;

public enum GeneralConstants {

	;
	
	public static final String WHITESPACE_SUBSTITUTE_FOR_SEARCH = "%s%"; //, String.class);
	public static final String LOCAL_TREEID_PREFIX = "__local_id_"; //, String.class);

	public static final NodeProperty[] VISIBLE_TREE_PROPERTIES = {
		NodeProperty.TREE_ID,
		NodeProperty.OT_TAG,
		NodeProperty.OT_INGROUP_CLADE, // ? not sure if we want this
//		NodeProperty.FOCAL_CLADE, // ? not sure if we want this // either way it is not set
		NodeProperty.OT_BRANCH_LENGTH_MODE,
//		NodeProperty.SOURCE_ID,
		NodeProperty.ROOTING_IS_SET,
		NodeProperty.INGROUP_IS_SET
//		NodeProperty.INGROUP_START_NODE_ID
	};
	
	public static final NodeProperty[] VISIBLE_SOURCE_PROPERTIES = {
		NodeProperty.SOURCE_ID,
		NodeProperty.LOCATION,
		NodeProperty.OT_CURATOR_NAME,
		NodeProperty.OT_DATA_DEPOSIT,
//		NodeProperty.FOCAL_CLADE, // ? is this a study property or a tree property? // either way it is not set
//		NodeProperty.OT_INGROUP_CLADE, // ? is this a study property or a tree property?
		NodeProperty.OT_PUBLICATION_REFERENCE,
		NodeProperty.OT_STUDY_ID,
		NodeProperty.OT_STUDY_PUBLICATION,
		NodeProperty.OT_TAG,
		NodeProperty.OT_YEAR,
	};
	
	public static final SearchableProperty[] TREE_PROPERTIES_FOR_SIMPLE_INDEXING = {
		SearchableProperty.BRANCH_LENGTH_MODE,
		SearchableProperty.TAG_TREE
	};
	
	public static final SearchableProperty[] SOURCE_PROPERTIES_FOR_SIMPLE_INDEXING = {
		SearchableProperty.CURATOR_NAME,
		SearchableProperty.DATA_DEPOSIT,
		SearchableProperty.PUBLICATION_REFERENCE,
		SearchableProperty.SOURCE_ID,
		SearchableProperty.STUDY_PUBLICATION,
		SearchableProperty.YEAR,
		SearchableProperty.TAG_SOURCE
	};
	
	// We just use the enum to hold constant variables, no need to set general form.
	GeneralConstants() {}
}
