package com.fedex.smartpost.utilities.remote;

import java.util.List;
import java.util.Map;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public interface RemoteService {
	Session login(String username, String password, String host);
	List<String> fileList(Session session, String root) throws JSchException, SftpException;
	Map<String, Integer> grepFileScript(Session session, String path) throws JSchException;
}
