package com.infinities.skyport.vnc.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.SessionException;
import org.apache.shiro.session.mgt.SessionContext;
import org.apache.shiro.session.mgt.SessionKey;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.SubjectContext;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.util.HashedWheelTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.infinities.skyport.model.console.ConsoleType;
import com.infinities.skyport.model.console.ServerConfiguration;
import com.infinities.skyport.model.console.ServerConfiguration.Mode;
import com.infinities.skyport.model.console.WebsockifyProfile;
import com.infinities.skyport.vnc.bootstrap.BootstrapBuilder;
import com.infinities.skyport.vnc.bootstrap.ConsoleBootstrap;
import com.infinities.skyport.vnc.handler.SkyportIdleStateAwareHandler;
import com.infinities.skyport.websockify.SSLSetting;
import com.infinities.skyport.websockify.WebsockifyServer;
import com.netiq.websockify.StaticTargetResolver;
import com.netiq.websockify.WebsockifyProxyPipelineFactory;

public class LocalWebsockifyServer implements WebsockifyServer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ExecutorService executor;
	private ChannelFactory channelFactory;
	private ClientSocketChannelFactory cf;
	private static final Logger logger = LoggerFactory.getLogger(LocalWebsockifyServer.class);


	// private ChannelGroup channelGroup;

	public LocalWebsockifyServer() {

	}

	@Override
	public Channel connect(WebsockifyProfile profile, SSLSetting sslSetting, ServerConfiguration configuration,
			Object securityManager) {
		ServerBootstrap sb = new ServerBootstrap(channelFactory);
		HashedWheelTimer idleTimer = new HashedWheelTimer();
		// Here is where we handle accept timeouts
		// sb.setParentHandler(new SkyportIdleStateHandler(idleTimer, 0, 0,
		// 120));
		// Here is where we handle read/write timeouts on child channels
		sb.getPipeline().addLast("idleHandler", new IdleStateHandler(idleTimer, 60, 60, 60));
		sb.getPipeline().addLast("stateawarehandler", new SkyportIdleStateAwareHandler());
		// byteBuffer use to put 16 byte challenge
		// sb.getPipeline().addLast("passwordhandler",
		// new PasswordHandler(byteBuffer, profile.getToken(),
		// profile.getPassword()));

		ConsoleBootstrap bootstrap = BootstrapBuilder.createInstance(profile, cf);
		sb.setPipelineFactory(new WebsockifyProxyPipelineFactory(cf, new StaticTargetResolver(profile.getTargetHost(),
				profile.getTargetPort()), bootstrap, sslSetting, configuration,
				(org.apache.shiro.mgt.SecurityManager) securityManager));
		// Start up the channel.
		Channel serverChannel = sb.bind(new InetSocketAddress(profile.getSourcePort()));
		bootstrap.setWebsockifyServer(this);
		bootstrap.setServerChannel(serverChannel);
		// channelGroup.add(serverChannel);

		return serverChannel;
	}

	@Override
	public void close(Channel serverChannel) {
		if (serverChannel != null && serverChannel.isBound()) {
			serverChannel.close();
			// channelGroup.remove(serverChannel);
			serverChannel = null;
			logger.debug("close channel");
		}
	}

	@Override
	public void activate() {
		// channelGroup = new
		// DefaultChannelGroup(SkyportWebsockifyServer.class.getName());
		executor = Executors.newCachedThreadPool();
		channelFactory = new NioServerSocketChannelFactory(executor, executor);
		cf = new NioClientSocketChannelFactory(executor, executor);
	}

	@Override
	public void deactivate() {
		// channelGroup.close().awaitUninterruptibly();
		cf.shutdown();
		channelFactory.releaseExternalResources();
		channelFactory.shutdown();
		executor.shutdown();
	}

	public static void main(String args[]) throws InterruptedException, IOException {
		WebsockifyServer websockifyServer = new LocalWebsockifyServer();
		websockifyServer.activate();

		// websockify server的設定
		ServerConfiguration configuration = new ServerConfiguration();
		configuration.setMode(Mode.local);
		configuration.setProtocol("HTTP/1.1");
		configuration.setIp("127.0.0.1");// url ip(看你web server使用哪個ip)
		configuration.setPort("8085");// url port(看你web server使用哪個port)
		configuration.setPath("/index.html"); // *和要呈現的頁面url有關
		configuration.setEnableSSL(false);// web server是不是使用https?

		WebsockifyProfile profile = new WebsockifyProfile();
//ssh
//		profile.setTargetHost("54.165.34.176"); // vnc or ssh server ip
//		profile.setTargetPort(22); // vnc or ssh server port
//		profile.setInternalSSL(false); // websocket是不是使用ssl的 wss:// or ws://
//		String token = UUID.randomUUID().toString();
//		System.err.println(token);
//		profile.setUsername("ec2-user");
//		String fileLocation = "keypairs" + File.separator + "sshDemo.pem";
//		logger.info("pem loaded from: {}", fileLocation);
//		String key = BaseEncoding.base64().encode(Files.asByteSource(new File(fileLocation)).read());
//		
//		profile.setKey(key);
//		profile.setToken(token); // 給使用者看的token
//		profile.setPassword("vagrant"); // vnc or ssh server的真實密碼
//		// token和vnc or ssh server密碼會在websockify server交換
//		profile.setConsoleType(ConsoleType.VPSSH); // console type vnc or ssh
//rdp
		profile.setTargetHost("192.168.9.106"); // vnc or ssh server ip
		profile.setTargetPort(3389); // vnc or ssh server port
		profile.setInternalSSL(false); // websocket是不是使用ssl的 wss:// or ws://
		String token = UUID.randomUUID().toString();
		System.err.println(token);
		profile.setUsername("pohsun");
		profile.setToken(token); // 給使用者看的token
		profile.setPassword("2ggudoou");
//		profile.setPassword("3m>n^$[U4xj@MQd"); // vnc or ssh server的真實密碼
		// token和vnc or ssh server密碼會在websockify server交換
		profile.setConsoleType(ConsoleType.RDP); // console type vnc or ssh
		profile.setSourcePort(10900); // 選一個port號當作websocket port

		Channel channel = websockifyServer.connect(profile, SSLSetting.OFF, configuration,
				new org.apache.shiro.mgt.SecurityManager() {

					@Override
					public AuthenticationInfo authenticate(AuthenticationToken authenticationToken)
							throws AuthenticationException {
						return null;
					}

					@Override
					public boolean isPermitted(PrincipalCollection principals, String permission) {

						return false;
					}

					@Override
					public boolean isPermitted(PrincipalCollection subjectPrincipal, Permission permission) {

						return false;
					}

					@Override
					public boolean[] isPermitted(PrincipalCollection subjectPrincipal, String... permissions) {

						return null;
					}

					@Override
					public boolean[] isPermitted(PrincipalCollection subjectPrincipal, List<Permission> permissions) {

						return null;
					}

					@Override
					public boolean isPermittedAll(PrincipalCollection subjectPrincipal, String... permissions) {

						return false;
					}

					@Override
					public boolean isPermittedAll(PrincipalCollection subjectPrincipal, Collection<Permission> permissions) {

						return false;
					}

					@Override
					public void checkPermission(PrincipalCollection subjectPrincipal, String permission)
							throws AuthorizationException {

					}

					@Override
					public void checkPermission(PrincipalCollection subjectPrincipal, Permission permission)
							throws AuthorizationException {

					}

					@Override
					public void checkPermissions(PrincipalCollection subjectPrincipal, String... permissions)
							throws AuthorizationException {

					}

					@Override
					public void checkPermissions(PrincipalCollection subjectPrincipal, Collection<Permission> permissions)
							throws AuthorizationException {

					}

					@Override
					public boolean hasRole(PrincipalCollection subjectPrincipal, String roleIdentifier) {

						return false;
					}

					@Override
					public boolean[] hasRoles(PrincipalCollection subjectPrincipal, List<String> roleIdentifiers) {

						return null;
					}

					@Override
					public boolean hasAllRoles(PrincipalCollection subjectPrincipal, Collection<String> roleIdentifiers) {

						return false;
					}

					@Override
					public void checkRole(PrincipalCollection subjectPrincipal, String roleIdentifier)
							throws AuthorizationException {

					}

					@Override
					public void checkRoles(PrincipalCollection subjectPrincipal, Collection<String> roleIdentifiers)
							throws AuthorizationException {

					}

					@Override
					public void checkRoles(PrincipalCollection subjectPrincipal, String... roleIdentifiers)
							throws AuthorizationException {

					}

					@Override
					public Session start(SessionContext context) {

						return null;
					}

					@Override
					public Session getSession(SessionKey key) throws SessionException {

						return null;
					}

					@Override
					public Subject login(Subject subject, AuthenticationToken authenticationToken)
							throws AuthenticationException {

						return null;
					}

					@Override
					public void logout(Subject subject) {

					}

					@Override
					public Subject createSubject(SubjectContext context) {

						return null;
					}
				});

		// 可以搭配 http://kanaka.github.io/noVNC/noVNC/vnc.html 使用
		// 輸入連線資訊host:127.0.0.1, port:10900, password:[token]

		System.out.print("Enter to leave \n");
		int ch;
		while ((ch = System.in.read()) != '\n')
			System.out.print((char) ch);
		websockifyServer.close(channel);
		websockifyServer.deactivate();
		System.err.println("close");
	}

}
