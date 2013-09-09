package opentree.otu;

import jade.tree.JadeNode;
import jade.tree.JadeTree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Map;

import opentree.otu.constants.OTUConstants;
import opentree.otu.constants.GraphProperty;
import opentree.otu.constants.NodeProperty;
import opentree.otu.constants.RelType;
import opentree.otu.constants.SearchableProperty;

import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.TermQuery;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.impl.nioneo.store.PropertyType;

public class DatabaseBrowser extends DatabaseAbstractBase {
	
	public final Index<Node> treeRootNodesByTreeId = getNodeIndex(NodeIndexDescription.TREE_ROOT_NODES_BY_TREE_ID);
	public final Index<Node> treeRootNodesBySourceId = getNodeIndex(NodeIndexDescription.TREE_ROOT_NODES_BY_SOURCE_ID);
	public final Index<Node> sourceMetaNodesBySourceId = getNodeIndex(NodeIndexDescription.SOURCE_METADATA_NODES_BY_SOURCE_ID);
	
	private static Set<String> protectedSourceProperties;
	private static Set<String> protectedTreeProperties;
	
	public DatabaseBrowser(EmbeddedGraphDatabase embeddedGraph) {
		super(embeddedGraph);
	}

	public DatabaseBrowser(GraphDatabaseService gdbs) {
		super(gdbs);
		collectProtectedProperties();
	}

	public DatabaseBrowser(GraphDatabaseAgent gdba) {
		super(gdba);
		collectProtectedProperties();
	}

	private void collectProtectedProperties() {
		
		protectedSourceProperties = new HashSet<String>();
		protectedTreeProperties = new HashSet<String>();
		
		for (NodeProperty p : OTUConstants.PROTECTED_SOURCE_PROPERTIES) {
			protectedSourceProperties.add(p.name);
		}

		for (NodeProperty p : OTUConstants.PROTECTED_TREE_PROPERTIES) {
			protectedTreeProperties.add(p.name);
		}
	}
	
	/**
	 * Search the indexes, and get a list of source ids that match the search
	 * @param search
	 * 		A SearchableProperty to specify the search domain
	 * @param searchValue
	 * 		The value to be searched for
	 * @return
	 * 		A list of strings containing the node ids of the source meta nodes for sources found during search
	 */
	public Iterable<String> doBasicSearch(SearchableProperty search, String searchValue) {
		
		HashSet<String> sourceIds = new HashSet<String>();

		// fuzzy query on the fulltext index
		FuzzyQuery fuzzyQuery = new FuzzyQuery(new Term(search.property.name, QueryParser.escape(searchValue)),
    			GeneralUtils.getMinIdentity(searchValue));
		IndexHits<Node> hits = getNodeIndex(search.index).query(fuzzyQuery);
        try {
			for (Node hit : hits) {
				sourceIds.add((String) hit.getProperty(NodeProperty.SOURCE_ID.name));
			}
		} finally {
			hits.close();
		}

        // kludge: special case for exact taxon names searches with spaces.
        // having this here avoids having to create lots of unnecessary abstraction
        if (search.equals(SearchableProperty.DESCENDANT_MAPPED_TAXON_NAMES)) {
			hits = getNodeIndex(NodeIndexDescription.TREE_ROOT_NODES_BY_MAPPED_TAXON_NAME_WHITESPACE_FILLED)
					.get(search.property.name, searchValue);
            try {
    			for (Node hit : hits) {
    				sourceIds.add((String) hit.getProperty(NodeProperty.SOURCE_ID.name));
    			}
    		} finally {
    			hits.close();
    		}
        }        
        
		return sourceIds;
	}
	
