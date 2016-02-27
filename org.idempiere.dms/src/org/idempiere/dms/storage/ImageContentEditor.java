package org.idempiere.dms.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.component.Panel;
import org.compiere.model.I_AD_StorageProvider;
import org.compiere.util.CLogger;
import org.idempiere.dms.factories.IContentEditor;
import org.idempiere.model.I_DMS_Content;
import org.zkoss.util.media.AMedia;
import org.zkoss.zul.Iframe;

public class ImageContentEditor extends Panel implements IContentEditor
{

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 7241323248511502223L;

	public static CLogger		log					= CLogger.getCLogger(ImageContentEditor.class);

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

	@Override
	public Panel initPanel()
	{
		this.setHeight("100%");
		this.setWidth("100%");

		Iframe iframeContentPriview = new Iframe();

		AMedia media = null;

		try
		{
			media = new AMedia(file, null, null);
		}
		catch (FileNotFoundException e)
		{
			log.log(Level.SEVERE, "Document cannot be displayed:" + e.getLocalizedMessage());
			throw new AdempiereException("Document cannot be displayed:" + e.getLocalizedMessage());
		}

		iframeContentPriview.setContent(media);
		iframeContentPriview.setWidth("100%");
		iframeContentPriview.setHeight("100%");
		iframeContentPriview.setStyle("overflow: auto;");

		this.appendChild(iframeContentPriview);

		return this;
	}

}
