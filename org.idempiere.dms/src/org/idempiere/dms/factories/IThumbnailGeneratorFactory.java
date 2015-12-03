package org.idempiere.dms.factories;

public interface IThumbnailGeneratorFactory {
	public IThumbnailGenerator get(String mimeType);
}
