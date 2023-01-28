package com.logilite.dms.factories;

/**
 * Interface DMS Viewer Factory
 * 
 * @author Sachin
 */
public interface IDMSViewerFactory
{
	public IDMSViewer get(String toggleAction);
}
