package org.idempiere.dms;

import java.io.File;
import java.io.FileNotFoundException;

import org.adempiere.webui.window.FDialog;
import org.idempiere.model.MDMSContent;
import org.zkoss.util.media.AMedia;
import org.zkoss.zhtml.Filedownload;

/**
 * @author Sachin
 */
public class DMS_ZK_Util
{

	public static void downloadDocument(DMS dms, MDMSContent content) throws FileNotFoundException
	{
		File document = dms.getFileFromStorage(content);

		if (document.exists())
			downloadDocument(document);
		else
			FDialog.warn(0, "Document is not available.");
	} // downloadDocument

	public static void downloadDocument(File document) throws FileNotFoundException
	{
		AMedia media = new AMedia(document, "application/octet-stream", null);
		Filedownload.save(media);
	} // downloadDocument

}
