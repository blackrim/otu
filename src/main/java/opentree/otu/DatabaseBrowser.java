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
	public final Index<Node> sourceMetaNodesBySourceId = getNodeIndex(NodeIndexDescription.SOURCE_METADATA_NODES_BY_SOURCE_ID);
	
	public DatabaseBrowser(EmbeddedGraphDatabase embeddedGraph) {
		super(embeddedGraph);
	}

	public DatabaseBrowser(GraphDatabaseService gdbs) {
		super(gdbs);
	}

	public DatabaseBrowser(GraphDatabaseAgent gdba) {
		super(gdba);
	}
	
	/**
	 * Search the indexes, and get a list of source ids that match the search
	 * @param search
	 * 		A SearchableProperty to specify the search domain
	 * @param searchValue
	 * 		The value to be searched for
	 * @return
	 * 		A list of strings containing the node ids for the source meta nodes for sources found during search
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
			Node sourceMeta = DatabaseUtils.getSingleNodeIndexHit(sourceMetaNodesBySourceId, remote+"SourceId",sourceId);
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
	
	/**
	 * Returns a JSON array string of sources that have imported trees.
	 * @return
	 */
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
	}

	/**
	 * Return a JSON string of source ids and corresponding tree ids for all locally-imported sources.
	 * @return
	 */
	public String getJSONOfSourceIdsAndTreeIdsForImportedTrees() {

		StringBuffer retstr = new StringBuffer("{ \"studies\" : [");
		IndexHits<Node> hits = treeRootNodesByTreeId.query(LOCAL_LOCATION + "TreeId", "*");
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
		hits.close();
		retstr.append("] }");
		return retstr.toString();
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
	public static Set<Node> getDescendantTips(Node ancestor){ // does not appear to be used.
		HashSet<Node> descendantTips = new HashSet<Node>();
		TraversalDescription CHILDOF_TRAVERSAL = Traversal.description().relationships(RelType.CHILDOF, Direction.INCOMING);
		for(Node curGraphNode: CHILDOF_TRAVERSAL.breadthFirst().traverse(ancestor).nodes()){
			if(curGraphNode.hasProperty("oty")){ // what is this? should this be "otu"?
				descendantTips.add(curGraphNode);
			}
		}
		return descendantTips;
	}
	
	/**
	 * Retrieve a source metadata node from the graph.
	 * @param treeId
	 * 		The id of the tree to get
	 * @param location
	 * 		The location of the study containing the tree to get. For local trees, use DatabaseBrowser.LOCAL_LOCATION
	 * @return
	 * 		The metadata node for this source, or null if no such source exists
	 */
	public Node getSourceMetaNode(String sourceId, String location) {
		return DatabaseUtils.getSingleNodeIndexHit(sourceMetaNodesBySourceId, location + "SourceId", sourceId);
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
		return DatabaseUtils.getSingleNodeIndexHit(treeRootNodesByTreeId, location + "TreeId", treeId);
	}
	
	/**
	 * Return a JSON string containing the metadata for the corresponding source. Will fail if the provided node
	 * is not a source metadata node. A general purpose method that gathers information about local and remote sources.
	 * 
	 * @param sourceMeta
	 * 		The metadata node for the source
	 * @return
	 * 		A JSON string containing metadata for this source
	 */
	public Map<String, Object> getSourceMetadata(Node sourceMeta) {
		
		// get properties indicated for public consumption
		Map<String, Object> metadata = new HashMap<String, Object>();
		for (NodeProperty property : OTUConstants.VISIBLE_SOURCE_PROPERTIES) {
			Object value = (Object) "";
			if (sourceMeta.hasProperty(property.name)) {
				value = sourceMeta.getProperty(property.name);
			}
			metadata.put(property.name, value);
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
	 * 		A JSON string containing of the metadata for this tree
	 */
	public static Map<String, Object> getMetadataForTree(Node root) {

		Map<String, Object> metadata = new HashMap<String, Object>();

		for (NodeProperty property : OTUConstants.VISIBLE_TREE_PROPERTIES) {
			Object value = (Object) "";
			if (root.hasProperty(property.name)) {
				value = root.getProperty(property.name);
			}
			metadata.put(property.name, value);
		}
		
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("metadata", metadata);
		
		return result;

		// we could set properties to *always* be included in a separate enum. as it stands, we will only
		// see properties that actually exist for the node
		
/*		StringBuffer bf = new StringBuffer("{ \"metadata\": {");

		boolean first = true;
		for (String p : root.getPropertyKeys()) {
			if (first) {
				first = false;
			} else {
				bf.append(",");
			}
			bf.append("\"" + p + "\" : \"" + String.valueOf(root.getProperty(p)) + "\"");
		}
		
/*		if (root.hasProperty("ot:branchLengthMode")) {
			bf.append(String.valueOf(root.getProperty("ot:branchLengthMode")));
		}
		bf.append("\", \"ot:inGroupClade\": \"");
		if (root.hasProperty("ot:inGroupClade")) {
			bf.append(String.valueOf(root.getProperty("ot:inGroupClade")));
		}
		bf.append("\", \"ot:focalClade\": \"");
		if (root.hasProperty("ot:focalClade")) {
			bf.append(String.valueOf(root.getProperty("ot:focalClade")));
		}
		bf.append("\", \"ot:tag\": \"");
		if (root.hasProperty("ot:tag")) {
			bf.append(String.valueOf(root.getProperty("ot:tag")));
		}
		bf.append("\", \"rooting_set\": \"");
		if (root.hasProperty("rooting_set")) {
			bf.append(String.valueOf(root.getProperty("rooting_set")));
		}
		bf.append("\", \"ingroup_set\": \"");
		if (root.hasProperty("ingroup_set")) {
			bf.append(String.valueOf(root.getProperty("ingroup_set")));
		} */

/*		bf.append("} }");
		return bf.toString(); */
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
