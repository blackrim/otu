package opentree.otu.constants;

public enum GeneralConstants {

	;
	
	public static final String WHITESPACE_SUBSTITUTE_FOR_SEARCH = "%s%"; //, String.class);
	public static final String LOCAL_TREEID_PREFIX = "__local_id_"; //, String.class);
//	VISIBLE_TREE_ROOT_PROPERTIES (visibleTreeProperties, NodeProperty[].class);

//	public Object value;
//	public Class<?> type;

	public static final NodeProperty[] VISIBLE_TREE_PROPERTIES = {
		NodeProperty.TREE_ID,
		NodeProperty.OT_TAG,
		NodeProperty.SOURCE_ID,
		NodeProperty.ROOTING_IS_SET,
		NodeProperty.IS_ROOT,
		NodeProperty.INGROUP_IS_SET,
		NodeProperty.IS_INGROUP
	};
	
	public static final NodeProperty[] VISIBLE_SOURCE_PROPERTIES = {
		NodeProperty.SOURCE_ID,
		NodeProperty.LOCATION,
		NodeProperty.OT_CURATOR_NAME,
		NodeProperty.OT_DATA_DEPOSIT,
		NodeProperty.OT_FOCAL_CLADE,
		NodeProperty.OT_INGROUP_CLADE,
		NodeProperty.OT_PUBLICATION_REFERENCE,
		NodeProperty.OT_STUDY_ID,
		NodeProperty.OT_STUDY_PUBLICATION,
		NodeProperty.OT_TAG,
		NodeProperty.OT_YEAR,
	};
	
	// We just use the enum to hold constant variables, no need to set general form.
	GeneralConstants() {
		
	}
//	GeneralConstants (Object value, Class<?> type) {
//		this.value = value;
//		this.type = type;
//	}
}
