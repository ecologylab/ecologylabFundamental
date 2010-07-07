/**
 * 
 */
package ecologylab.services.distributed.server.clientsessionmanager;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import ecologylab.collections.Scope;
import ecologylab.generic.StringTools;
import ecologylab.services.distributed.common.ServerConstants;
import ecologylab.services.distributed.impl.MessageWithMetadata;
import ecologylab.services.distributed.impl.MessageWithMetadataPool;
import ecologylab.services.distributed.impl.NIOServerIOThread;
import ecologylab.services.distributed.server.NIOServerProcessor;
import ecologylab.services.exceptions.BadClientException;
import ecologylab.services.messages.RequestMessage;
import ecologylab.services.messages.ResponseMessage;
import ecologylab.services.messages.UpdateMessage;
import ecologylab.xml.ElementState;
import ecologylab.xml.TranslationScope;
import ecologylab.xml.SIMPLTranslationException;

/**
 * The base class for all ContextManagers, objects that track the state and respond to clients on a
 * server. There is a one-to-one correspondence between connected clients and ContextManager
 * instances.
 * 
 * AbstractContextManager handles all encoding and decoding of messages, as well as translating
 * them. Hook methods provide places where subclasses may modify behavior for specific purposes.
 * 
 * Typical usage is to have the context manager's request queue be filled by a network thread, while
 * it is emptied by a working thread.
 * 
 * The normal cycle for filling the queue is to call acquireIncomingSequenceBuf() to clear and get
 * the incomingCharBuffer, then fill it externally (normally passing it as an argument to a
 * CharsetDecoder.decode call), then calling processIncomingSequenceBufToQueue() to release it and
 * let the ContextManager store the characters, converting messages into objects as they become
 * available.
 * 
 * For a complete, basic implementation (which is suitable for most uses), see
 * {@link ecologylab.services.distributed.server.clientsessionmanager.ClientSessionManager
 * ContextManager}.
 * 
 * @author Zachary O. Toups (zach@ecologylab.net)
 * 
 */
public abstract class TCPClientSessionManager<S extends Scope> extends BaseSessionManager<S> implements ServerConstants
{
	/**
	 * Stores the key-value pairings from a parsed HTTP-like header on an incoming message.
	 */
	protected final HashMap<String, String>															headerMap									= new HashMap<String, String>();

	protected int																												startReadIndex						= 0;

	/**
	 * Stores incoming character data until it can be parsed into an XML message and turned into a
	 * Java object.
	 */
	protected final StringBuilder																				msgBufIncoming;

	/** Stores outgoing character data for ResponseMessages. */
	protected final StringBuilder																				msgBufOutgoing;

	/** Stores outgoing header character data. */
	protected final StringBuilder																				headerBufOutgoing					= new StringBuilder(
																																																		MAX_HTTP_HEADER_LENGTH);

	protected final StringBuilder																				startLine									= new StringBuilder(
																																																		MAX_HTTP_HEADER_LENGTH);

	/**
	 * The network communicator that will handle all the reading and writing for the socket associated
	 * with this ContextManager
	 */
	protected NIOServerIOThread																					server;

	/**
	 * The maximum message length allowed for clients that connect to this session manager. Note that
	 * most of the buffers used by AbstractClientManager are mutable in size, and will dynamically
	 * reallocate as necessary if they were initialized to be too small.
	 */
	protected int																												maxMessageSize;

	/** Used to translate incoming message XML strings into RequestMessages. */
	protected TranslationScope																					translationScope;

	/**
	 * stores the sequence of characters read from the header of an incoming message, may need to
	 * persist across read calls, as the entire header may not be sent at once.
	 */
	private final StringBuilder																					currentHeaderSequence			= new StringBuilder();

	/**
	 * stores the sequence of characters read from the header of an incoming message and identified as
	 * being a key for a header entry; may need to persist across read calls.
	 */
	private final StringBuilder																					currentKeyHeaderSequence	= new StringBuilder();

