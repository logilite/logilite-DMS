package org.idempiere.dms.storage;

import java.io.File;

import org.compiere.model.I_AD_StorageProvider;
import org.idempiere.dms.factories.IContentEditor;
import org.idempiere.model.I_DMS_Content;

public class PDFContentEditor implements IContentEditor
{

	/**
	 * 
	 */
	@SuppressWarnings("unused")
	private static final long	serialVersionUID	= 3166734130983495805L;

	File						file				= null;
	I_DMS_Content				content				= null;
	I_AD_StorageProvider		provider;
	String						baseDir;

	@Override
	public void setFile(File file)
	{
		this.file = file;
	}

	@Override
	public void setContent(I_DMS_Content content)
	{
		this.content = content;
	}

	@Override
	public void init(I_AD_StorageProvider storageProvider)
	{
		provider = storageProvider;
		baseDir = storageProvider.getFolder();
	}

}
