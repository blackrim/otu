package jade.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class NexsonSource {

	private String sourceId;
	private HashMap<String, Object> properties;
	private ArrayList<JadeTree> trees;
	
	public NexsonSource(String sourceId) {
		this.sourceId = sourceId;
		properties = new HashMap<String, Object>();
		trees = new ArrayList<JadeTree>();
	}
	
	public String getId() {
		return sourceId;
	}
	
	public Iterable<JadeTree> getTrees() {
		return trees;
	}
	
	public Map<String, Object> getProperties() {
		return properties;
	}
	
	public void setProperty(String propertyName, Object value) {
		properties.put(propertyName, value);
	}
	
	public void addTree(JadeTree tree) {
		trees.add(tree);
	}
	
	public void addTrees(Collection<JadeTree> treesToAdd) {
		this.trees.addAll(treesToAdd);
	}
	
	public void setProperty(String propertyName, String value) {
		properties.put(propertyName, value);
	}
	
	public Object getProperty(String propertyName) {
		return properties.get(propertyName);
	}
	
	public int getTreeCount() {
		return trees.size();
	}
}
