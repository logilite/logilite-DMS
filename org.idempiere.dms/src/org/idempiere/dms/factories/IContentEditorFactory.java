package org.idempiere.dms.factories;

/**
 * @author deepak@logilite.com
 * 
 */
public interface IContentEditorFactory {
	public IContentEditor get(String mimeType);
}
