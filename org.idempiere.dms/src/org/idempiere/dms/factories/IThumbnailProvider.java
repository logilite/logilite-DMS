package org.idempiere.dms.factories;

import java.io.File;

import org.idempiere.model.I_DMS_Content;

/**
 * @author deepak@logilite.com
 */
public interface IThumbnailProvider
{
	public void init();

	public String getURL(I_DMS_Content content, String size);

	public File getFile(I_DMS_Content content, String size);

	public void addThumbnail(I_DMS_Content content, File file, String size);

}