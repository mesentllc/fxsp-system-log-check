package com.fedex.smartpost.utilities.remote;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.Vector;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RemoteServiceImpl implements RemoteService {
	private static final Log log = LogFactory.getLog(RemoteService.class);

	@Override
	public Session login(String username, String password, String host) {
		Session session;
		Properties config = new Properties();
		config.put("StrictHostKeyChecking", "no");
		JSch jSch = new JSch();
		try {
			session = jSch.getSession(username, host, 22);
			session.setPassword(password);
			session.setConfig(config);
			session.connect();
			return session;
		}
		catch (JSchException e) {
			log.error("Exception Caught", e);
			return null;
		}
	}

	@Override
	public List<String> fileList(Session session, String root) throws JSchException, SftpException {
		List<String> fileList = new ArrayList<>();
		ChannelSftp channel;
		if (session == null) {
			return null;
		}
		channel = (ChannelSftp)session.openChannel("sftp");
		channel.connect();
		channel.cd(root);
		Vector lsList = channel.ls(root);
		channel.disconnect();
		for (Object lsEntry : lsList) {
			String directory = ((ChannelSftp.LsEntry)lsEntry).getFilename();
			if (!directory.startsWith(".")) {
				fileList.add(root + "/" + directory);
			}
		}
		return fileList;
	}

	@Override
	public Map<String, Integer> grepFileScript(Session session, String path) throws JSchException {
		Map<String, Integer> lastSeen = new TreeMap<>();
		String[] levels = {"WARN", "ERROR", "FATAL"};
		for (String level : levels) {
			String output = grepLog(session, path, level);
			if (output != null && output.trim().length() > 0) {
				String[] lines = output.split("\n");
				lastSeen.put(level, lines.length);
			}
		}
		return lastSeen;
	}

	private String grepLog(Session session, String path, String level) throws JSchException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		String dateToLookFor = sdf.format(new Date());
		ChannelExec channel;
		int delay = 0;

		if (session == null) {
			return null;
		}
		String applicationName = path.substring(path.lastIndexOf("/") + 1);
		if (applicationName.startsWith("fxsp-")) {
			channel = (ChannelExec)session.openChannel("exec");
			channel.setCommand("grep " + level + " " + path + "/*" + applicationName + ".log | grep " + dateToLookFor);
			channel.setInputStream(null);
			channel.setOutputStream(outputStream);
			channel.setErrStream(System.err);
			channel.connect();
			while (!channel.isClosed()) {
				if (++delay == 0) {
					log.info("Waiting for grep script to complete...");
				}
			}
			channel.disconnect();
		}
		return outputStream.toString();
	}
}
