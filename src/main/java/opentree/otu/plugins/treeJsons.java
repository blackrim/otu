package opentree.otu.plugins;

import jade.tree.*;
import opentree.otu.DatabaseManager;
import opentree.otu.exceptions.NoSuchTreeException;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.server.plugins.*;

public class treeJsons extends ServerPlugin{
	
	/**
	 * @param nodeid
	 * @return
	 * @throws NoSuchTreeException 
	 */
	@Description( "Get the neo4j root node for a given OT tree id" )
	@PluginTarget( GraphDatabaseService.class )
	public Long getRootNodeIDFromTreeID(@Source GraphDatabaseService graphDb,
			@Description( "The OT tree id of the tree to be found.")
			@Parameter(name = "treeID", optional = false) String treeID) throws NoSuchTreeException {
		DatabaseManager dm = new DatabaseManager(graphDb);
		// TODO: add check for whether tree is imported. If not then return this information
		Node rootNode = dm.getRootNodeFromTreeIDValidated(treeID);
		return rootNode.getId();
	}
	
	/**
	 * @param nodeid
	 * @return
	 */
	@Description( "Remove a previously imported tree from the graph" )
	@PluginTarget( GraphDatabaseService.class )
	public String deleteTreeFromTreeID(@Source GraphDatabaseService graphDb,
			@Description( "study ID")
			@Parameter(name = "studyID", optional = false) String studyID,
			@Description( "tree ID")
			@Parameter(name = "treeID", optional = false) String treeID) {
		DatabaseManager dm = new DatabaseManager(graphDb);
		// TODO: add check for whether tree is imported. If not then return error
		dm.deleteTreeFromTreeID(studyID, treeID);
		return "{\"worked\":1}";
	}
	
	/**
	 * @param nodeid
	 * @return
	 */
	@Description( "Reroot the tree containing the indicated node, using that node as the new root. Returns the neo4j node id of the new root." )
	@PluginTarget( GraphDatabaseService.class )
	public Long rerootTree(@Source GraphDatabaseService graphDb,
			@Description( "The Neo4j node id of the node to be used as the root for its tree.")
			@Parameter(name = "nodeID", optional = false) Long nodeID) {
		DatabaseManager dm = new DatabaseManager(graphDb);
		// TODO: add check for whether tree is imported. If not then return error
		Node rootNode = graphDb.getNodeById(nodeID);
		Node newroot = dm.rerootTree(rootNode);
		return newroot.getId();
	}
	
	/**
	 * @param nodeid
	 * @return
	 */
	@Description( "Set the ingroup of the tree containing the indicated node to that node." )
	@PluginTarget( GraphDatabaseService.class )
	public Long ingroupSelect(@Source GraphDatabaseService graphDb,
			@Description( "The Neo4j node id of the node to be used as the ingroup for its tree.")
			@Parameter(name = "nodeID", optional = false) Long nodeID) {
		DatabaseManager dm = new DatabaseManager(graphDb);
		// TODO: add check for whether tree is imported. If not then return error
		Node rootNode = graphDb.getNodeById(nodeID);
		dm.designateIngroup(rootNode);
		return rootNode.getId();
	}
	
	/**
	 * @param nodeid
	 * @return
	 */
	@Description( "Return a tree in JSON format, starting from the indicated tree node" )
	@PluginTarget( GraphDatabaseService.class )
	public String getTreeJson(@Source GraphDatabaseService graphDb,
			@Description( "The Neo4j node id of the node to be used as the root for the tree (can be used to extract subtrees as well).")
			@Parameter(name = "nodeID", optional = false) Long nodeID) {
		DatabaseManager dm = new DatabaseManager(graphDb);
		// TODO: add check for whether tree is imported. If not then return error instead of just empty tree
		Node rootNode = graphDb.getNodeById(nodeID);
		JadeTree t = dm.getTreeFromNode(rootNode, 100);
		return t.getRoot().getJSON(false);
	}
	
	@Description( "Get tree metadata" )
	@PluginTarget( GraphDatabaseService.class )
	public String getTreeMetaData(@Source GraphDatabaseService graphDb,
			@Description( "study ID") //  should we be using "source ID" for consistency?
			@Parameter(name = "studyID", optional = false) String studyID,
			@Description( "tree ID")
			@Parameter(name = "treeID", optional = false) String treeID) {
		DatabaseManager dm = new DatabaseManager(graphDb);
		// TODO: add that the source don't exist
		// TODO: add check for whether source is imported, include that info in the returned JSON
		String metadata = dm.getTreeMetaData(studyID, treeID);
		return metadata;
	}
	
	@Description( "Get study metadata" ) // should we calling this "get source metadata" for consistency?
	@PluginTarget( GraphDatabaseService.class )
	public String getStudyIDFromTreeID(@Source GraphDatabaseService graphDb, // shouldn't this be called "getStudyMetadataForTreeID"?
			@Description( "tree ID")
			@Parameter(name = "treeID", optional = false) String treeID) {
		// TODO: add check for whether source is imported, include that info in the returned JSON
		DatabaseManager dm = new DatabaseManager(graphDb);
		String metadata = dm.getStudyIDFromTreeID(treeID);
		return metadata;
	}
}
