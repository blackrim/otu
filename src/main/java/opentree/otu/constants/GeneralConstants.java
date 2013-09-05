package opentree.otu.constants;

public enum GeneralConstants {
	
	WHITESPACE_SUBSTITUTE_FOR_SEARCH ("%s%", String.class),
	LOCAL_TREEID_PREFIX ("__local_id_", String.class);
//	VISIBLE_TREE_ROOT_PROPERTIES (visibleTreeProperties, NodeProperty[].class);

	public Object value;
	public Class<?> type;

	public static final NodeProperty[] visibleTreeProperties = {
		NodeProperty.TREE_ID,
		NodeProperty.OT_TAG,
		NodeProperty.SOURCE_ID,
		NodeProperty.ROOTING_IS_SET,
		NodeProperty.IS_ROOT,
		NodeProperty.INGROUP_IS_SET,
		NodeProperty.IS_INGROUP
	};
	
	GeneralConstants (Object value, Class<?> type) {
		this.value = value;
		this.type = type;
	}
}
