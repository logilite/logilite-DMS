package org.idempiere.dms.factories;

import java.util.HashMap;

import org.adempiere.webui.component.Grid;
import org.idempiere.dms.DMS;
import org.idempiere.model.I_DMS_Association;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.I_DMS_Version;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

/**
 * Interface DMS Viewer
 * 
 * @author Sachin
 */
public interface IDMSViewer
{

	public String getContentName(I_DMS_Content content, int version);

	public void init(	DMS dms, HashMap<I_DMS_Version, I_DMS_Association> contentsMap, Grid grid, int compWidth, int compHeight,
						EventListener<? extends Event> listener, String[] eventsList);

	public void removeSelection(Component prevComponent);

	public void setSelection(Component component);
}
