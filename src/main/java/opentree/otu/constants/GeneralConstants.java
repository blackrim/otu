package opentree.otu.constants;

public enum GeneralConstants {

	WHITESPACE_SUBSTITUTE_FOR_SEARCH ("%s%", String.class),
	LOCAL_TREEID_PREFIX ("__local_id_", String.class);
	
	public Object value;
	public Class<?> type;
	
	GeneralConstants (Object value, Class<?> type) {
		this.value = value;
		this.type = type;
	}
}
