package ecologylab.services.nio;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.concurrent.LinkedBlockingQueue;

import ecologylab.appframework.ObjectRegistry;
import ecologylab.generic.Debug;
import ecologylab.services.BadClientException;
import ecologylab.services.ServerConstants;
import ecologylab.services.ServicesServer;
import ecologylab.services.messages.RequestMessage;
import ecologylab.services.messages.ResponseMessage;
import ecologylab.xml.ElementState;
import ecologylab.xml.TranslationSpace;
import ecologylab.xml.XmlTranslationException;

/**
 * Stores information about the connection context for the client on the server.
 * Should be extended for more specific implementations. Handles accumulating
 * incoming messages and translating them into RequestMessage objects, as well
 * as the ability to perform the messages' services and send their responses.
 * 
 * Generally, this class can be driven by one or more threads, depending on the
 * desired functionality.
 * 
 * @author Zach Toups
 */
public class ContextManager extends Debug implements ServerConstants
{
    private StringBuilder                         accumulator               = new StringBuilder(
                                                                                    MAX_PACKET_SIZE);

    protected boolean                             messageWaiting            = false;

    private RequestMessage                        request;

    protected LinkedBlockingQueue<RequestMessage> requestQueue              = new LinkedBlockingQueue<RequestMessage>();

    private Object                                token                     = null;

    private CharBuffer                            outgoingChars             = CharBuffer
                                                                                    .allocate(MAX_PACKET_SIZE);

    private final static CharsetEncoder           encoder                   = Charset
                                                                                    .forName(
                                                                                            CHARACTER_ENCODING)
                                                                                    .newEncoder();

    protected long                                initialTimeStamp          = System
                                                                                    .currentTimeMillis();

    protected boolean                             receivedAValidMsg;

    protected ObjectRegistry                      registry;

    private int                                   badTransmissionCount;

    NIOServerBackend                              server;

    protected SocketChannel                       socket;

    private int                                   endOfFirstHeader          = -1;

    private int                                   contentLength             = -1;

    StringBuilder                                 firstMessageInAccumulator = new StringBuilder();

    /**
     * Used to translate incoming message XML strings into RequestMessages.
     */
    private TranslationSpace                      translationSpace;

    public ContextManager(Object token, /* SelectionKey key, */
    NIOServerBackend server, SocketChannel socket,
            TranslationSpace translationSpace, ObjectRegistry registry)
    {
        this.token = token;

        this.socket = socket;
        this.server = server;
        // this.key = key;

        // channel = (SocketChannel) key.channel();
        //
        this.registry = registry;
        this.translationSpace = translationSpace;

        System.out.println("my server is: " + this.server);
    }

    /**
     * @return the next message in the requestQueue.
     */
    protected RequestMessage getNextMessage()
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
     * Calls performService on the given RequestMessage using the local
     * ObjectRegistry. Can be overridden by subclasses to provide more
     * specialized functionality.
     * 
     * @param requestMessage
     * @return
     */
    protected ResponseMessage performService(RequestMessage requestMessage)
    {
        requestMessage.setSender(this.socket.socket().getInetAddress());

        return requestMessage.performService(registry);
    }

    /**
     * Calls performService(requestMessage), then converts the resulting
     * ResponseMessage into a String, adds the HTTP-like headers, and passes the
     * final String to the server backend for sending to the client.
     * 
     * @param request
     */
    private void processRequest(RequestMessage request)
    {
        ResponseMessage response = null;

        if (request == null)
        {
            debug("No request.");
        }
        else
        {
            // perform the service being requested
            response = performService(request);

            if (response != null)
            { // if the response is null, then we do
                // nothing else
                try
                {
                    response.setUid(request.getUid());

                    String outgoingReq = response.translateToXML(false);
                    String header = "content-length:" + outgoingReq.length()
                            + "\r\n\r\n";

                    outgoingChars.clear();
                    outgoingChars.put(header).put(outgoingReq);
                    outgoingChars.flip();

                    ByteBuffer temp = encoder.encode(outgoingChars);

                    server.send(this.socket, temp);
                }
                catch (XmlTranslationException e)
                {
                    e.printStackTrace();
                }
                catch (CharacterCodingException e)
                {
                    e.printStackTrace();
                }
            }

        }
    }

    public void processNextMessageAndSendResponse()
    {
        this.processRequest(this.getNextMessage());
    }

    /**
     * Calls processNextMessageAndSendResponse() on each queued message.
     * 
     * Can be overridden for more specific functionality.
     * 
     * @throws BadClientException
     */
    public void processAllMessagesAndSendResponses() throws BadClientException
    {
        timeoutBeforeValidMsg();
        while (isMessageWaiting())
        {
            this.processNextMessageAndSendResponse();
            timeoutBeforeValidMsg();
        }
    }

    /**
     * Checks the time since the last valid message. If the time has been too
     * long (MAX_TIME_BEFORE_VALID_MSG), throws a BadClientException, which the
     * server will deal with.
     * 
     * If the time has not been too long, updates the time stamp.
     * 
     * @throws BadClientException
     */
    void timeoutBeforeValidMsg() throws BadClientException
    {
        long now = System.currentTimeMillis();
        long elapsedTime = now - this.initialTimeStamp;
        if (elapsedTime >= MAX_TIME_BEFORE_VALID_MSG)
        {
            throw new BadClientException(this.socket.socket().getInetAddress()
                    .getHostAddress(),
                    "Too long before valid response: elapsedTime="
                            + elapsedTime + ".");
        }
        else
        {
            this.initialTimeStamp = now;
        }
    }