	/**
	 * Search all known remotes to see if they contain a source with the specified id. Returns an iterable of source meta
	 * nodes for all matching sources.
	 * @param sourceId
	 * @return
	 */
	public List<Node> getRemoteSourceMetaNodesForSourceId(String sourceId) {
		List<Node> remoteSourceMetasFound = new LinkedList<Node>();
		for (String remote : getKnownRemotes()) {
			Node sourceMeta = DatabaseUtils.getSingleNodeIndexHit(sourceMetaNodesBySourceId, remote + OTUConstants.SOURCE_ID, sourceId);
			if (sourceMeta != null) {
				remoteSourceMetasFound.add(sourceMeta);
			}
		}
		return remoteSourceMetasFound;
	}
	
	/**
	 * Get the array of known remote identifier strings
	 * @return
	 */
	public List<String> getKnownRemotes() {
		List<String> knownRemotes = new ArrayList<String>();
		String[] knownRemotesArr = (String[]) graphDb.getGraphProperty(GraphProperty.KNOWN_REMOTES);
		if (knownRemotesArr != null) {
			for (String remote : knownRemotesArr) {
				knownRemotes.add(remote);
			}
		}
		return knownRemotes;
	}
	
	/*
	 * Returns a JSON array string of sources that have imported trees.
	 * @return
	 *
	public String getJSONOfSourceIdsForImportedTrees() {

		// find all imported trees, get their study ids from the attached metadata nodes
		HashSet<String> sourceIds = new HashSet<String>();
		IndexHits<Node> sourcesFound = null;
		try {
			sourcesFound = sourceMetaNodesBySourceId.query(LOCAL_LOCATION+"SourceId"+":*");
			for (Node s : sourcesFound) {
				sourceIds.add((String) s.getProperty(NodeProperty.SOURCE_ID.name));
			}
		} finally {
			sourcesFound.close();
		}
		
		// write the string
		StringBuffer json = new StringBuffer("{ \"sources\" : [");
		Iterator<String> sourceIdsIter = sourceIds.iterator();
		while (sourceIdsIter.hasNext()) {
			json.append("\"" + sourceIdsIter.next() + "\" ");
			if (sourceIdsIter.hasNext()) {
				json.append(",");
			}
		}
		json.append("] }");
		
		return json.toString();
	} */
	
	/**
	 * Return a list containing all the tree ids for the specified source id.
	 * @return
	 */
	public Map<String, Object> getSourceIds(String location) {
		return getSourceIds(location, new HashSet<String>());
	}
	
	/**
	 * Return a list containing the ids of all imported sources
	 * @return
	 */
	public Map<String, Object> getSourceIds(String location, Set<String> excludedSourceIds) {
		List<String> sourceIds = new LinkedList<String>();
		
		IndexHits<Node> sourcesFound = sourceMetaNodesBySourceId.query(location + OTUConstants.SOURCE_ID + ":*");
		try {
			while (sourcesFound.hasNext()) {
				String sid = (String) sourcesFound.next().getProperty(NodeProperty.SOURCE_ID.name);
				if (!excludedSourceIds.contains(sid)) {
					sourceIds.add(sid);
				}
			}
		} finally {
			sourcesFound.close();
		}
		
		Map<String, Object> results = new HashMap<String, Object>();
		results.put("sources", sourceIds);
		return results;
	}
		
	/*
	 * Return a JSON string of source ids and corresponding tree ids for all locally-imported sources.
	 * @return
	 *
	@Deprecated
	public String getJSONOfSourceIdsAndTreeIdsForImportedTrees() {
		
		StringBuffer retstr = new StringBuffer("{ \"studies\" : [");
		IndexHits<Node> hits = treeRootNodesByTreeId.query(LOCAL_LOCATION + "TreeId", "*");
		try {
			while (hits.hasNext()) {
				retstr.append("[ \"");
				Node x = hits.next();
				retstr.append((String) x.getSingleRelationship(RelType.METADATAFOR, Direction.INCOMING)
						.getStartNode().getProperty(NodeProperty.SOURCE_ID.name));
				retstr.append("\", \"");
				retstr.append((String) x.getProperty(NodeProperty.TREE_ID.name));
				retstr.append("\"]");
				if (hits.hasNext()) {
					retstr.append(",");
				}
			}
		} finally {
			hits.close();
		}
		retstr.append("] }");
		return retstr.toString();

	} */
	
