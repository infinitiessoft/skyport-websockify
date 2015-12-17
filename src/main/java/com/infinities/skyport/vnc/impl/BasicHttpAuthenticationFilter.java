package com.infinities.skyport.vnc.impl;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.AUTHORIZATION;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.HOST;

import java.io.Serializable;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.codec.Base64;
import org.apache.shiro.subject.Subject;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicHttpAuthenticationFilter implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(BasicHttpAuthenticationFilter.class);


	public boolean executeLogin(HttpRequest request) throws Exception {
		AuthenticationToken token = createToken(request);
		if (token == null) {
			String msg =
					"createToken method implementation returned null. A valid non-null AuthenticationToken "
							+ "must be created in order to execute a login attempt.";
			throw new IllegalStateException(msg);
		}
		try {
			Subject subject = getSubject();
			subject.login(token);
			return onLoginSuccess();
		} catch (AuthenticationException e) {
			return onLoginFailure();
		}
	}

	protected boolean onLoginSuccess() throws Exception {
		logger.debug("login success");
		return true;
	}

	protected boolean onLoginFailure() {
		logger.debug("login fail");
		return false;
	}

	protected String getAuthzHeader(HttpRequest request) {
		return request.headers().get(AUTHORIZATION);
	}

	protected AuthenticationToken createToken(HttpRequest request) {
		String authorizationHeader = getAuthzHeader(request);
		if (authorizationHeader == null || authorizationHeader.length() == 0) {
			// Create an empty authentication token since there is no
			// Authorization header.
			return createToken("", "", request);
		}

		logger.debug("Attempting to execute login with headers [" + authorizationHeader + "]");

		String[] prinCred = getPrincipalsAndCredentials(authorizationHeader);
		if (prinCred == null || prinCred.length < 2) {
			// Create an authentication token with an empty password,
			// since one hasn't been provided in the request.
			String username = prinCred == null || prinCred.length == 0 ? "" : prinCred[0];
			return createToken(username, "", request);
		}

		String username = prinCred[0];
		String password = prinCred[1];

		return createToken(username, password, request);
	}

	protected AuthenticationToken createToken(String username, String password, HttpRequest request) {
		boolean rememberMe = isRememberMe();
		String host = getHost(request);
		return createToken(username, password, rememberMe, host);
	}

	protected AuthenticationToken createToken(String username, String password, boolean rememberMe, String host) {
		return new UsernamePasswordToken(username, password, rememberMe, host);
	}

	protected String getHost(HttpRequest request) {
		return request.headers().get(HOST);
	}

	protected boolean isRememberMe() {
		return false;
	}

	protected String[] getPrincipalsAndCredentials(String authorizationHeader) {
		if (authorizationHeader == null) {
			return null;
		}
		String[] authTokens = authorizationHeader.split(" ");
		if (authTokens == null || authTokens.length < 2) {
			return null;
		}
		return getPrincipalsAndCredentials(authTokens[0], authTokens[1]);
	}

	protected String[] getPrincipalsAndCredentials(String scheme, String encoded) {
		String decoded = Base64.decodeToString(encoded);
		return decoded.split(":");
	}

	protected Subject getSubject() {
		return SecurityUtils.getSubject();
	}

}