    /**
     * @return Returns the token.
     */
    public Object getToken()
    {
        return token;
    }

    /**
     * @return Returns the messageWaiting.
     */
    public boolean isMessageWaiting()
    {
        return messageWaiting;
    }

    /**
     * Hook method to provide specific functionality.
     * 
     * @param messageString
     * @return
     * @throws XmlTranslationException
     */
    protected RequestMessage translateXMLStringToRequestMessage(String messageString)
    throws XmlTranslationException
    {
        return (RequestMessage) ElementState.translateFromXMLString(
                messageString, translationSpace);
    }
    
    /**
     * Takes an incoming message in the form of an XML String and converts it
     * into a RequestMessage. Then places the RequestMessage on the
     * requestQueue.
     * 
     * @param incomingMessage
     * @throws BadClientException
     */
    private void processString(String incomingMessage)
            throws BadClientException
    {
        if (show(5))
        {
            debug("processing: " + incomingMessage);
            debug("translationSpace: " + translationSpace.toString());
        }

        request = null;
        try
        {
            request = this.translateXMLStringToRequestMessage(incomingMessage);
        }
        catch (XmlTranslationException e)
        {
            // drop down to request == null, below
        }

        if (request == null)
        {
            System.out.println("ERROR: " + incomingMessage);
            if (++badTransmissionCount >= MAXIMUM_TRANSMISSION_ERRORS)
            {
                throw new BadClientException(this.socket.socket()
                        .getInetAddress().getHostAddress(),
                        "Too many Bad Transmissions: " + badTransmissionCount);
            }
            // else
            error("ERROR: translation failed: badTransmissionCount="
                    + badTransmissionCount);
        }
        else
        {
            receivedAValidMsg = true;
            badTransmissionCount = 0;

            synchronized (requestQueue)
            {
                this.enqueueRequest(request);
            }
        }
    }

    /**
     * Adds the given request to this's request queue.
     * 
     * enqueueRequest(RequestMessage) is a hook method for ContextManagers that
     * need to implement other functionality, such as prioritizing messages.
     * 
     * @param request
     */
    protected void enqueueRequest(RequestMessage request)
    {
        if (requestQueue.offer(request))
        {
            messageWaiting = true;
        }
    }

    /**
     * Converts the given bytes into chars, then extracts any messages from the
     * chars and enqueues them.
     * 
     * @param message
     */
    public void enqueueStringMessage(CharBuffer message)
            throws CharacterCodingException, BadClientException
    {
        accumulator.append(message);

        // System.out.println("accum: "+accumulator.toString());

        // look for HTTP header
        while (accumulator.length() > 0)
        {
            if (endOfFirstHeader == -1)
                endOfFirstHeader = accumulator.indexOf("\r\n\r\n");

            if (endOfFirstHeader == -1)
            { // no header yet; if it's too large,
                // bad client; if it's not too large
                // yet, just exit
                if (accumulator.length() > ServerConstants.MAX_HTTP_HEADER_LENGTH)
                {
                    throw new BadClientException(this.socket.socket()
                            .getInetAddress().getHostAddress(),
                            "Maximum HTTP header length exceeded.");
                }

                break;
            }

            if (contentLength == -1)
            {
                try
                {
                    contentLength = ServicesServer.parseHeader(accumulator
                            .substring(0, endOfFirstHeader));
                }
                catch (IllegalStateException e)
                {
                    throw new BadClientException(this.socket.socket()
                            .getInetAddress().getHostAddress(),
                            "Malformed header.");
                }
            }

            if (contentLength == -1)
                break;

            // make sure contentLength isn't too big
            if (contentLength > ServerConstants.MAX_PACKET_SIZE)
            {
                throw new BadClientException(this.socket.socket()
                        .getInetAddress().getHostAddress(),
                        "Specified content length too large: " + contentLength);
            }

            try
            {
                // if we got here, endOfFirstHeader is not -1, so we need to add
                // 4 to it to ensure we're just after all the header
                endOfFirstHeader += 4;

                firstMessageInAccumulator.append(accumulator.substring(
                        endOfFirstHeader, endOfFirstHeader + contentLength));
                accumulator.delete(0, endOfFirstHeader + contentLength);
                endOfFirstHeader = -1;
                contentLength = -1;
                // System.out.println(firstMessage);
            }
            catch (NullPointerException e)
            {
                e.printStackTrace();
            }
            catch (IndexOutOfBoundsException e)
            {
                debug("don't have a complete message yet.");
                // append what we do have, then continue
                endOfFirstHeader = 0; // we already finished with the header
                String partOfMessage = accumulator.substring(endOfFirstHeader,
                        accumulator.length());
                contentLength -= partOfMessage.length();

                accumulator.delete(0, accumulator.length() - 1);

                e.printStackTrace();
                break;
            }

            if (firstMessageInAccumulator != null)
            {
                processString(firstMessageInAccumulator.toString());
                firstMessageInAccumulator.delete(0, firstMessageInAccumulator
                        .length());
            }
        }
    }

    /**
     * Hook method for having shutdown behavior.
     * 
     * This method is called whenever the client terminates their connection or
     * when the server is shutting down.
     */
    public void shutdown()
    {

    }
}
