package opentree.otu.plugins;

import jade.tree.*;
import opentree.otu.DatabaseBrowser;
import opentree.otu.DatabaseManager;
import opentree.otu.constants.NodeProperty;
import opentree.otu.constants.RelType;
import opentree.otu.exceptions.NoSuchTreeException;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.server.plugins.*;
import org.neo4j.server.rest.repr.OpentreeRepresentationConverter;
import org.neo4j.server.rest.repr.Representation;

public class treeJsons extends ServerPlugin{
	
	/**
	 * @param nodeId
	 * @return
	 * @throws NoSuchTreeException 
	 */
	@Description( "Get the neo4j root node for a given tree id" )
	@PluginTarget( GraphDatabaseService.class )
	public Long getRootNodeIdForTreeId(@Source GraphDatabaseService graphDb,
			@Description( "The id of the tree to be found.")
			@Parameter(name = "treeId", optional = false) String treeId) throws NoSuchTreeException {

		DatabaseBrowser browser = new DatabaseBrowser(graphDb);

		// TODO: add check for whether tree is imported. If not then return this information
		
		Node rootNode = browser.getTreeRootNode(treeId, browser.LOCAL_LOCATION);
		return rootNode.getId();
	}
	
	/**
	 * @param nodeId
	 * @return
	 */
	@Description( "Remove a previously imported tree from the graph" )
	@PluginTarget( GraphDatabaseService.class )
	public String deleteTreeFromTreeId(@Source GraphDatabaseService graphDb,
//			@Description( "study ID")
//			@Parameter(name = "studyID", optional = false) String studyID,
			@Description( "The id of the tree to be deleted")
			@Parameter(name = "treeId", optional = false) String treeId) {
		
		DatabaseManager manager = new DatabaseManager(graphDb);
		DatabaseBrowser browser = new DatabaseBrowser(graphDb);
		
//		manager.deleteLocalTreeFromTreeID(studyID, treeID);
		Node root = browser.getTreeRootNode(treeId, browser.LOCAL_LOCATION);
		manager.deleteTree(root);
		return "{\"worked\":1}";
	}
	
	/**
	 * @param nodeId
	 * @return
	 */
	@Description( "Reroot the tree containing the indicated node, using that node as the new root. Returns the neo4j node id of the new root." )
	@PluginTarget( GraphDatabaseService.class )
	public Long rerootTree(@Source GraphDatabaseService graphDb,
			@Description( "The Neo4j node id of the node to be used as the root for its tree.")
			@Parameter(name = "nodeId", optional = false) Long nodeId) {
		DatabaseManager manager = new DatabaseManager(graphDb);
		Node rootNode = graphDb.getNodeById(nodeId);
		Node newroot = manager.rerootTree(rootNode);
		return newroot.getId();
	}
	
	/**
	 * @param nodeId
	 * @return
	 */
	@Description( "Set the ingroup of the tree containing the indicated node to that node." )
	@PluginTarget( GraphDatabaseService.class )
	public Long ingroupSelect(@Source GraphDatabaseService graphDb,
			@Description( "The Neo4j node id of the node to be used as the ingroup for its tree.")
			@Parameter(name = "nodeId", optional = false) Long nodeId) {
		DatabaseManager manager = new DatabaseManager(graphDb);
		Node rootNode = graphDb.getNodeById(nodeId);
		manager.designateIngroup(rootNode);
		return rootNode.getId();
	}
	
	/**
	 * @param nodeId
	 * @return
	 */
	@Description( "Return a tree in JSON format, starting from the indicated tree node" )
	@PluginTarget( GraphDatabaseService.class )
	public String getTreeJson(@Source GraphDatabaseService graphDb,
			@Description( "The Neo4j node id of the node to be used as the root for the tree (can be used to extract subtrees as well).")
			@Parameter(name = "nodeId", optional = false) Long nodeId) {
//		DatabaseBrowser browser = new DatabaseBrowser(graphDb);

		// TODO: add check for whether tree is imported. If not then return error instead of just empty tree
		Node rootNode = graphDb.getNodeById(nodeId);
		JadeTree t = DatabaseBrowser.getTreeFromNode(rootNode, 100);

		return t.getRoot().getJSON(false);
	}
	
	@Description( "Get tree metadata" )
	@PluginTarget( GraphDatabaseService.class )
	public Representation getTreeMetaData(@Source GraphDatabaseService graphDb,
//			@Description( "study ID") //  should we be using "source ID" for consistency?
//			@Parameter(name = "studyID", optional = false) String studyID,
			@Description( "The database tree id for the tree")
			@Parameter(name = "treeId", optional = false) String treeId) {
		
		DatabaseBrowser browser = new DatabaseBrowser(graphDb);

		// TODO: add that the source don't exist // not sure what this means...

		Node root = browser.getTreeRootNode(treeId, browser.LOCAL_LOCATION);
//		String metadata = browser.getMetadataForTree(root);
		return OpentreeRepresentationConverter.convert(browser.getMetadataForTree(root));
	}
	
	@Description( "Get the id for the source associated with the specified tree id" )
	@PluginTarget( GraphDatabaseService.class )
	public String getSourceIdForTreeId(@Source GraphDatabaseService graphDb,
			@Description( "The tree id to use")
			@Parameter(name = "treeId", optional = false) String treeId) {
		
//		String metadata = manager.getStudyIDFromTreeID(treeId);

		DatabaseBrowser browser = new DatabaseBrowser(graphDb);

		Node treeRoot = browser.getTreeRootNode(treeId, browser.LOCAL_LOCATION);
		Node sourceMeta = treeRoot.getSingleRelationship(RelType.METADATAFOR, Direction.INCOMING).getStartNode();
		return (String) sourceMeta.getProperty(NodeProperty.SOURCE_ID.name);
	}
}