	/** A buffer for data that will be sent back to the client. */
	private final CharBuffer																						outgoingChars;

	/**
	 * Tracks the number of bad transmissions from the client; used for determining if a client is
	 * bad.
	 */
	private int																													badTransmissionCount;

	private int																													endOfFirstHeader					= -1;

	/**
	 * Counts how many characters still need to be extracted from the incomingMessageBuffer before
	 * they can be turned into a message (based upon the HTTP header). A value of -1 means that there
	 * is not yet a complete header, so no length has been determined (yet).
	 */
	private int																													contentLengthRemaining		= -1;

	/**
	 * Specifies whether or not the current message uses compression.
	 */
	private String																											contentEncoding						= "identity";

	/**
	 * Set of encoding schemes that the client supports
	 */
	private Set<String>																									availableEncodings				= new HashSet<String>();

	/**
	 * Stores the first XML message from the incomingMessageBuffer, or parts of it (if it is being
	 * read over several invocations).
	 */
	private final StringBuilder																					firstMessageBuffer				= new StringBuilder();

	private long																												contentUid								= -1;

	private CharBuffer																									zippingChars;

	private ByteBuffer																									zippingInBytes;

	private Inflater																										inflater									= new Inflater();

	private Deflater																										deflater									= new Deflater();

	private ByteBuffer																									zippingOutBytes;

	private ByteBuffer																									compressedMessageBuffer;

	/**
	 * A queue of the requests to be performed by this ContextManager. Subclasses may override
	 * functionality and not use requestQueue.
	 */
	protected final Queue<MessageWithMetadata<RequestMessage, Object>>	requestQueue							= new LinkedBlockingQueue<MessageWithMetadata<RequestMessage, Object>>();

	protected final MessageWithMetadataPool<RequestMessage, Object>			reqPool										= new MessageWithMetadataPool<RequestMessage, Object>(
																																																		2,
																																																		4);

	protected CharsetDecoder																						decoder										= CHARSET
																																																		.newDecoder();

	protected CharsetEncoder																						encoder										= CHARSET
																																																		.newEncoder();

	private static final String																					POST_PREFIX								= "POST ";

	private static final String																					GET_PREFIX								= "GET ";

	/**
	 * Creates a new ContextManager.
	 * 
	 * @param sessionId
	 * @param clientSessionScope
	 *          TODO
	 * @param maxMessageSizeIn
	 * @param server
	 * @param frontend
	 * @param socket
	 * @param translationScope
	 * @param registry
	 */
	public TCPClientSessionManager(String sessionId, int maxMessageSizeIn, NIOServerIOThread server,
			NIOServerProcessor frontend, SelectionKey socket, TranslationScope translationScope,
			Scope<?> baseScope)
	{
		super(sessionId, frontend, socket, baseScope);

		this.server = server;
		this.translationScope = translationScope;

		// set up session id
		this.sessionId = sessionId;

		this.maxMessageSize = maxMessageSizeIn;

		this.outgoingChars = CharBuffer.allocate(maxMessageSize + MAX_HTTP_HEADER_LENGTH);

		this.zippingChars = CharBuffer.allocate(maxMessageSize);
		this.zippingInBytes = ByteBuffer.allocate(maxMessageSize);
		this.zippingOutBytes = ByteBuffer.allocate(maxMessageSize);
		this.compressedMessageBuffer = ByteBuffer.allocate(maxMessageSize);

		msgBufIncoming = new StringBuilder(maxMessageSize + MAX_HTTP_HEADER_LENGTH);

		msgBufOutgoing = new StringBuilder(maxMessageSize + MAX_HTTP_HEADER_LENGTH);

		this.prepareBuffers(msgBufIncoming, msgBufOutgoing, headerBufOutgoing);
	}

