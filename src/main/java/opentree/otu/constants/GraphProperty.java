package opentree.otu.constants;

/**
 * Graph properties are stored in node 0. These are basic pieces of information used to identify the graph itself. For more information see:
 * 
 * https://github.com/OpenTreeOfLife/treemachine/wiki/Vocabulary
 * 
 * @author cody hinchliff
 *
 */
public enum GraphProperty {
	
	NEXSON_GIT_DIR ("nexsonGitDir", String.class, "The directory on the current system where the nexson git lies"),//, to add more
	KNOWN_REMOTES ("known_remotes", String[].class, "An array containing the names for all known remotes. To facilitate multiple remotes");
	
	public String propertyName;
	public final Class<?> type;
	public final String description;
    
    GraphProperty(String propertyName, Class<?> T, String description) {
        this.propertyName = propertyName;
        this.type = T;
        this.description = description;
    }
}
