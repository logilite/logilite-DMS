package org.idempiere.dms.factories;

import java.io.File;

import org.adempiere.webui.component.Panel;
import org.compiere.model.I_AD_StorageProvider;
import org.idempiere.model.I_DMS_Content;

/**
 * @author deepak@logilite.com
 * Interface to preview content on content viewer tab
 */
public interface IContentEditor {
	public void setFile(File file);
	public void setContent(I_DMS_Content content);
	void init(I_AD_StorageProvider storageProvider);
	public Panel initPanel();

}