	/**
	 * Extracts messages from the given CharBuffer, using HTTP-like headers, converting them into
	 * RequestMessage instances, then enqueues those instances.
	 * 
	 * enqueueStringMessage will normally be called repeatedly, as new data comes in from a client. It
	 * will automatically parse messages that are split up over multiple reads, and will handle
	 * multiple messages in one read, if necessary.
	 * 
	 * @param message
	 *          the CharBuffer containing one or more messages, or pieces of messages.
	 */
	public final void processIncomingSequenceBufToQueue(CharBuffer incomingSequenceBuf)
			throws CharacterCodingException, BadClientException
	{
		// debug("incoming: " + incomingSequenceBuf);

		synchronized (msgBufIncoming)
		{
			msgBufIncoming.append(incomingSequenceBuf);

			// look for HTTP header
			while (msgBufIncoming.length() > 0)
			{
				if (endOfFirstHeader == -1)
				{
					endOfFirstHeader = this.parseHeader(startReadIndex, msgBufIncoming);
				}

				if (endOfFirstHeader == -1)
				{ /*
					 * no header yet; if it's too large, bad client; if it's not too large yet, just exit,
					 * it'll get checked again when more data comes down the pipe
					 */
					if (msgBufIncoming.length() > ServerConstants.MAX_HTTP_HEADER_LENGTH)
					{
						// clear the buffer
						BadClientException e = new BadClientException(
								((SocketChannel) this.socketKey.channel()).socket().getInetAddress()
										.getHostAddress(), "Maximum HTTP header length exceeded. Read "
										+ msgBufIncoming.length()
										+ "/"
										+ MAX_HTTP_HEADER_LENGTH);

						msgBufIncoming.setLength(0);

						throw e;
					}

					// next time around, start reading from where we left off this
					// time
					startReadIndex = msgBufIncoming.length();

					break;
				}
				else
				{ // we've read all of the header, and have it loaded into the map;
					// now we can use it
					if (contentLengthRemaining == -1)
					{
						try
						{
							// handle all header information here; delete it when done
							// here
							String contentLengthString = this.headerMap.get(CONTENT_LENGTH_STRING);
							contentLengthRemaining = (contentLengthString != null) ? Integer
									.parseInt(contentLengthString) : 0;

							String uidString = this.headerMap.get(UNIQUE_IDENTIFIER_STRING);
							contentUid = (uidString != null) ? Long.parseLong(uidString) : 0;

							this.contentEncoding = this.headerMap.get(HTTP_CONTENT_CODING);

							String encodings = this.headerMap.get(HTTP_ACCEPT_ENCODING);
							if (encodings != null)
							{
								String[] encodingList = encodings.split(",");

								for (String encoding : encodingList)
								{
									this.availableEncodings.add(encoding);
								}
							}

							// done with the header text; delete it; header values will
							// be retained for later processing by subclasses
							msgBufIncoming.delete(0, endOfFirstHeader);
						}
						catch (NumberFormatException e)
						{
							e.printStackTrace();
							contentLengthRemaining = -1;
						}
						// next time we read the header (the next message), we need to
						// start from the beginning
						startReadIndex = 0;
					}
				}

				/*
				 * we have the end of the header (otherwise we would have broken out earlier). If we don't
				 * have the content length, something bad happened, because it should have been read.
				 */
				if (contentLengthRemaining == -1)
				{
					/*
					 * if we still don't have the remaining length, then there was a problem
					 */
					break;
				}
				else if (contentLengthRemaining > maxMessageSize)
				{
					throw new BadClientException(((SocketChannel) this.socketKey.channel()).socket()
							.getInetAddress().getHostAddress(), "Specified content length too large: "
							+ contentLengthRemaining);
				}

				try
				{
					// see if the incoming buffer has enough characters to
					// include the specified content length
					if (msgBufIncoming.length() >= contentLengthRemaining)
					{
						firstMessageBuffer.append(msgBufIncoming.substring(0, contentLengthRemaining));

						msgBufIncoming.delete(0, contentLengthRemaining);

						// reset to do a new read on the next invocation
						contentLengthRemaining = -1;
						endOfFirstHeader = -1;
					}
					else
					{
						firstMessageBuffer.append(msgBufIncoming);

						// indicate that we need to get more from the buffer in
						// the next invocation
						contentLengthRemaining -= msgBufIncoming.length();

						msgBufIncoming.setLength(0);
					}
				}
				catch (NullPointerException e)
				{
					e.printStackTrace();
				}

				if ((contentLengthRemaining == -1))
				{ /*
					 * if we've read a complete message, then contentLengthRemaining will be reset to -1
					 */
					if (this.contentEncoding == null || this.contentEncoding.equals("identity"))
					{
						processString(firstMessageBuffer, contentUid);
					}
					else if (contentEncoding.equals(HTTP_DEFLATE_ENCODING))
					{
						try
						{
							processString(this.unCompress(firstMessageBuffer), contentUid);
						}
						catch (DataFormatException e)
						{
							throw new BadClientException(((SocketChannel) this.socketKey.channel()).socket()
									.getInetAddress().getHostAddress(), "Content was not encoded properly: "
									+ e.getMessage());
						}
					}
					else
					{
						throw new BadClientException(((SocketChannel) this.socketKey.channel()).socket()
								.getInetAddress().getHostAddress(), "Content encoding: "
								+ contentEncoding
								+ " not supported!");
					}

					// clean up: clear the message buffer and the header values
					firstMessageBuffer.setLength(0);
					this.headerMap.clear();
					StringTools.clear(this.startLine);
				}
			}
		}
	}

