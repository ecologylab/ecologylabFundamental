package ecologylab.services.messages;

/**
 * The ResponseMessage send from server to client when the RequestMessage
 * is well-formed, but doesn't make sense in the current context.
 * 
 * @author andruid
 * @author blake
 * @author eunyee
 */
public class BadSemanticContentResponse extends ResponseMessage
{

	public BadSemanticContentResponse()
	{
		super();
	}

	public BadSemanticContentResponse(String response)
	{
		super(response);
	}

}
