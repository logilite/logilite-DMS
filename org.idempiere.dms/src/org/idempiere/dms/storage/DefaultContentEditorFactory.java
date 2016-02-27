package org.idempiere.dms.storage;

import org.idempiere.dms.factories.IContentEditor;
import org.idempiere.dms.factories.IContentEditorFactory;

public class DefaultContentEditorFactory implements IContentEditorFactory
{

	@Override
	public IContentEditor get(String mimeType)
	{
		if (mimeType.equalsIgnoreCase("application/pdf"))
		{
			return new PDFContentEditor();
		}
		else if (mimeType.startsWith("image/"))
		{
			return new ImageContentEditor();
		}
		return null;
	}

}
