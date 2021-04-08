package org.idempiere.dms.factories;

import org.idempiere.dms.DMSPermission;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.MDMSPermission;

public interface IPermission
{
	public MDMSPermission createPermission(DMSPermission permission, MDMSContent content, boolean isCreateForSubContent);

	public MDMSPermission createContentPermission(MDMSContent content);

	public boolean validateContentPermission(MDMSContent content);

	public void initContentPermission(MDMSContent content);

	public boolean isRead( );

	public boolean isWrite( );

	public boolean isDelete( );

	public boolean isNavigation( );

	public boolean isAllPermission( );

}
