package org.idempiere.dms.factories;

/**
 * @author deepak@logilite.com
 */
public interface IThumbnailGeneratorFactory {
	public IThumbnailGenerator get(String mimeType);
}
