package edu.ucsf.orng.shindig.auth;

import static org.apache.shindig.auth.AbstractSecurityToken.Keys.APP_URL;
import static org.apache.shindig.auth.AbstractSecurityToken.Keys.OWNER;
import static org.apache.shindig.auth.AbstractSecurityToken.Keys.VIEWER;
import static org.apache.shindig.auth.AbstractSecurityToken.Keys.CONTAINER;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.channels.ServerSocketChannel;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.shindig.auth.BlobCrypterSecurityToken;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.auth.SecurityTokenCodec;
import org.apache.shindig.auth.SecurityTokenException;
import org.apache.shindig.common.crypto.BlobCrypterException;
import org.apache.shindig.common.servlet.GuiceServletContextListener.CleanupCapable;
import org.apache.shindig.common.servlet.GuiceServletContextListener.CleanupHandler;
import org.apache.shindig.config.ContainerConfig;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class OrngSecurityTokenService implements Runnable, CleanupCapable {

	private static final Logger LOG = Logger.getLogger(OrngSecurityTokenService.class.getName());	
	
	private SecurityTokenCodec securityTokenCodec;
	private final int port;
	private final int socketTimeout;
	private boolean stop = false;
	private ExecutorService listenerService = null;
	private ExecutorService encoderService = null;
	
	@Inject
	public OrngSecurityTokenService(ContainerConfig config, SecurityTokenCodec codec, @Named("orng.tokenservice.port") int port, @Named("orng.tokenservice.socketTimeout") int socketTimeout, CleanupHandler cleanup) {
		this.securityTokenCodec = codec;
		this.port = port;
		this.socketTimeout = socketTimeout; // we expect this to be in seconds, not milliseconds!
	    cleanup.register(this);
		// start listening for connections
	    listenerService = Executors.newFixedThreadPool(1);
	    encoderService = Executors.newCachedThreadPool();
	    listenerService.submit(this);
	}

	public void run() {
		LOG.log(Level.INFO, "Waiting for client message...");
		//
		// The server do a loop here to accept all connection initiated by the
		// client application.
		//

		while (!stop) {
			try {
				acceptConnections();
			} 
			catch (Exception e) {
				if (stop) {
				    LOG.log(Level.INFO, "Shutting down secure token service");				
				}
				else {
				    LOG.log(Level.WARNING, "Error in secure token service", e);				
				}
			}
		}
	}

	// Accept connections for current time. Lazy Exception thrown.
	private void acceptConnections() throws Exception {
		// Create a new server socket and set to non blocking mode
		ServerSocketChannel ssc = ServerSocketChannel.open();
		// Bind the server socket to the local host and port

		InetAddress lh = InetAddress.getLocalHost();
		InetSocketAddress isa = new InetSocketAddress(lh, port);
		ssc.socket().bind(isa);
		ssc.configureBlocking(true);

		// Here's where everything happens. The select method will
		// return when any operations registered above have occurred, the
		// thread has been interrupted, etc.
		try {
			while (!stop) {
				final Socket s = ssc.accept().socket();
				s.setSoTimeout(socketTimeout * 1000);
				LOG.log(Level.INFO, "SoTimeout, SoLinger = " + s.getSoTimeout() + ", " + s.getSoLinger());
				encoderService.submit(new Runnable() {
					public void run() {
						BufferedReader in = null;
						PrintWriter out = null;
						try {						
							// we expect exceptions here
							in = new BufferedReader(new InputStreamReader(
									s.getInputStream()));
							out = new PrintWriter(s.getOutputStream(), true);
							String input = in.readLine();
							while (StringUtils.isNotBlank(input)) {
								String token = convert(input);
								// Send back the security token
								out.println(token);
								out.flush();
								input = in.readLine();
							}
						} 
						catch (IOException e) {
							LOG.log(Level.INFO, "Socket Exception : " + e.getMessage(), e);
						} 
						catch (Exception e) {
							LOG.log(Level.WARNING, e.getMessage(), e);
						}
						finally {
							if (in != null) {
								try { in.close(); } catch (Exception e) {LOG.log(Level.WARNING, e.getMessage(), e);}								
							}
							if (out != null) {
								try { out.close(); } catch (Exception e) {LOG.log(Level.WARNING, e.getMessage(), e);}								
							}
						}
					}
				});
			}
		}
		finally {
			ssc.close();
		}
	}

	String convert(String input) throws UnsupportedEncodingException, SecurityTokenException, BlobCrypterException {
	    LOG.log(Level.INFO, "Received " + input + ": length = " + input.length());
		Map<String, String> tokenParameters = getQueryMap(input);
	    
		return convert(tokenParameters);
	}
	
	public String convert(Map<String, String> tokenParameters) throws SecurityTokenException {
		SecurityToken token = new BlobCrypterSecurityToken(tokenParameters.get(CONTAINER.getKey()), "*", "0", tokenParameters);
		return securityTokenCodec.encodeToken(token);		
	}

	private Map<String, String> getQueryMap(String query) throws UnsupportedEncodingException {
		String[] params = query.split("&");
		Map<String, String> map = Maps.newHashMap();
		for (String param : params) {
			String name = param.split("=")[0];
			String value = param.split("=")[1];
			map.put(name, 
					OWNER.getKey().equals(name) || VIEWER.getKey().equals(name) || APP_URL.getKey().equals(name) ? URLDecoder.decode(value, "UTF-8") : value);
		}
		return map;
	}

	public void cleanup() {
		stop = true;
		listenerService.shutdown();
		encoderService.shutdown();
	}

}
