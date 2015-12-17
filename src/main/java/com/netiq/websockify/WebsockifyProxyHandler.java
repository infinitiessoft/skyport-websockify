package com.netiq.websockify;

import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static org.jboss.netty.handler.codec.http.HttpHeaders.setContentLength;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpMethod.GET;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.TEMPORARY_REDIRECT;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

import javax.activation.MimetypesFileTypeMap;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelFutureProgressListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.DefaultFileRegion;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.FileRegion;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.handler.codec.base64.Base64;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.handler.stream.ChunkedFile;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.infinities.skyport.vnc.bootstrap.ConsoleBootstrap;
import com.infinities.skyport.vnc.handler.ChallengeHandler;
import com.infinities.skyport.websockify.outbound.OutboundWebsocketHandler;
import com.infinities.skyport.websockify.outbound.OutboundWebsocketHandlerFactory;

public class WebsockifyProxyHandler extends SimpleChannelUpstreamHandler {

	private static final Logger logger = LoggerFactory.getLogger(WebsockifyProxyHandler.class);
	private static final String URL_PARAMETER = "url";
	public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
	public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
	public static final int HTTP_CACHE_SECONDS = 60;
	public static final String REDIRECT_PATH = "/redirect";

	// private final ClientSocketChannelFactory cf;
	// private final IProxyTargetResolver resolver;
	private String webDirectory;

	private WebSocketServerHandshaker handshaker = null;

	// This lock guards against the race condition that overrides the
	// OP_READ flag incorrectly.
	// See the related discussion: http://markmail.org/message/x7jc6mqx6ripynqf
	final Object trafficLock = new Object();

	private volatile Channel outboundChannel;
	private OutboundWebsocketHandler outboundWebsocketHandler;
	private ConsoleBootstrap bootstrap;
	private ByteBuffer byteBuffer;


	public WebsockifyProxyHandler(ClientSocketChannelFactory cf, IProxyTargetResolver resolver, String webDirectory,
			ConsoleBootstrap bootstrap, ByteBuffer byteBuffer) {
		// this.cf = cf;
		// this.resolver = resolver;
		this.webDirectory = webDirectory;
		this.outboundChannel = null;
		this.bootstrap = bootstrap;
		this.byteBuffer = byteBuffer;
	}

	// inboundChannel: client to proxy, outboundChannel: proxy to remote host
	private void ensureTargetConnection(ChannelEvent e, boolean websocket, ConsoleBootstrap cb, final Object sendMsg,
			Map<String, List<String>> queryParams) throws Exception {
		if (outboundChannel == null) {
			// Suspend incoming traffic until connected to the remote host.
			final Channel inboundChannel = e.getChannel();

			inboundChannel.setReadable(false);
			logger.info("Inbound proxy connection from {}", inboundChannel.getRemoteAddress());

			// resolve the target
			final InetSocketAddress target = cb.getTarget();
			if (target == null) {
				logger.error("Connection from {} failed to resolve target.", inboundChannel.getRemoteAddress());
				// there is no target
				inboundChannel.close();
				return;
			}

			// Start the connection attempt.
			if (websocket) {
				// cb.getPipeline().addLast("xvphandler", new
				// XvpVncAuthHandler(e.getChannel(), trafficLock));
				// TODO remove if use xvpvnc
				ChallengeHandler challengeHandler = new ChallengeHandler(e.getChannel(), trafficLock, byteBuffer);
				cb.getPipeline().addLast("challengehandler", challengeHandler);
				outboundWebsocketHandler = OutboundWebsocketHandlerFactory.getHandler(cb.getType(), inboundChannel,
						trafficLock);
				// outboundWebsocketHandler = new
				// OutboundTextWebsocketHandler(e.getChannel(), trafficLock);
				cb.getPipeline().addLast("handler", outboundWebsocketHandler);
			} else {
				cb.getPipeline().addLast("handler", new OutboundHandler(e.getChannel(), trafficLock));
			}
			ChannelFuture f = cb.connect(inboundChannel, trafficLock, queryParams);
			outboundChannel = f.getChannel();
			// Wait until the connection attempt succeeds or fails.

			if (sendMsg != null)
				outboundChannel.write(sendMsg);
		} else {
			if (sendMsg != null)
				outboundChannel.write(sendMsg);
		}
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
		try {
			logger.debug("receive msg");
			Object msg = e.getMessage();

			// An HttpRequest means either an initial websocket connection
			// or a web server request
			if (msg instanceof HttpRequest) {
				HttpRequest req = (HttpRequest) e.getMessage();

				List<Entry<String, String>> entrys = req.headers().entries();
				String str = "";

				for (Entry<String, String> entry : entrys) {
					str += entry.getKey() + "=" + entry.getValue() + " ";
				}

				logger.debug("request >>> {}, {}", new Object[] { str, req.getUri() });
				handleHttpRequest(ctx, (HttpRequest) msg, e);
				// A WebSocketFrame means a continuation of an established
				// websocket
				// connection
			} else if (msg instanceof WebSocketFrame) {
				handleWebSocketFrame(ctx, (WebSocketFrame) msg, e);
				// A channel buffer we treat as a VNC protocol request
			} else if (msg instanceof ChannelBuffer) {
				handleVncDirect(ctx, (ChannelBuffer) msg, e);
			}
		} catch (Exception ex) {
			throw ex;
		} finally {
		}
	}

