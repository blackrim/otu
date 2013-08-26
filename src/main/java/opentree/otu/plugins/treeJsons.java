package opentree.otu.plugins;

import jade.tree.*;

import opentree.otu.DatabaseManager;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.server.plugins.*;

public class treeJsons extends ServerPlugin{
	
	/**
	 * @param nodeid
	 * @return
	 */
	@Description( "Return a JSON with alternative parents presented" )
	@PluginTarget( GraphDatabaseService.class )
	public Long getRootNodeIDFromTreeID(@Source GraphDatabaseService graphDb,
			@Description( "The Neo4j tree id of the tree to be used as the root for the tree.")
			@Parameter(name = "treeID", optional = false) String treeID) {
		DatabaseManager dm = new DatabaseManager(graphDb);
		Node rootNode = dm.getRootNodeFromTreeID(treeID);
		return rootNode.getId();
	}
	
	/**
	 * @param nodeid
	 * @return
	 */
	@Description( "Return a JSON with alternative parents presented" )
	@PluginTarget( GraphDatabaseService.class )
	public String deleteTreeFromTreeID(@Source GraphDatabaseService graphDb,
			@Description( "study ID")
			@Parameter(name = "studyID", optional = false) String studyID,
			@Description( "tree ID")
			@Parameter(name = "treeID", optional = false) String treeID) {
		DatabaseManager dm = new DatabaseManager(graphDb);
		dm.deleteTreeFromTreeID(studyID, treeID);
		return "{\"worked\":1}";
	}
	
	/**
	 * @param nodeid
	 * @return
	 */
	@Description( "Return a JSON with alternative parents presented" )
	@PluginTarget( GraphDatabaseService.class )
	public Long rerootTree(@Source GraphDatabaseService graphDb,
			@Description( "The Neo4j tree id of the tree to be used as the root for the tree.")
			@Parameter(name = "nodeID", optional = false) Long nodeID) {
		DatabaseManager dm = new DatabaseManager(graphDb);
		Node rootNode = graphDb.getNodeById(nodeID);
		Node newroot = dm.rerootTree(rootNode);
		return newroot.getId();
	}
	
	/**
	 * @param nodeid
	 * @return
	 */
	@Description( "Return a JSON with alternative parents presented" )
	@PluginTarget( GraphDatabaseService.class )
	public Long ingroupSelect(@Source GraphDatabaseService graphDb,
			@Description( "The Neo4j tree id of the tree to be used as the root for the tree.")
			@Parameter(name = "nodeID", optional = false) Long nodeID) {
		DatabaseManager dm = new DatabaseManager(graphDb);
		Node rootNode = graphDb.getNodeById(nodeID);
		dm.designateIngroup(rootNode);
		return rootNode.getId();
	}
	
	/**
	 * @param nodeid
	 * @return
	 */
	@Description( "Return a JSON with alternative parents presented" )
	@PluginTarget( GraphDatabaseService.class )
	public String getTreeJson(@Source GraphDatabaseService graphDb,
			@Description( "The Neo4j node id of the node to be used as the root for the tree.")
			@Parameter(name = "nodeID", optional = false) Long nodeID) {
		DatabaseManager dm = new DatabaseManager(graphDb);
		Node rootNode = graphDb.getNodeById(nodeID);
		JadeTree t = dm.getTreeFromNode(rootNode, 100);
		return t.getRoot().getJSON(false);
	}
	
	@Description( "Get tree metadata" )
	@PluginTarget( GraphDatabaseService.class )
	public String getTreeMetaData(@Source GraphDatabaseService graphDb,
			@Description( "study ID")
			@Parameter(name = "studyID", optional = false) String studyID,
			@Description( "tree ID")
			@Parameter(name = "treeID", optional = false) String treeID) {
		DatabaseManager dm = new DatabaseManager(graphDb);
		//TODO add that the source don't exist
		String metadata = dm.getTreeMetaData(studyID, treeID);
		return metadata;
	}
	
	@Description( "Get study metadata" )
	@PluginTarget( GraphDatabaseService.class )
	public String getStudyIDFromTreeID(@Source GraphDatabaseService graphDb,
			@Description( "tree ID")
			@Parameter(name = "treeID", optional = false) String treeID) {
		DatabaseManager dm = new DatabaseManager(graphDb);
		String metadata = dm.getStudyIDFromTreeID(treeID);
		return metadata;
	}
}
