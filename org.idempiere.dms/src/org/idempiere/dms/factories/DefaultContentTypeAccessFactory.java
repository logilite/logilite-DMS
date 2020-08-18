package org.idempiere.dms.factories;

public class DefaultContentTypeAccessFactory implements IContentTypeAccessFactory
{

	@Override
	public IContentTypeAccess get( )
	{
		return new DefaultContentTypeAccessFiltered();
	}

}
