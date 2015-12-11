package org.idempiere.dms.storage;

import java.io.File;
import java.io.IOException;

import org.compiere.model.I_AD_StorageProvider;
import org.idempiere.model.IFileStorageProvider;

public class FileSystemStorageProvider implements IFileStorageProvider{
	I_AD_StorageProvider provider;
	String baseDir;
	@Override
	public void init(I_AD_StorageProvider storageProvider) {
		provider = storageProvider;
		baseDir = storageProvider.getFolder();
	}

	@Override
	public File[] getFiles(String parent, String pattern) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public File getFile(String path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] list(String parent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] getBLOB(String path) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean writeBLOB(String path, byte[] data) {
		// TODO Auto-generated method stub
		return false;
	}

}
