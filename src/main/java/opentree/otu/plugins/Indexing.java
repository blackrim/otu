package opentree.otu.plugins;

import jade.MessageLogger;
import jade.tree.JadeTree;
import jade.tree.NexsonReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

import opentree.otu.DatabaseIndexer;
import opentree.otu.DatabaseManager;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.server.plugins.Description;
import org.neo4j.server.plugins.Parameter;
import org.neo4j.server.plugins.PluginTarget;
import org.neo4j.server.plugins.ServerPlugin;
import org.neo4j.server.plugins.Source;
import org.neo4j.server.rest.repr.ListRepresentation;
import org.neo4j.server.rest.repr.Representation;
import org.neo4j.server.rest.repr.RepresentationFormat;
import org.neo4j.server.rest.repr.ValueRepresentation;

import scala.actors.threadpool.Arrays;

/**
 * services for indexing. very preliminary, should probably be reorganized (later).
 * @author cody
 *
 */
public class Indexing extends ServerPlugin {
	
	@Description( "Perform indexing of public nexsons repo" )
	@PluginTarget( GraphDatabaseService.class )
	public Representation indexAllPublicStudies(@Source GraphDatabaseService graphDb) throws InterruptedException, IOException, ParseException {
		Thread.sleep(10000);

        JSONParser parser = new JSONParser();

		// get the commits from the public repo
		String nexsonCommitsURLStr = "https://bitbucket.org/api/2.0/repositories/blackrim/avatol_nexsons/commits";
//		URL nexsonCommitsURL = new URL(nexsonCommitsURLStr);
        BufferedReader nexsonCommits = new BufferedReader(new InputStreamReader(new URL(nexsonCommitsURLStr).openStream()));
        JSONObject commitsJSON = (JSONObject) parser.parse(nexsonCommits); 

        // get just the most recent commit
//	    recenthash = cjson["values"][0]["hash"] // example python code
        String mostRecentCommitHash = (String) ((JSONObject) ((JSONArray) commitsJSON.get("values")).get(0)).get("hash");

        String nexsonsDirURL = "https://bitbucket.org/api/1.0/repositories/blackrim/avatol_nexsons/raw/"+mostRecentCommitHash+"/";
//		URL nexsonsURL = new URL(currentNexsonsURL);
        BufferedReader nexsonsDir = new BufferedReader(new InputStreamReader(new URL(nexsonsDirURL).openStream()));

//        return ValueRepresentation.string(nexsonsReader.readLine());

//      /*        

//        JSONArray nexsons = (JSONArray) parser.parse(nexsonsReader);

        DatabaseIndexer di = new DatabaseIndexer(graphDb);
        LinkedList<String> indexedStudies = new LinkedList<String>(); // just for testing really
        LinkedList<String> errorStudies = new LinkedList<String>();

        // for each nexson in the latest commit
        String dirEntry = "";
        while (dirEntry != null) {
        	dirEntry = nexsonsDir.readLine();
        	try {
        		Integer.valueOf(dirEntry);
        	} catch (NumberFormatException ex) {
        		errorStudies.add(dirEntry);
        		continue;
        	}
			
            String curNexsonURL = nexsonsDirURL + dirEntry;
            BufferedReader nexson = new BufferedReader(new InputStreamReader(new URL(curNexsonURL).openStream()));

			MessageLogger msgLogger = new MessageLogger("");
			List<JadeTree> trees = null;
			trees = NexsonReader.readNexson(nexson, false, msgLogger);
			String studyId = String.valueOf(trees.get(0).getObject("ot:studyId"));
			
			di.indexStudy(trees, studyId);
			indexedStudies.add(studyId); // remember studies we saw; useful for testing
        }

        return ListRepresentation.string(indexedStudies);
        
//        */
	}
}
