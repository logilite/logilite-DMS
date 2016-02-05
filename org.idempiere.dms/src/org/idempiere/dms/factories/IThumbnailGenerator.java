package org.idempiere.dms.factories;

import java.io.File;
import java.util.ArrayList;

import org.idempiere.model.I_DMS_Content;

/**
 * @author deepak@logilite.com
 */
public interface IThumbnailGenerator
{
	public void init();

	public ArrayList<File> getThumbnails(File document, I_DMS_Content dms_content);

}