package edu.ucsf.profiles.shindig.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.Runnable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.shindig.auth.BlobCrypterSecurityToken;
import org.apache.shindig.auth.BlobCrypterSecurityTokenCodec;
import org.apache.shindig.common.crypto.BasicBlobCrypter;
import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.common.crypto.BlobCrypterException;
import org.apache.shindig.common.util.Utf8UrlCoder;
import org.apache.shindig.config.ContainerConfig;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

public class SecureTokenGeneratorService implements Runnable {

	private final int port;

	protected static final String CONTAINER_KEY = "c";
	protected static final String OWNER_KEY = "o";
	protected static final String VIEWER_KEY = "v";
	protected static final String GADGET_KEY = "g";

	/**
	 * Keys are container ids, values are crypters
	 */
	protected final Map<String, BlobCrypter> crypters = Maps.newHashMap();

	public SecureTokenGeneratorService(int port) {
		this.port = port;
	}

	@Inject
	public void configure(ContainerConfig config) {
		try {
			for (String container : config.getContainers()) {
				String keyFile = config.getString(container,
						BlobCrypterSecurityTokenCodec.SECURITY_TOKEN_KEY_FILE);
				if (keyFile != null) {
					BlobCrypter crypter = new BasicBlobCrypter(
							new File(keyFile));
					crypters.put(container, crypter);
				}
			}
		} catch (IOException e) {
			// Someone specified securityTokenKeyFile, but we couldn't load the
			// key. That merits killing
			// the server.
			throw new RuntimeException(e);
		}
	}

	public void run() {
		System.out.println("Waiting for client message...");
		//
		// The server do a loop here to accept all connection initiated by the
		// client application.
		//

		try {
			acceptConnections();
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("Shutting down secure token service");
	}

	// Accept connections for current time. Lazy Exception thrown.
	private void acceptConnections() throws Exception {
		// Selector for incoming time requests
		Selector acceptSelector = SelectorProvider.provider().openSelector();

		// Create a new server socket and set to non blocking mode
		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.configureBlocking(false);

		// Bind the server socket to the local host and port

		InetAddress lh = InetAddress.getLocalHost();
		InetSocketAddress isa = new InetSocketAddress(lh, port);
		ssc.socket().bind(isa);

		// Register accepts on the server socket with the selector. This
		// step tells the selector that the socket wants to be put on the
		// ready list when accept operations occur, so allowing multiplexed
		// non-blocking I/O to take place.
		SelectionKey acceptKey = ssc.register(acceptSelector,
				SelectionKey.OP_ACCEPT);

		int keysAdded = 0;

		// Here's where everything happens. The select method will
		// return when any operations registered above have occurred, the
		// thread has been interrupted, etc.
		while ((keysAdded = acceptSelector.select()) > 0) {
			// Someone is ready for I/O, get the ready keys
			Set<SelectionKey> readyKeys = acceptSelector.selectedKeys();
			Iterator<SelectionKey> i = readyKeys.iterator();

			// Walk through the ready keys collection and process date requests.
			while (i.hasNext()) {
				SelectionKey sk = i.next();
				i.remove();
				// The key indexes into the selector so you
				// can retrieve the socket that's ready for I/O
				ServerSocketChannel nextReady = (ServerSocketChannel) sk
						.channel();
				// Accept the date request and send back the date string
				Socket s = nextReady.accept().socket();
				BufferedReader in = new BufferedReader(new InputStreamReader(
						s.getInputStream()));
				String input = in.readLine();

				PrintWriter out = new PrintWriter(s.getOutputStream(), true);
				try {
					String token = convert(input);
					// Send back the security token
					out.print(token);

				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					out.close();
				}
			}
		}
	}

	String convert(String input) throws BlobCrypterException, UnsupportedEncodingException {
		System.out
				.println("Received " + input + ": length = " + input.length());
		Map<String, String> tokenParams = getQueryMap(input);

		String container = tokenParams.containsKey(CONTAINER_KEY) ? tokenParams
				.get(CONTAINER_KEY) : "default";

		BlobCrypterSecurityToken st = new BlobCrypterSecurityToken(
				crypters.get(container), container, "localhost");
		st.setViewerId(tokenParams.get(VIEWER_KEY));
		st.setOwnerId(tokenParams.get(OWNER_KEY));
		st.setAppUrl(tokenParams.get(GADGET_KEY));

		String token = Utf8UrlCoder.encode(st.encrypt());

		// Send back the security token
		System.out.println("Returning " + token);
		return token;
	}

	private Map<String, String> getQueryMap(String query) throws UnsupportedEncodingException {
		String[] params = query.split("&");
		Map<String, String> map = Maps.newHashMap();
		for (String param : params) {
			String name = param.split("=")[0];
			String value = param.split("=")[1];
			map.put(name, GADGET_KEY.equals(name) ? URLDecoder.decode(value, "UTF-8") : value);
		}
		return map;
	}

	// Entry point.
	public static void main(String[] args) {
		// Parse command line arguments and
		// create a new time server (no arguments yet)
		try {
			SecureTokenGeneratorService nbt = new SecureTokenGeneratorService(
					8777);
			nbt.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
