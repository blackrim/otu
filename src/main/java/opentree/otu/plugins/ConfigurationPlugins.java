package opentree.otu.plugins;

import opentree.otu.ConfigurationManager;
import opentree.otu.DatabaseManager;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.server.plugins.Description;
import org.neo4j.server.plugins.Parameter;
import org.neo4j.server.plugins.PluginTarget;
import org.neo4j.server.plugins.ServerPlugin;
import org.neo4j.server.plugins.Source;

public class ConfigurationPlugins extends ServerPlugin {

	@Description( "" )
	@PluginTarget( GraphDatabaseService.class )
	public String getNexsonGitDir(@Source GraphDatabaseService graphDb) {
		DatabaseManager dm = new DatabaseManager(graphDb);
		ConfigurationManager cm = new ConfigurationManager(dm);
		String dir = cm.getNexsonGitDir();
		String retstr = "{\"nexsongitdir\":\""+dir+"\"}";
		return retstr;
	}
	
	@Description( "" )
	@PluginTarget( GraphDatabaseService.class )
	public boolean setNexsonGitDir(@Source GraphDatabaseService graphDb,
			@Description( "Nexson Git Directory String")
			@Parameter(name = "nexsongitdir", optional = false) String dir) {
		DatabaseManager dm = new DatabaseManager(graphDb);
		ConfigurationManager cm = new ConfigurationManager(dm);
		boolean success = cm.setNexsonGitDir(dir);
		return success;
	}
}
