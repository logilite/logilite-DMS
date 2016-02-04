package org.idempiere.dms.storage;

import org.idempiere.dms.factories.IThumbnailGenerator;
import org.idempiere.dms.factories.IThumbnailGeneratorFactory;

public class DefaultThumbnailGeneratorFactory implements IThumbnailGeneratorFactory
{

	@Override
	public IThumbnailGenerator get(String mimeType)
	{
		if (mimeType.equalsIgnoreCase("application/pdf"))
		{
			return new ThumbnailGenerator();
		}
		else if (mimeType.startsWith("imeges/"))
		{
			return new ThumbnailGenerator();
		}
		return null;
	}

}
