package org.idempiere.dms.factories;

import java.io.File;
import java.util.ArrayList;

import org.compiere.model.I_AD_StorageProvider;

/**
 * @author deepak@logilite.com
 * 
 */
public interface IThumbnailGenerator {
	public void init(I_AD_StorageProvider storageProvider);
	public ArrayList<File> getThumbnails(File document);
	
}
