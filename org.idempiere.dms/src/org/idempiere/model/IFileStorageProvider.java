package org.idempiere.model;

import java.io.File;

import org.compiere.model.I_AD_StorageProvider;

/**
 * @author Deepak@logilite.com
 */
public interface IFileStorageProvider
{
	public void init(I_AD_StorageProvider storageProvider);

	public File[] getFiles(String parent, String pattern);

	public File getFile(String path);

	public String[] list(String parent);

	public byte[] getBLOB(String path);

	public boolean writeBLOB(String path, byte[] data);

	public String getBaseDirectory(String path);
}