	private CharSequence unCompress(StringBuilder firstMessageBuffer)
			throws CharacterCodingException, DataFormatException
	{
		synchronized (zippingChars)
		{
			zippingChars.clear();

			firstMessageBuffer.getChars(0, firstMessageBuffer.length(), this.zippingChars.array(), 0);
			zippingChars.position(0);
			zippingChars.limit(firstMessageBuffer.length());

			zippingInBytes.clear();

			encoder.reset();
			encoder.encode(zippingChars, zippingInBytes, true);
			encoder.flush(zippingInBytes);

			zippingInBytes.flip();

			inflater.reset();
			inflater.setInput(zippingInBytes.array(), zippingInBytes.position(), zippingInBytes.limit());

			zippingOutBytes.clear();
			inflater
					.inflate(zippingOutBytes.array(), zippingOutBytes.position(), zippingOutBytes.limit());

			zippingOutBytes.position(0);
			zippingOutBytes.limit(inflater.getTotalOut());

			zippingChars.clear();

			decoder.reset();
			decoder.decode(zippingOutBytes, zippingChars, true);
			decoder.flush(zippingChars);

			zippingChars.flip();

			firstMessageBuffer.setLength(0);

			return firstMessageBuffer.append(zippingChars.array(), 0, zippingChars.limit());
		}
	}

	/**
	 * Calls processRequest(RequestMessage) on each queued message as they are acquired through
	 * getNextRequest() and finishing when isMessageWaiting() returns false.
	 * 
	 * The functionality of processAllMessagesAndSendResponses() may be overridden by overridding the
	 * following methods: isMessageWaiting(), processRequest(RequestMessage), getNextRequest().
	 * 
	 * @throws BadClientException
	 */
	public final void processAllMessagesAndSendResponses() throws BadClientException
	{
		while (isMessageWaiting())
		{
			this.processNextMessageAndSendResponse();
		}
	}

	/**
	 * Sets the SelectionKey, and sets the new SelectionKey to have the same attachment (session id)
	 * as the old one.
	 * 
	 * @param socket
	 *          the socket to set
	 */
	public void setSocket(SelectionKey socket)
	{
		String sessionId = (String) this.socketKey.attachment();

		this.socketKey = socket;

		this.socketKey.attach(sessionId);
	}

	protected abstract void clearOutgoingMessageBuffer(StringBuilder outgoingMessageBuf);

	protected abstract void clearOutgoingMessageHeaderBuffer(StringBuilder outgoingMessageHeaderBuf);

	protected abstract void createHeader(int messageSize, StringBuilder outgoingMessageHeaderBuf,
			RequestMessage incomingRequest, ResponseMessage outgoingResponse, long uid);

