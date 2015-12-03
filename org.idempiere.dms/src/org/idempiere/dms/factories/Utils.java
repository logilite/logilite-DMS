package org.idempiere.dms.factories;

import java.util.List;

import org.adempiere.base.Service;

/**
 * @author deepak@logilite.com
 * 
 */
public class Utils {

	public static IContentEditor getContentEditor(String mimeType){
		List<IContentEditorFactory> factories = Service.locator().list(IContentEditorFactory.class).getServices();
		IContentEditor editor = null;
		for(IContentEditorFactory factory:factories){
			editor = factory.get(mimeType);
			if(editor!=null)
				break;
		}
		return editor;
	}
	
	public static IThumbnailGenerator getThumbnailGenerator(String mimeType){
		List<IThumbnailGeneratorFactory> factories = Service.locator().list(IThumbnailGeneratorFactory.class).getServices();
		IThumbnailGenerator tGenerator = null;
		for(IThumbnailGeneratorFactory factory:factories){
			tGenerator = factory.get(mimeType);
			if(tGenerator!=null)
				break;
		}
		return tGenerator;
	}
	
	
}
