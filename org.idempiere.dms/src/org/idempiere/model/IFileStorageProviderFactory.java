package org.idempiere.model;

/**
 * 
 * @author Deepak@logilite.com
 *
 */
public interface IFileStorageProviderFactory {
	public IFileStorageProvider get(String type);
}