	protected abstract void makeUpdateHeader(int messageSize, StringBuilder headerBufOutgoing,
			UpdateMessage<?> update);

	/**
	 * Parses the header of an incoming set of characters (i.e. a message from a client to a server),
	 * loading all of the HTTP-like headers into the given headerMap.
	 * 
	 * If headerMap is null, this method will throw a null pointer exception.
	 * 
	 * @param allIncomingChars
	 *          - the characters read from an incoming stream.
	 * @param headerMap
	 *          - the map into which all of the parsed headers will be placed.
	 * @return the length of the parsed header, or -1 if it was not yet found.
	 */
	protected int parseHeader(int startChar, StringBuilder allIncomingChars)
	{
		// indicates that we might be at the end of the header
		boolean maybeEndSequence = false;

		// true if the start line has been found, or if a key has been found
		// instead
		boolean noMoreStartLine = false;

		char currentChar;

		synchronized (currentHeaderSequence)
		{
			StringTools.clear(currentHeaderSequence);
			StringTools.clear(currentKeyHeaderSequence);

			int length = allIncomingChars.length();

			for (int i = 0; i < length; i++)
			{
				currentChar = allIncomingChars.charAt(i);

				switch (currentChar)
				{
				case (':'):
					/*
					 * we have the end of a key; move the currentHeaderSequence into the
					 * currentKeyHeaderSequence and clear it
					 */
					currentKeyHeaderSequence.append(currentHeaderSequence);

					StringTools.clear(currentHeaderSequence);

					noMoreStartLine = true;

					break;
				case ('\r'):
					/*
					 * we have the end of a line; if there's a CRLF, then we have the end of the value
					 * sequence or the end of the header.
					 */
					if (allIncomingChars.charAt(i + 1) == '\n')
					{
						if (!maybeEndSequence)
						{
							if (noMoreStartLine)
							{ // load the key/value pair
								headerMap.put(currentKeyHeaderSequence.toString().toLowerCase(),
										currentHeaderSequence.toString().trim());
							}
							else
							{ // we potentially have data w/o a key-value pair; this
								// is the start-line of an HTTP header
								StringTools.clear(startLine);
								this.startLine.append(currentHeaderSequence);

								noMoreStartLine = true;
							}

							StringTools.clear(currentKeyHeaderSequence);
							StringTools.clear(currentHeaderSequence);

							i++; // so we don't re-read that last character
						}
						else
						{ // end of the header
							return i + 2;
						}

						maybeEndSequence = true;
					}
					break;
				default:
					currentHeaderSequence.append(currentChar);
					maybeEndSequence = false;
					break;
				}
			}

			// if we got here, we didn't finish the header
			return -1;
		}
	}

	protected abstract void prepareBuffers(StringBuilder incomingMessageBuf,
			StringBuilder outgoingMessageBuf, StringBuilder outgoingMessageHeaderBuf);

	protected abstract void translateResponseMessageToStringBufferContents(
			RequestMessage requestMessage, ResponseMessage responseMessage, StringBuilder messageBuffer)
			throws SIMPLTranslationException;

	/**
	 * Translates the given XML String into a RequestMessage object.
	 * 
	 * translateStringToRequestMessage(String) may be overridden to provide specific functionality,
	 * such as a ContextManager that does not use XML Strings.
	 * 
	 * @param messageCharSequence
	 *          - an XML String representing a RequestMessage object.
	 * @return the RequestMessage created by translating messageString into an object.
	 * @throws SIMPLTranslationException
	 *           if an error occurs when translating from XML into a RequestMessage.
	 * @throws UnsupportedEncodingException
	 *           if the String is not encoded properly.
	 */
	protected RequestMessage translateStringToRequestMessage(CharSequence messageCharSequence)
			throws SIMPLTranslationException, UnsupportedEncodingException
	{
		String startLineString = null;
		if (this.startLine == null || (startLineString = startLine.toString()).equals(""))
		{ // normal case
			return (RequestMessage) ElementState.translateFromXMLCharSequence(messageCharSequence,
					translationScope);
		}
		else if (startLineString.startsWith(GET_PREFIX))
		{ // get case
			// XXX
			// XXX
			// XXX
			// TODO when we figure out the right kind of response, we need to
			// handle get
			return (RequestMessage) null;
		}
		else if (startLineString.startsWith(POST_PREFIX))
		{ // post case
			String messageString = messageCharSequence.toString();

			if (!messageString.startsWith("<"))
				messageString = messageString.substring(messageString.indexOf('=') + 1);

			return (RequestMessage) ElementState.translateFromXMLCharSequence(messageString,
					translationScope);
		}
		else
		{ // made of fail case
			return (RequestMessage) null;
		}
	}

