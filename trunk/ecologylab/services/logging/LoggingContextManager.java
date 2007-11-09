/**
 * 
 */
package ecologylab.services.logging;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import ecologylab.appframework.ObjectRegistry;
import ecologylab.services.ServerConstants;
import ecologylab.services.messages.InitConnectionRequest;
import ecologylab.services.messages.RequestMessage;
import ecologylab.services.messages.ResponseMessage;
import ecologylab.services.nio.NIOServerBackend;
import ecologylab.services.nio.contextmanager.ContextManager;
import ecologylab.xml.TranslationSpace;
import ecologylab.xml.XMLTranslationException;

/**
 * Special version of ServerToClientConnection for the LoggingServer. Provides a
 * special impleentation of performService(), that open's an OutputStream as
 * necessary to the appropriate directory for logging, based on the headers in
 * the message, then logs the message to there with a minimum of translation.
 * 
 * @author andruid
 * @author eunyee
 * @author toupsz
 */
public class LoggingContextManager extends ContextManager
{

    OutputStreamWriter	outputStreamWriter;

    NIOLoggingServer 	loggingServer;

    boolean          	end = false;

    /**
     * @param token
     * @param loggingServer
     * @param server
     * @param socket
     * @param translationSpace
     * @param registry
     */
    public LoggingContextManager(Object token, int maxPacketSize,
            NIOLoggingServer loggingServer, NIOServerBackend server,
            SocketChannel socket, TranslationSpace translationSpace,
            ObjectRegistry registry)
    {
        super(token, maxPacketSize, server, loggingServer, socket,
                translationSpace, registry);

        this.loggingServer = loggingServer;
    }

    @Override protected ResponseMessage performService(
            RequestMessage requestMessage)
    {
        ResponseMessage responseMessage;
        if (requestMessage instanceof InitConnectionRequest)
        {
            responseMessage = super.performService(requestMessage);
        }
        else
        {
            if (requestMessage instanceof SendPrologue)
            {
                String name = loggingServer.getLogFilesPath()
                        + ((SendPrologue) requestMessage).getFileName();
                getOutputStreamWriter(name);
                // servicesServer.getObjectRegistry().registerObject(LoggingDef.keyStringForFileObject,
                // getFile(name) );
            }
            else if (outputStreamWriter == null)
            {
                debug("Prologue has not been received OR File has not been created!! "
                        + requestMessage);
            }

            if ((outputStreamWriter != null)
                    && (requestMessage instanceof LogRequestMessage))
            {
                ((LogRequestMessage) requestMessage).setWriter(outputStreamWriter);
            }

            responseMessage = super.performService(requestMessage);

            if (requestMessage instanceof SendEpilogue)
            {
                debug("received epiliogue, set end to true");
                try
				{
					outputStreamWriter.close();
				} catch (IOException e)
				{
					e.printStackTrace();
				}
                end = true;
            }
            else
            {
                requestMessage.getClass();
            }
        }
        return responseMessage;
    }
//    private static final CharsetEncoder     ENCODER                     = Charset.forName(ServerConstants.CHARACTER_ENCODING).newEncoder();

    protected OutputStreamWriter getOutputStreamWriter(String fileName)
    {
        if (outputStreamWriter == null)
        {
            try
            {
                File file = new File(fileName);
                String dirPath = file.getParent();
                if (dirPath != null)
                {
                    File dir = new File(dirPath);
                    if (!dir.exists())
                        dir.mkdirs();
                }
                
                debug("logging to file at: "+file.getAbsolutePath());
                
                // TODO what if (file.exists()) ???
                FileOutputStream fos		= new FileOutputStream(file, true);
                CharsetEncoder encoder      = Charset.forName(ServerConstants.CHARACTER_ENCODING).newEncoder();
                outputStreamWriter			= new OutputStreamWriter(fos, encoder);
                return outputStreamWriter;
            }
            catch (FileNotFoundException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return outputStreamWriter;
    }

    /**
     * Use the ServicesServer and its ObjectRegistry to do the translation. Put
     * a copy of the xmlString into the LoggingRequestMessage. Do not
     * doRecursiveDescent when translating the message, since we're logging, so
     * all we need is to copy the String to the appropriate log file.
     * 
     * @param messageString
     * @return
     * @throws XMLTranslationException
     */
//    protected RequestMessage translateStringToRequestMessage(
//            String messageString) throws XmlTranslationException
//    {
//        RequestMessage requestMessage = (RequestMessage) ElementState
//                .translateFromXMLString(messageString, translationSpace, false);
//
//        if (requestMessage instanceof LogRequestMessage)
//        { // special processing on log messages
//            LogRequestMessage lrm = (LogRequestMessage) requestMessage;
//            lrm.setXmlString(messageString);
//        }
//        else if (!(requestMessage instanceof InitConnectionRequest))
//        { // if not log message or connection initialization, bad things
//            throw new XmlTranslationException(
//                    "LoggingServer received non logging message: "
//                            + requestMessage);
//        }
//
//        return requestMessage;
//    }

    @Override public void shutdown()
    {
        while (this.messageWaiting || this.requestQueue.size() > 0)
        {
            try
            {
                wait(100);
            }
            catch (InterruptedException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        if (!end)
        {
            SendEpilogue sE = new SendEpilogue();
            sE.setWriter(outputStreamWriter);
            sE.performService(registry);
        }

        super.shutdown();
    }
}
