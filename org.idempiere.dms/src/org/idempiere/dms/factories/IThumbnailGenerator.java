package org.idempiere.dms.factories;

import java.io.File;

import org.idempiere.model.I_DMS_Content;

/**
 * @author deepak@logilite.com
 */
public interface IThumbnailGenerator
{
	public void init();

	public void addThumbnail(I_DMS_Content content, File file, String size);

}