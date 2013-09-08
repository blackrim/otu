package opentree.otu.constants;

public enum OTUConstants {

	;
	
	public static final String SOURCE_ID = "SourceId";
	public static final String TREE_ID = "TreeId";
	public static final String WHITESPACE_SUBSTITUTE_FOR_SEARCH = "%s%"; //, String.class);
	public static final String LOCAL_TREEID_PREFIX = "__local_id_"; //, String.class);

	public static final NodeProperty[] EDITABLE_TREE_PROPERTIES = {
//		NodeProperty.TREE_ID,
		NodeProperty.OT_TAG,
		NodeProperty.OT_INGROUP_CLADE, // ? not sure if we want this
		NodeProperty.OT_BRANCH_LENGTH_MODE,
//		NodeProperty.ROOTING_IS_SET,
//		NodeProperty.INGROUP_IS_SET
	};
	
	public static final NodeProperty[] EDITABLE_SOURCE_PROPERTIES = {
		NodeProperty.OT_CURATOR_NAME,
		NodeProperty.OT_DATA_DEPOSIT,
		NodeProperty.OT_PUBLICATION_REFERENCE,
		NodeProperty.OT_STUDY_ID,
		NodeProperty.OT_STUDY_PUBLICATION,
		NodeProperty.OT_TAG,
		NodeProperty.OT_YEAR,
	};
	
	public static final NodeProperty[] VISIBLE_OTU_PROPERTIES = {
		NodeProperty.ORIGINAL_LABEL,
		NodeProperty.NAME,
		NodeProperty.IS_WITHIN_INGROUP
		// etc?
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
	
	// We just use the enum to hold arbitrary constant variables as above, so no need to set a generalized structure.
	OTUConstants() {}
}
