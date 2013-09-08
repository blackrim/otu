package opentree.otu.plugins;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jade.MessageLogger;
import jade.tree.*;
import opentree.otu.DatabaseBrowser;
import opentree.otu.DatabaseManager;
import opentree.otu.GraphDatabaseAgent;
import opentree.otu.exceptions.DuplicateSourceException;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.server.plugins.*;
import org.neo4j.server.rest.repr.OpentreeRepresentationConverter;
import org.neo4j.server.rest.repr.Representation;

public class sourceJsons extends ServerPlugin {

	@Description("Return JSON containing information tree ids for all local sources")
	@PluginTarget(GraphDatabaseService.class)
	public Representation getTreeIdsForAllLocalSources(@Source GraphDatabaseService graphDb,
		@Description("Tree ids to exclude from the results") @Parameter(name="excludedTreeIds", optional=true) String[] excludedTreeIdsArr){
/*		DatabaseBrowser browser = new DatabaseBrowser(graphDb);
		String sourcetreelist = browser.getJSONOfSourceIdsAndTreeIdsForImportedTrees();
		return sourcetreelist; */
		Set<String> excludedTreeIds = new HashSet<String>();
		if (excludedTreeIdsArr != null) {
			for (Object tid : excludedTreeIdsArr) {
				excludedTreeIds.add((String) tid);
			}
		}
		DatabaseBrowser browser = new DatabaseBrowser(graphDb);
		return OpentreeRepresentationConverter.convert(browser.getTreeIdsForSource(browser.LOCAL_LOCATION, "*", excludedTreeIds));
	}
	
	@Description("Return JSON containing information tree ids for the specified source")
	@PluginTarget(GraphDatabaseService.class)
	public Representation getTreeIdsForLocalSource (
			@Source GraphDatabaseService graphDb,
			@Description("The source id") @Parameter(name="sourceId", optional=false) String sourceId,
			@Description("Tree ids to exclude from the results") @Parameter(name="excludedTreeIds", optional=true) String[] excludedTreeIdsArr){
		Set<String> excludedTreeIds = new HashSet<String>();
		if (excludedTreeIdsArr != null) {
			for (Object tid : excludedTreeIdsArr) {
				excludedTreeIds.add((String) tid);
			}
		}
		DatabaseBrowser browser = new DatabaseBrowser(graphDb);
		return OpentreeRepresentationConverter.convert(browser.getTreeIdsForSource(browser.LOCAL_LOCATION, sourceId, excludedTreeIds));
	}

	@Description("Return JSON containing information about local trees")
	@PluginTarget(GraphDatabaseService.class)
	public Representation getSourceList(@Source GraphDatabaseService graphDb,
			@Description("Source ids to be excluded from the results") @Parameter(name="excludedSourceIds", optional=true) String[] excludedSourceIdsArr) {
		Set<String> excludedSourceIds = new HashSet<String>();
		if (excludedSourceIdsArr != null) {
			for (Object sid : excludedSourceIdsArr) {
				excludedSourceIds.add((String) sid);
			}
		}
		DatabaseBrowser browser = new DatabaseBrowser(graphDb);
		return OpentreeRepresentationConverter.convert(browser.getSourceIds(browser.LOCAL_LOCATION, excludedSourceIds));
	}

	/**
	 * this is a single tree version
	 * 
	 * @param nodeid
	 * @return
	 */
	@Description("Load a single newick tree into the graph")
	@PluginTarget(GraphDatabaseService.class)
	public Representation putSourceNewickSingle(
			@Source GraphDatabaseService graphDb,
			@Description("A string to be used as the source id for for this source. Source ids must be unique.") @Parameter(name = "sourceId", optional = false) String sourceId,
			@Description("A newick string containing the tree to be added.") @Parameter(name = "newickString", optional = false) String newickString) {

		Map<String, String> result = new HashMap<String, String>();
		
		GraphDatabaseAgent gdb = new GraphDatabaseAgent(graphDb);
		DatabaseManager dm = new DatabaseManager(gdb);
		NexsonSource source = new NexsonSource(sourceId);

		// ArrayList<JadeTree> trees = new ArrayList<JadeTree>();
		// JadeTree t = tr.readTree(newickString);
		// trees.add(t);

		TreeReader tr = new TreeReader();
		source.addTree(tr.readTree(newickString));

		try {
			dm.addSource(source, DatabaseManager.LOCAL_LOCATION);
		} catch (DuplicateSourceException e) {
			result.put("worked","false");
			result.put("message","a local source with id " + sourceId + " already exists in the database");
		}
	
		result.put("worked","true");
		return OpentreeRepresentationConverter.convert(result);
	}

