package gov.usdot.cv.websocket.mongo;

import gov.usdot.cv.common.util.Syslogger;
import gov.usdot.cv.websocket.server.WebSocketEventListener;
import gov.usdot.cv.websocket.server.WebSocketServer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.log4j.Logger;

public class MongoQueryEventListener implements WebSocketEventListener {

	private final static String SYS_LOG_ID = "WebSocket QueryProcessor";
	
	private static final Logger logger = Logger.getLogger(MongoQueryEventListener.class
			.getName());
	private final static String QUERY_TAG = "QUERY:";
	private final static String SYSTEM_NAME = "systemQueryName";
	
	private List<MongoConfig> mongoConfigs;
	private Map<String, MongoQueryRunner> queryRunnerMap = new HashMap<String, MongoQueryRunner>();
	
	public MongoQueryEventListener(List<MongoConfig> mongoConfigs) {
		this.mongoConfigs = mongoConfigs;
	}
	
	public void connect() {
		for (MongoConfig config: mongoConfigs) {
			MongoQueryRunner queryRunner = new MongoQueryRunner(config);
			queryRunner.connect();
			queryRunnerMap.put(config.systemName, queryRunner);
		}
	}
	
	public void close() {
		for (MongoQueryRunner queryRunner: queryRunnerMap.values()) {
			queryRunner.close();
		}
		queryRunnerMap.clear();
	}
	
	public void onMessage(String websocketID, String message) {
		if (message.startsWith(QUERY_TAG)) {
			logger.info("Received query message " + message + " from websocket " + websocketID);
			message = message.substring(QUERY_TAG.length()).trim();
			JSONObject json = (JSONObject)JSONSerializer.toJSON(message);
			if (json.containsKey(SYSTEM_NAME)) {
				String systemName = json.getString(SYSTEM_NAME);
				if (queryRunnerMap.containsKey(systemName)) {
					queryRunnerMap.get(systemName).runQuery(websocketID, json, message);
					Syslogger.getInstance().log(SYS_LOG_ID, 
						String.format("Processed query for websocketID %s, query %s", websocketID, json.toString()));
				} else {
					String errorMsg = "Invalid systemQueryName: " + systemName + 
							", not one of the supported systemQueryName: " + queryRunnerMap.keySet().toString();
					logger.error(errorMsg);
					WebSocketServer.sendMessage(websocketID, "ERROR: " + errorMsg);
				}
			} else {
				String errorMsg = "Query message missing required systemQueryName field";
				logger.error(errorMsg);
				WebSocketServer.sendMessage(websocketID, "ERROR: " + errorMsg);
			}
		}
	}

	public void onOpen(String websocketID) {
		// do nothing
	}

	public void onClose(String websocketID) {
		for (MongoQueryRunner queryRunner: queryRunnerMap.values()) {
			queryRunner.killRunningQueries(websocketID);
		}
	}
}