	/**
	 * Adds the given request to this's request queue.
	 * 
	 * enqueueRequest(RequestMessage) is a hook method for ContextManagers that need to implement
	 * other functionality, such as prioritizing messages.
	 * 
	 * If enqueueRequest(RequestMessage) is overridden, the following methods should also be
	 * overridden: isMessageWaiting(), getNextRequest().
	 * 
	 * @param request
	 */
	protected void enqueueRequest(MessageWithMetadata<RequestMessage, Object> request)
	{
		messageWaiting = this.requestQueue.offer(request);
	}

	/**
	 * Returns the next message in the request queue.
	 * 
	 * getNextRequest() may be overridden to provide specific functionality, such as a priority queue.
	 * In this case, it is important to override the following methods: isMessageWaiting(),
	 * enqueueRequest().
	 * 
	 * @return the next message in the requestQueue.
	 */
	protected MessageWithMetadata<RequestMessage, Object> getNextRequest()
	{
		synchronized (requestQueue)
		{
			int queueSize = requestQueue.size();

			if (queueSize == 1)
			{
				messageWaiting = false;
			}

			// return null if none left, or the next Request otherwise
			return requestQueue.poll();
		}
	}

	/**
	 * Calls processRequest(RequestMessage) on the result of getNextRequest().
	 * 
	 * In order to override functionality processRequest(RequestMessage) and/or getNextRequest()
	 * should be overridden.
	 * 
	 */
	private final void processNextMessageAndSendResponse()
	{
		this.processRequest(this.getNextRequest());
	}

	/**
	 * Calls performService(requestMessage), then converts the resulting ResponseMessage into a
	 * String, adds the HTTP-like headers, and passes the completed String to the server backend for
	 * sending to the client.
	 * 
	 * @param request
	 *          - the request message to process.
	 */
	protected final ResponseMessage processRequest(
			MessageWithMetadata<RequestMessage, Object> requestWithMetadata)
	{
		RequestMessage request = requestWithMetadata.getMessage();

		ResponseMessage response = super.processRequest(request, ((SocketChannel) this.socketKey
				.channel()).socket().getInetAddress());

		if (response != null)
		{ // if the response is null, then we do
			// nothing else
			sendResponseToClient(requestWithMetadata, response, request);
		}
		else
		{
			debug("context manager did not produce a response message.");
		}

		requestWithMetadata = reqPool.release(requestWithMetadata);

		return response;
	}