	/**
	 * Return a list containing all the tree ids for the specified source id.
	 * @return
	 */
	public Map<String, Object> getTreeIdsForSourceId(String location, String sourceId) {
		return getTreeIdsForSource(location, sourceId, new HashSet<String>());
	}
	
	/**
	 * Return a list containing all the tree ids for the specified source id except any tree ids that are in the excludedTreeIds variable.
	 * @return
	 */
	public Map<String, Object> getTreeIdsForSource(String location, String sourceId, Set<String> excludedTreeIds) {
	
		List<String> treeIds = new LinkedList<String>();
		
		IndexHits<Node> hits = treeRootNodesBySourceId.query(location + OTUConstants.SOURCE_ID + ":" + sourceId);
		try {
			while (hits.hasNext()) {
				String tid = (String) hits.next().getProperty(NodeProperty.TREE_ID.name);
				if (!excludedTreeIds.contains(tid)) {
					treeIds.add(tid);
				}
			}
		} finally {
			hits.close();
		}
		
		Map<String, Object> results = new HashMap<String, Object>();
		results.put("trees", treeIds);
		return results;
	}
	
	/**
	 * get a list of otu nodes based on a study metadatanode. Used by NexsonWriter.
	 */
	public HashSet<Node> getOTUsFromMetadataNode(Node sourceMeta){
		HashSet<Node> reths =  new HashSet<Node>();
		for (Relationship rel: sourceMeta.getRelationships(Direction.OUTGOING, RelType.METADATAFOR)){
			Node treeroot = rel.getEndNode();
			reths.addAll(getDescendantTips(treeroot));
		}
		return reths;
	}
	 
	/**
	 * Get the set of tip nodes descended from a tree node. Used by NexsonWriter
	 * 
	 * @param ancestor
	 * 		The start node for the traversal. All tip nodes descended from this node will be included in the result.
	 * @return
	 * 		A set containing the nodes found by the tree traversal. Returns an empty set if no nodes are found.
	 */
	public static Set<Node> getDescendantTips(Node ancestor) { // does not appear to be used.
		HashSet<Node> descendantTips = new HashSet<Node>();
		TraversalDescription CHILDOF_TRAVERSAL = Traversal.description().relationships(RelType.CHILDOF, Direction.INCOMING);
		for (Node curGraphNode: CHILDOF_TRAVERSAL.breadthFirst().traverse(ancestor).nodes()) {
			if (curGraphNode.hasProperty("oty")) { // what is this? should this be "otu"?
				descendantTips.add(curGraphNode);
			}
		}
		return descendantTips;
	}
	
	/**
	 * Retrieve a source metadata node from the graph.
	 * @param sourceId
	 * 		The id of the source to get
	 * @param location
	 * 		The location of the study containing the tree to get. For local trees, use DatabaseBrowser.LOCAL_LOCATION
	 * @return
	 * 		The metadata node for this source, or null if no such source exists
	 */
	public Node getSourceMetaNode(String sourceId, String location) {
		return DatabaseUtils.getSingleNodeIndexHit(sourceMetaNodesBySourceId, location + OTUConstants.SOURCE_ID, sourceId);
	}
	
	/**
	 * Retrieve a tree root node from the graph.
	 * @param treeId
	 * 		The id of the tree to get
	 * @param location
	 * 		The location of the study containing the tree to get. For local trees, use DatabaseBrowser.LOCAL_LOCATION
	 * @return
	 * 		The root node for this tree, or null if no such tree exists
	 */
	public Node getTreeRootNode(String treeId, String location) {
		return DatabaseUtils.getSingleNodeIndexHit(treeRootNodesByTreeId, location + OTUConstants.TREE_ID, treeId);
	}
	