	/**
	 * this is single or multiple trees
	 * 
	 * @param graphDb
	 * @param sourceId
	 * @param newickString
	 * @return
	 */
	@Description("Incomplete placeholder for multi-tree upload")
	@PluginTarget(GraphDatabaseService.class)
	public String putSourceNewickMultiple(
			@Source GraphDatabaseService graphDb,
			@Description("A string to be used as the source id for for this source. Source ids must be unique.")
			@Parameter(name = "sourceId", optional = false) String sourceId,
			@Description("A newick string containing the tree to be added.")
			@Parameter(name = "newickString", optional = false) String newickString) {

		return null;
	}

	@Description("Load a nexson file into the graph database")
	@PluginTarget(GraphDatabaseService.class)
	public Representation putSourceNexsonFile(
			@Source GraphDatabaseService graphDb,
			@Description("A string to be used as the source id for for this source. Source ids must be unique.")
			@Parameter(name = "sourceId", optional = false) String sourceId,
			@Description("A nexson string to be parsed")
			@Parameter(name = "nexsonString", optional = false) String nexsonString) {

		Map<String, Object> result = new HashMap<String, Object>();

		MessageLogger msgLogger = new MessageLogger("");
		StringReader sr = new StringReader(nexsonString);
		NexsonSource source = null;
		try {
			source = NexsonReader.readNexson(sr, sourceId, false, msgLogger);
		} catch (IOException e) {
			result.put("worked",false);
			result.put("message", e.toString());
		}

		DatabaseManager manager = new DatabaseManager(graphDb);
		try {
			manager.addSource(source, DatabaseManager.LOCAL_LOCATION);
		} catch (DuplicateSourceException ex) {
			result.put("worked",false);
			result.put("message","a local source with id " + sourceId + " already exists in the database");
		}

		if (result.size() < 1) {
			result.put("worked",true);
		}

		return OpentreeRepresentationConverter.convert(result);
	}

	@Description("Get source metadata")
	@PluginTarget(GraphDatabaseService.class)
	public Representation getSourceMetaData(@Source GraphDatabaseService graphDb,
			@Description("source Id")
			@Parameter(name = "sourceId", optional = false) String sourceId) {
		
		DatabaseBrowser browser = new DatabaseBrowser(graphDb);

		// look for local first since it's faster
		Node sourceMeta = browser.getSourceMetaNode(sourceId, DatabaseBrowser.LOCAL_LOCATION);

		// if we didn't find a local source, check remotes
		if (sourceMeta == null) {
			List<Node> sourceMetasRemote = browser.getRemoteSourceMetaNodesForSourceId(sourceId);
			if (sourceMetasRemote.size() > 0) {
				sourceMeta = sourceMetasRemote.get(0);
			}
		}
		
//		String metadata = DatabaseBrowser.getMetadataJSONForSource(sourceMeta);
//		return metadata;
		
		return OpentreeRepresentationConverter.convert(sourceMeta == null ? null : browser.getMetadataForSource(sourceMeta));
	}

	/**
	 * Delete a tree from the db
	 * 
	 * @param nodeid
	 * @return
	 */
	@Description("Return a JSON with alternative parents presented")
	@PluginTarget(GraphDatabaseService.class)
	public Representation deleteSourceFromSourceId(@Source GraphDatabaseService graphDb,
			@Description("source Id") @Parameter(name = "sourceId", optional = false) String sourceId) {

		DatabaseManager dm = new DatabaseManager(graphDb);
		DatabaseBrowser browser = new DatabaseBrowser(graphDb);
		Node sourceMeta = browser.getSourceMetaNode(sourceId, DatabaseBrowser.LOCAL_LOCATION);

		dm.deleteSource(sourceMeta);

		Map<String, Object> result = new HashMap<String, Object>();
		result.put("worked", true);
		return OpentreeRepresentationConverter.convert(result);
	}

}