	private synchronized void sendResponseToClient(
			MessageWithMetadata<RequestMessage, Object> requestWithMetadata, ResponseMessage response,
			RequestMessage request)
	{
		try
		{
			// setup outgoingMessageBuffer
			this.translateResponseMessageToStringBufferContents(request, response, msgBufOutgoing);
		}
		catch (SIMPLTranslationException e1)
		{
			e1.printStackTrace();
		}

		try
		{
			boolean usingCompression = this.availableEncodings.contains(HTTP_DEFLATE_ENCODING);
			/*
			 * If Compressing must know the length of the data being sent so must compress here
			 */
			if (usingCompression)
			{
				this.compressedMessageBuffer.clear();
				this.compress(msgBufOutgoing, this.compressedMessageBuffer);
				this.compressedMessageBuffer.flip();
				this.clearOutgoingMessageBuffer(msgBufOutgoing);
			}

			this.clearOutgoingMessageHeaderBuffer(headerBufOutgoing);

			// setup outgoingMessageHeaderBuffer
			this.createHeader((usingCompression) ? this.compressedMessageBuffer.limit() : msgBufOutgoing
					.length(), headerBufOutgoing, request, response, requestWithMetadata.getUid());

			if (usingCompression)
			{
				headerBufOutgoing.append(HTTP_HEADER_LINE_DELIMITER);
				headerBufOutgoing.append(HTTP_CONTENT_CODING);
				headerBufOutgoing.append(":");
				headerBufOutgoing.append(HTTP_DEFLATE_ENCODING);
			}

			headerBufOutgoing.append(HTTP_HEADER_TERMINATOR);

			// move the characters from the outgoing buffers into
			// outgoingChars using bulk get and put methods
			outgoingChars.clear();

			headerBufOutgoing.getChars(0, headerBufOutgoing.length(), outgoingChars.array(), 0);

			if (!usingCompression)
			{
				msgBufOutgoing.getChars(0, msgBufOutgoing.length(), outgoingChars.array(),
						headerBufOutgoing.length());

				outgoingChars.limit(headerBufOutgoing.length() + msgBufOutgoing.length());

				this.clearOutgoingMessageBuffer(msgBufOutgoing);
			}
			else
			{
				outgoingChars.limit(headerBufOutgoing.length());
			}

			outgoingChars.position(0);

			ByteBuffer outgoingBuffer = this.server.acquireByteBufferFromPool();

			synchronized (encoder)
			{
				encoder.reset();

				encoder.encode(outgoingChars, outgoingBuffer, true);

				encoder.flush(outgoingBuffer);
			}

			if (usingCompression)
			{
				outgoingBuffer.put(this.compressedMessageBuffer);
			}

			server.enqueueBytesForWriting(this.socketKey, outgoingBuffer);
		}
		catch (DataFormatException e)
		{
			debug("Failed to compress response!");
			e.printStackTrace();
		}
	}

	public synchronized void sendUpdateToClient(UpdateMessage<?> update)
	{
		if (this.isInvalidating())
		{
			return;
		}
		try
		{
			// setup outgoingMessageBuffer
			update.serialize(msgBufOutgoing);
		}
		catch (SIMPLTranslationException e1)
		{
			e1.printStackTrace();
		}

		try
		{
			boolean usingCompression = this.availableEncodings.contains(HTTP_DEFLATE_ENCODING);
			/*
			 * If Compressing must know the length of the data being sent so must compress here
			 */
			if (usingCompression)
			{
				this.compressedMessageBuffer.clear();
				this.compress(msgBufOutgoing, this.compressedMessageBuffer);
				this.compressedMessageBuffer.flip();
				this.clearOutgoingMessageBuffer(msgBufOutgoing);
			}

			this.clearOutgoingMessageHeaderBuffer(headerBufOutgoing);

			// setup outgoingMessageHeaderBuffer
			this.makeUpdateHeader((usingCompression) ? this.compressedMessageBuffer.limit()
					: msgBufOutgoing.length(), headerBufOutgoing, update);

			if (usingCompression)
			{
				headerBufOutgoing.append(HTTP_HEADER_LINE_DELIMITER);
				headerBufOutgoing.append(HTTP_CONTENT_CODING);
				headerBufOutgoing.append(":");
				headerBufOutgoing.append(HTTP_DEFLATE_ENCODING);
			}

			headerBufOutgoing.append(HTTP_HEADER_TERMINATOR);

			// move the characters from the outgoing buffers into
			// outgoingChars using bulk get and put methods
			outgoingChars.clear();

			headerBufOutgoing.getChars(0, headerBufOutgoing.length(), outgoingChars.array(), 0);

			if (!usingCompression)
			{
				msgBufOutgoing.getChars(0, msgBufOutgoing.length(), outgoingChars.array(),
						headerBufOutgoing.length());

				outgoingChars.limit(headerBufOutgoing.length() + msgBufOutgoing.length());

				this.clearOutgoingMessageBuffer(msgBufOutgoing);
			}
			else
			{
				outgoingChars.limit(headerBufOutgoing.length());
			}

			outgoingChars.position(0);

			ByteBuffer outgoingBuffer = this.server.acquireByteBufferFromPool();

			synchronized (encoder)
			{
				encoder.reset();

				encoder.encode(outgoingChars, outgoingBuffer, true);

				encoder.flush(outgoingBuffer);
			}

			if (usingCompression)
			{
				outgoingBuffer.put(this.compressedMessageBuffer);
			}

			server.enqueueBytesForWriting(this.socketKey, outgoingBuffer);
		}
		catch (DataFormatException e)
		{
			debug("Failed to compress update!");
			e.printStackTrace();
		}
	}

