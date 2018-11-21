package com.fedex.smartpost.utilities;

import java.io.BufferedWriter;
import java.io.Console;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.fedex.smartpost.utilities.remote.RemoteService;
import com.fedex.smartpost.utilities.remote.RemoteServiceImpl;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LogCheck {
	private static final Log log = LogFactory.getLog(LogCheck.class);
	private RemoteService remoteService = new RemoteServiceImpl();

	private Map<String, List<String>> setup() {
		Map<String, List<String>> servers = new HashMap<>();
		List<String> serverList = new ArrayList<>();
		for (int i = 2; i < 10; i++) {
			serverList.add(String.format("pje5615%d.ground.fedex.com", i));
		}
		servers.put("sptl", serverList);
		serverList = new ArrayList<>();
		for (int i = 87; i < 91; i++) {
			serverList.add(String.format("pje225%d.ground.fedex.com", i));
		}
		servers.put("sshp", serverList);
		serverList = new ArrayList<>();
		for (int i = 0; i < 8; i++) {
			serverList.add(String.format("pje0353%d.ground.fedex.com", i));
		}
		servers.put("srtg", serverList);
		return servers;
	}

	private void process(String username, String password) throws IOException, SftpException, JSchException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-dd-MM_HHmmss");
		BufferedWriter writer = new BufferedWriter(new FileWriter(String.format("/CheckLog-%s.txt", sdf.format(new Date()))));
		Map<String, List<String>> servers = setup();
		for (String domain : servers.keySet()) {
			for (String server : servers.get(domain)) {
				if (!readLog(domain, server, username, password, writer)) {
					writer.write("Unable to log into " + server + "\n");
					log.error("Unable to log into " + server);
				}
			}
		}
		writer.close();
	}

	private boolean readLog(String domain, String server, String username, String password, BufferedWriter writer) throws IOException, JSchException, SftpException {
		String pathRoot = String.format("/var/fedex/%s/logs/%s", domain, domain);
		Session session = remoteService.login(username, password, server);
		if (session == null) {
			return false;
		}
		log.info("Logged into " + server);
		List<String> fileList = remoteService.fileList(session, pathRoot);
		log.info(fileList.size() + " directories found.");
		for (String root : fileList) {
			Map<String, Integer> lastSeen = remoteService.grepFileScript(session, root);
			if (lastSeen.size() > 0) {
				boolean alreadyWrittenTo = false;
				writer.write(server + ":" + root + " - (");
				for (String level : lastSeen.keySet()) {
					if (alreadyWrittenTo) {
						writer.write(String.format(", %s: %d", level, lastSeen.get(level)));
					}
					else {
						writer.write(String.format("%s: %d", level, lastSeen.get(level)));
						alreadyWrittenTo = true;
					}
				}
				writer.write(")\n");
			}
		}
		session.disconnect();
		return true;
	}

	public static void main(String[] args) throws IOException, JSchException, SftpException {
		String username;
		String password;

		log.info("Staring LogCheck...");
		if (args.length == 2) {
			username = args[0];
			password = args[1];
		}
		else {
			Console console = System.console();
			username = console.readLine("[%s] ", "Username:");
			password = new String(console.readPassword("[%s] ", "Password:"));
		}
		new LogCheck().process(username, password);
		log.info("LogCheck Completed...");
	}
}
