package org.idempiere.dms.factories;

import java.io.File;

import org.idempiere.model.I_DMS_Content;


public interface IThumbnailProvider {
	public String getURL(I_DMS_Content content);
	public File getFile(I_DMS_Content content);
}
