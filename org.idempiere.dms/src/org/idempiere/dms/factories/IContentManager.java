package org.idempiere.dms.factories;

import java.io.File;

import org.idempiere.model.I_DMS_Content;

public interface IContentManager {

	public void addBLOB(I_DMS_Content content, byte[] data);
	public void addFile(I_DMS_Content content, File file);
	public File getFile(I_DMS_Content content);
	public byte[] getBLOB(I_DMS_Content content);
}