	/**
	 * Return a map of relevant properties for the specified OTU node.
	 * @param otu
	 * @return
	 */
	public Map<String, Object> getMetadataForOTU(Node otu) {
		
		Map<String, Object> metadata = new HashMap<String, Object>();

		// TODO: we may want to make this consistent with the  protected source property behavior tree root and source meta nodes
		for (NodeProperty property : OTUConstants.VISIBLE_OTU_PROPERTIES) {
			Object value = (Object) "";
			if (otu.hasProperty(property.name)) {
				value = otu.getProperty(property.name);
			}
			metadata.put(property.name, value);
		}
		
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("metadata", metadata);
		
		return result;

	}
	
	/**
	 * Return a map containing the metadata for the corresponding source. Will fail if the provided node
	 * is not a source metadata node. A general purpose method that gathers information about local and remote sources.
	 * 
	 * @param sourceMeta
	 * 		The metadata node for the source
	 * @return
	 * 		A map containing information about this source
	 */
	public Map<String, Object> getMetadataForSource(Node sourceMeta) {
		
		// get properties indicated for public consumption
		Map<String, Object> metadata = new HashMap<String, Object>();
		for (String key : sourceMeta.getPropertyKeys()) {

			if (!protectedSourceProperties.contains(key)) {
				
				Object value = (Object) "";
				if (sourceMeta.hasProperty(key)) {
					value = sourceMeta.getProperty(key);
				}
				metadata.put(key, value);
			}
		}

		// get the trees
		List<String> trees = new LinkedList<String>(); // will actually store the tree ids
		for (Relationship rel : sourceMeta.getRelationships(RelType.METADATAFOR, Direction.OUTGOING)) {
			trees.add((String) rel.getEndNode().getProperty(NodeProperty.TREE_ID.name));
		}

		// check if local
		boolean hasLocalCopy = false;
		List<String> remotes = new LinkedList<String>();
		if (sourceMeta.getProperty(NodeProperty.LOCATION.name).equals(LOCAL_LOCATION)) {
			hasLocalCopy = true;
		} else {
			hasLocalCopy = false;
		}

		// check for remotes
		for (Node remoteMeta : getRemoteSourceMetaNodesForSourceId((String) sourceMeta.getProperty(NodeProperty.SOURCE_ID.name))) {
			remotes.add((String) remoteMeta.getProperty(NodeProperty.LOCATION.name));
		}

		// put it together and what have you got
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("sourceId", sourceMeta.getProperty(NodeProperty.SOURCE_ID.name));
		result.put("metadata", metadata);
		result.put("trees", trees);
		result.put("has_local_copy", hasLocalCopy);
		result.put("remotes_known", remotes);

		// bibbity bobbity boo
		return result;
	}

	/**
	 * Get a JSON string containing tree metadata for the specified tree root node. Will fail if this node is not the
	 * root node of a tree.
	 * 
	 * @param root
	 * 		The root node of a tree
	 * @return
	 * 		A map containing information about this tree
	 */
	public static Map<String, Object> getMetadataForTree(Node root) {

		// gather properties suitable for public consumption
		Map<String, Object> metadata = new HashMap<String, Object>();
		for (String key : root.getPropertyKeys()) {

			if (!protectedTreeProperties.contains(key)) {

				Object value = (Object) "";
				if (root.hasProperty(key)) {
					value = root.getProperty(key);
				}
				metadata.put(key, value);
			}
		}
		
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("treeId", root.getProperty((String) NodeProperty.TREE_ID.name));
		result.put("sourceId", root.getProperty((String) NodeProperty.SOURCE_ID.name));
		result.put("metadata", metadata);
		
		return result;
	}
	
