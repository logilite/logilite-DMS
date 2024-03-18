package com.logilite.dms.factories;

import java.util.HashMap;

import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.Rows;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

import com.logilite.dms.DMS;
import com.logilite.dms.model.ContentDetail;
import com.logilite.dms.model.I_DMS_Association;
import com.logilite.dms.model.I_DMS_Version;

/**
 * Interface DMS Viewer
 * 
 * @author Sachin
 */
public interface IDMSViewer
{

	public void init(	DMS dms, HashMap<I_DMS_Version, I_DMS_Association> contentsMap, Grid grid, int compWidth,
						int compHeight, EventListener<? extends Event> listener, String[] eventsList);

	public void removeSelection(Component prevComponent);

	public void setSelection(Component component);

	public void setAttributesInRow(Component component, ContentDetail contentDetail);

	public void createComponent(Rows rows, ContentDetail contentDetail, int compWidth, int compHeight, boolean isFirstPage);

}
