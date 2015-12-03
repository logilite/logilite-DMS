package org.idempiere.dms.factories;

import java.io.File;
import java.util.ArrayList;


public interface IThumbnailGenerator {
	public ArrayList<File> getThumbnails(File document);
}