	private void compress(StringBuilder src, ByteBuffer dest) throws DataFormatException
	{
		synchronized (zippingChars)
		{
			zippingChars.clear();

			src.getChars(0, src.length(), this.zippingChars.array(), 0);
			zippingChars.position(0);
			zippingChars.limit(src.length());

			zippingInBytes.clear();

			encoder.reset();
			encoder.encode(zippingChars, zippingInBytes, true);
			encoder.flush(zippingInBytes);

			zippingInBytes.flip();

			deflater.reset();
			deflater.setInput(zippingInBytes.array(), zippingInBytes.position(), zippingInBytes.limit());
			deflater.finish();

			dest.position(dest.position()
					+ deflater.deflate(dest.array(), dest.position(), dest.remaining()));
		}
	}

	/**
	 * Takes an incoming message in the form of an XML String and converts it into a RequestMessage
	 * using translateStringToRequestMessage(String). Then places the RequestMessage on the
	 * requestQueue using enqueueRequest().
	 * 
	 * @param incomingMessage
	 * @param headerMap2
	 * @throws BadClientException
	 */
	private final void processString(CharSequence incomingMessage, long incomingUid)
			throws BadClientException
	{
		Exception failReason = null;
		RequestMessage request = null;
		try
		{
			request = this.translateStringToRequestMessage(incomingMessage);
		}
		catch (SIMPLTranslationException e)
		{
			// drop down to request == null, below
			failReason = e;
		}
		catch (UnsupportedEncodingException e)
		{
			// drop down to request == null, below
			failReason = e;
		}

		if (request == null)
		{
			if (incomingMessage.length() > 100)
			{
				debug("ERROR; incoming message could not be translated: " + incomingMessage.toString());

				debug("HEADERS:");
				debug(headerMap.toString());

				if (failReason != null)
				{
					debug("EXCEPTION: " + failReason.getMessage());
					failReason.printStackTrace();
				}
			}
			else
			{
				debug("ERROR; incoming message could not be translated: " + incomingMessage.toString());

				debug("HEADERS:");
				debug(headerMap.toString());

				if (failReason != null)
				{
					debug("EXCEPTION: " + failReason.getMessage());
					failReason.printStackTrace();
				}
			}
			if (++badTransmissionCount >= MAXIMUM_TRANSMISSION_ERRORS)
			{
				throw new BadClientException(((SocketChannel) this.socketKey.channel()).socket()
						.getInetAddress().getHostAddress(), "Too many Bad Transmissions: "
						+ badTransmissionCount);
			}
			// else
			error("translation failed: badTransmissionCount=" + badTransmissionCount);
		}
		else
		{
			badTransmissionCount = 0;

			MessageWithMetadata<RequestMessage, Object> pReq = this.reqPool.acquire();

			pReq.setMessage(request);
			pReq.setUid(incomingUid);

			synchronized (requestQueue)
			{
				this.enqueueRequest(pReq);
			}
		}
	}

	public SessionHandle getHandle()
	{
		return handle;
	}
}