	// public byte[] getDecodeBASE64String(byte[] base64Data) {
	// return org.apache.shiro.codec.Base64.decode(base64Data);
	// }

	private void handleHttpRequest(ChannelHandlerContext ctx, HttpRequest req, final MessageEvent e) throws Exception {
		// Allow only GET methods.
		if (req.getMethod() != GET) {
			sendHttpResponse(ctx, req, new DefaultHttpResponse(HTTP_1_1, FORBIDDEN));
			return;
		}

		String upgradeHeader = req.headers().get("Upgrade");
		if (upgradeHeader != null && upgradeHeader.toUpperCase().equals("WEBSOCKET")) {
			logger.debug("Websocket request from {}", e.getRemoteAddress());
			// Handshake
			WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
					this.getWebSocketLocation(req), "base64", false);
			this.handshaker = wsFactory.newHandshaker(req);
			if (this.handshaker == null) {
				wsFactory.sendUnsupportedWebSocketVersionResponse(ctx.getChannel());
			} else {
				// deal with a bug in the flash websocket emulation
				// it specifies WebSocket-Protocol when it seems it should
				// specify Sec-WebSocket-Protocol
				String protocol = req.headers().get("WebSocket-Protocol");
				String secProtocol = req.headers().get("Sec-WebSocket-Protocol");
				if (protocol != null && secProtocol == null) {
					req.headers().add("Sec-WebSocket-Protocol", protocol);
				}
				this.handshaker.handshake(ctx.getChannel(), req);
			}
			URI uri = new URI(req.getUri());
			Map<String, List<String>> queries = splitQuery(uri.getQuery());
			ensureTargetConnection(e, true, bootstrap, null, queries);// null);
		} else {
			HttpRequest request = (HttpRequest) e.getMessage();
			String redirectUrl = isRedirect(request.getUri());
			if (redirectUrl != null) {
				logger.debug("Redirecting to {}", redirectUrl);
				HttpResponse response = new DefaultHttpResponse(HTTP_1_1, TEMPORARY_REDIRECT);
				response.headers().set(HttpHeaders.Names.LOCATION, redirectUrl);
				sendHttpResponse(ctx, req, response);
				return;
			} else if (!Strings.isNullOrEmpty(webDirectory))/*
															 * not a websocket
															 * connection
															 * attempt
															 */{
				handleWebRequest(ctx, e);
			}
		}
	}

	private static Map<String, List<String>> splitQuery(String query) throws UnsupportedEncodingException {
		final Map<String, List<String>> query_pairs = new LinkedHashMap<String, List<String>>();
		if (!Strings.isNullOrEmpty(query)) {
			final String[] pairs = query.split("&");
			for (String pair : pairs) {
				final int idx = pair.indexOf("=");
				final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
				if (!query_pairs.containsKey(key)) {
					query_pairs.put(key, new LinkedList<String>());
				}
				final String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder
						.decode(pair.substring(idx + 1), "UTF-8") : null;
				query_pairs.get(key).add(value);
			}
		}
		return query_pairs;
	}

	private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame, final MessageEvent e) {
		logger.debug("receive websocketEvent");
		// Check for closing frame
		if (frame instanceof CloseWebSocketFrame) {
			this.handshaker.close(ctx.getChannel(), (CloseWebSocketFrame) frame);
			return;
		} else if (frame instanceof PingWebSocketFrame) {
			ctx.getChannel().write(new PongWebSocketFrame(frame.getBinaryData()));
			return;
		} else if (!(frame instanceof TextWebSocketFrame)) {
			throw new UnsupportedOperationException(
					String.format("%s frame types not supported", frame.getClass().getName()));
		}

		ChannelBuffer msg = ((TextWebSocketFrame) frame).getBinaryData();
		logger.debug("receive encodedMsg: {}", new String(msg.array()));
		ChannelBuffer decodedMsg = Base64.decode(msg);

		synchronized (trafficLock) {
			logger.debug("send decodedMsg pipeline: {}", outboundChannel.getPipeline().toString());
			outboundChannel.write(decodedMsg);
			// If outboundChannel is saturated, do not read until notified in
			// OutboundHandler.channelInterestChanged().
			if (!outboundChannel.isWritable()) {
				e.getChannel().setReadable(false);
			}
		}
	}

	private void handleVncDirect(ChannelHandlerContext ctx, ChannelBuffer buffer, final MessageEvent e) throws Exception {
		// ensure the target connection is open and send the data
		ensureTargetConnection(e, false, bootstrap, buffer, new HashMap<String, List<String>>());
	}

	private void handleWebRequest(ChannelHandlerContext ctx, final MessageEvent e) throws Exception {

		HttpRequest request = (HttpRequest) e.getMessage();
		if (request.getMethod() != GET) {
			sendError(ctx, METHOD_NOT_ALLOWED);
			return;
		}

		logger.info("Web request from {} for {}", new Object[] { e.getRemoteAddress(), request.getUri() });

		final String path = sanitizeUri(request.getUri());
		if (path == null) {
			sendError(ctx, FORBIDDEN);
			return;
		}

		File file = new File(path);
		if (file.isHidden() || !file.exists()) {
			sendError(ctx, NOT_FOUND);
			return;
		}
		if (!file.isFile()) {
			sendError(ctx, FORBIDDEN);
			return;
		}

		// Cache Validation
		String ifModifiedSince = request.headers().get(HttpHeaders.Names.IF_MODIFIED_SINCE);
		if (ifModifiedSince != null && !ifModifiedSince.equals("")) {
			SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
			Date ifModifiedSinceDate = dateFormatter.parse(ifModifiedSince);

			// Only compare up to the second because the datetime format we send
			// to the client does not have milliseconds
			long ifModifiedSinceDateSeconds = ifModifiedSinceDate.getTime() / 1000;
			long fileLastModifiedSeconds = file.lastModified() / 1000;
			if (ifModifiedSinceDateSeconds == fileLastModifiedSeconds) {
				sendNotModified(ctx);
				return;
			}
		}

		RandomAccessFile raf;
		try {
			raf = new RandomAccessFile(file, "r");
		} catch (FileNotFoundException fnfe) {
			sendError(ctx, NOT_FOUND);
			return;
		}
		long fileLength = raf.length();

		HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
		setContentLength(response, fileLength);
		setContentTypeHeader(response, file);
		setDateAndCacheHeaders(response, file);

		Channel ch = e.getChannel();

		// Write the initial line and the header.
		ch.write(response);

		// Write the content.
		ChannelFuture writeFuture;
		if (ch.getPipeline().get(SslHandler.class) != null) {
			// Cannot use zero-copy with HTTPS.
			writeFuture = ch.write(new ChunkedFile(raf, 0, fileLength, 8192));
		} else {
			// No encryption - use zero-copy.
			final FileRegion region = new DefaultFileRegion(raf.getChannel(), 0, fileLength);
			writeFuture = ch.write(region);
			writeFuture.addListener(new ChannelFutureProgressListener() {

				@Override
				public void operationComplete(ChannelFuture future) {
					region.releaseExternalResources();
				}

				@Override
				public void operationProgressed(ChannelFuture future, long amount, long current, long total) {
					System.out.printf("%s: %d / %d (+%d)%n", path, current, total, amount);
				}
			});
		}

		// Decide whether to close the connection or not.
		if (!isKeepAlive(request)) {
			// Close the connection when the whole content is written out.
			writeFuture.addListener(ChannelFutureListener.CLOSE);
		}
	}

	private static Map<String, String> getQueryMap(String query) {
		String[] params = query.split("&");
		Map<String, String> map = new HashMap<String, String>();
		for (String param : params) {
			String name = param.split("=")[0];
			String value = param.split("=")[1];
			map.put(name, value);
		}
		return map;
	}

	// checks to see if the uri is a redirect request
	// if it is, it returns the url parameter
	private String isRedirect(String uri) throws URISyntaxException, MalformedURLException {
		// Decode the path.
		URI url = new URI(uri);

		if (REDIRECT_PATH.equals(url.getPath())) {
			String query = url.getRawQuery();
			Map<String, String> params = getQueryMap(query);

			String urlParam = params.get(URL_PARAMETER);
			if (urlParam == null)
				return null;

			try {
				return URLDecoder.decode(urlParam, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				logger.error("unexpected exception", e);
			}
		}

		return null;
	}

	private String sanitizeUri(String uri) throws URISyntaxException {
		// Decode the path.
		URI url = new URI(uri);
		uri = url.getPath();

		// Convert file separators.
		uri = uri.replace('/', File.separatorChar);

		// Simplistic dumb security check.
		// You will have to do something serious in the production environment.
		if (uri.contains(File.separator + ".") || uri.contains("." + File.separator) || uri.startsWith(".")
				|| uri.endsWith(".")) {
			return null;
		}

		// Convert to absolute path.
		return webDirectory + File.separator + uri;
	}

	private void sendHttpResponse(ChannelHandlerContext ctx, HttpRequest req, HttpResponse res) {
		// Generate an error page if response status code is not OK (200).
		if (res.getStatus().getCode() != 200) {
			res.setContent(ChannelBuffers.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8));
			setContentLength(res, res.getContent().readableBytes());
		}

		// Send the response and close the connection if necessary.
		ChannelFuture f = ctx.getChannel().write(res);
		if (!isKeepAlive(req) || res.getStatus().getCode() != 200) {
			f.addListener(ChannelFutureListener.CLOSE);
		}
	}

	private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
		HttpResponse response = new DefaultHttpResponse(HTTP_1_1, status);
		response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
		response.setContent(ChannelBuffers.copiedBuffer("Failure: " + status.toString() + "\r\n", CharsetUtil.UTF_8));

		// Close the connection as soon as the error message is sent.
		ctx.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
	}

	/**
	 * When file timestamp is the same as what the browser is sending up, send a
	 * "304 Not Modified"
	 * 
	 * @param ctx
	 *            Context
	 */
	private void sendNotModified(ChannelHandlerContext ctx) {
		HttpResponse response = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.NOT_MODIFIED);
		setDateHeader(response);

		// Close the connection as soon as the error message is sent.
		ctx.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
	}

	/**
	 * Sets the Date header for the HTTP response
	 * 
	 * @param response
	 *            HTTP response
	 */
	private void setDateHeader(HttpResponse response) {
		SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
		dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

		Calendar time = new GregorianCalendar();
		response.headers().set(HttpHeaders.Names.DATE, dateFormatter.format(time.getTime()));
	}

	/**
	 * Sets the Date and Cache headers for the HTTP Response
	 * 
	 * @param response
	 *            HTTP response
	 * @param fileToCache
	 *            file to extract content type
	 */
	private void setDateAndCacheHeaders(HttpResponse response, File fileToCache) {
		SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
		dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

		// Date header
		Calendar time = new GregorianCalendar();
		response.headers().set(HttpHeaders.Names.DATE, dateFormatter.format(time.getTime()));

		// Add cache headers
		time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
		response.headers().set(HttpHeaders.Names.EXPIRES, dateFormatter.format(time.getTime()));
		response.headers().set(HttpHeaders.Names.CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS);
		response.headers().set(HttpHeaders.Names.LAST_MODIFIED, dateFormatter.format(new Date(fileToCache.lastModified())));
	}

	/**
	 * Sets the content type header for the HTTP Response
	 * 
	 * @param response
	 *            HTTP response
	 * @param file
	 *            file to extract content type
	 */
	private void setContentTypeHeader(HttpResponse response, File file) {
		MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
		response.headers().set(HttpHeaders.Names.CONTENT_TYPE, mimeTypesMap.getContentType(file.getPath()));
	}

	private String getWebSocketLocation(HttpRequest req) {
		return "wss://" + req.headers().get(HttpHeaders.Names.HOST);
	}

	@Override
	public void channelInterestChanged(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		// If inboundChannel is not saturated anymore, continue accepting
		// the incoming traffic from the outboundChannel.
		synchronized (trafficLock) {
			if (e.getChannel().isWritable() && outboundChannel != null) {
				outboundChannel.setReadable(true);
			}
		}
	}

	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {

		logger.info("Inbound proxy connection from {} closed", ctx.getChannel().getRemoteAddress());
		if (outboundChannel != null) {
			closeOnFlush(outboundChannel);
		}
		bootstrap.close();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {

		logger.error("Exception on inbound proxy connection", e.getCause());
		closeOnFlush(e.getChannel());
		bootstrap.close();
	}

	/**
	 * Closes the specified channel after all queued write requests are flushed.
	 */
	static void closeOnFlush(Channel ch) {
		if (ch.isConnected()) {
			ch.write(ChannelBuffers.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
		}
	}
}