	/**
	 * Get the subtree of a given tree graph node. Does not perform error checks to make sure the tree exists.
	 * @param inRoot
	 * @param maxNodes
	 * @return
	 */
	public static JadeTree getTreeFromNode(Node inRoot, int maxNodes) {
		TraversalDescription CHILDOF_TRAVERSAL = Traversal.description().relationships(RelType.CHILDOF, Direction.INCOMING);
		JadeNode root = new JadeNode();
		HashMap<Node, JadeNode> traveledNodes = new HashMap<Node, JadeNode>();
		int maxtips = maxNodes;
		HashSet<Node> includednodes = new HashSet<Node>();
		HashSet<Node> parents = new HashSet<Node>();
		for (Node curGraphNode : CHILDOF_TRAVERSAL.breadthFirst().traverse(inRoot).nodes()) {
			if (includednodes.size() > maxtips && parents.size() > 1) {
				break;
			}
			JadeNode curNode = null;
			if (curGraphNode == inRoot) {
				curNode = root;
			} else {
				curNode = new JadeNode();
			}
			traveledNodes.put(curGraphNode, curNode);
			if (curGraphNode.hasProperty(NodeProperty.NAME.name)) {
				curNode.setName(GeneralUtils.cleanName(String.valueOf(curGraphNode.getProperty(NodeProperty.NAME.name))));
				// curNode.setName(GeneralUtils.cleanName(curNode.getName()));
			}
			if (curGraphNode.hasProperty(NodeProperty.IS_WITHIN_INGROUP.name)) {
				curNode.assocObject("ingroup", true);
			}
			curNode.assocObject("nodeId", String.valueOf(curGraphNode.getId()));
			JadeNode parentJadeNode = null;
			Relationship incomingRel = null;
			if (curGraphNode.hasRelationship(Direction.OUTGOING, RelType.CHILDOF) && curGraphNode != inRoot) {
				Node parentGraphNode = curGraphNode.getSingleRelationship(RelType.CHILDOF, Direction.OUTGOING).getEndNode();
				if (includednodes.contains(parentGraphNode)) {
					includednodes.remove(parentGraphNode);
				}
				parents.add(parentGraphNode);
				if (traveledNodes.containsKey(parentGraphNode)) {
					parentJadeNode = traveledNodes.get(parentGraphNode);
					incomingRel = curGraphNode.getSingleRelationship(RelType.CHILDOF, Direction.OUTGOING);
				}
			}
			includednodes.add(curGraphNode);
			// add the current node to the tree we're building

			if (parentJadeNode != null) {
				parentJadeNode.addChild(curNode);
			}
			// get the immediate synth children of the current node
			LinkedList<Relationship> taxChildRels = new LinkedList<Relationship>();
			int numchild = 0;
			for (Relationship taxChildRel : curGraphNode.getRelationships(Direction.INCOMING, RelType.CHILDOF)) {
				taxChildRels.add(taxChildRel);
				numchild += 1;
			}
			if (numchild > 0) {
				// need to add a property of the jadenode if there are children, so if they aren't included, we can color it
				curNode.assocObject("haschild", true);
				curNode.assocObject("numchild", numchild);
			}
		}
		int curbackcount = 0;
		boolean going = true;
		JadeNode newroot = root;
		Node curRoot = inRoot;
		while (going && curbackcount < 5) {
			if (curRoot.hasRelationship(Direction.OUTGOING, RelType.CHILDOF)) {
				Node curGraphNode = curRoot.getSingleRelationship(RelType.CHILDOF, Direction.OUTGOING).getEndNode();
				JadeNode temproot = new JadeNode();
				if (curGraphNode.hasProperty(NodeProperty.NAME.name)) {
					temproot.setName(GeneralUtils.cleanName(String.valueOf(curGraphNode.getProperty(NodeProperty.NAME.name))));
				}
				temproot.assocObject("nodeId", String.valueOf(curGraphNode.getId()));
				temproot.addChild(newroot);
				curRoot = curGraphNode;
				newroot = temproot;
				curbackcount += 1;
			} else {
				going = false;
				break;
			}
		}
		// (add a bread crumb)
		return new JadeTree(newroot);
	}
}
