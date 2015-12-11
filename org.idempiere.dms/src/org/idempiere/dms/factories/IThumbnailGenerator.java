package org.idempiere.dms.factories;

import java.io.File;
import java.util.ArrayList;

/**
 * @author deepak@logilite.com
 * 
 */
public interface IThumbnailGenerator {
	public ArrayList<File> getThumbnails(File document);
}
