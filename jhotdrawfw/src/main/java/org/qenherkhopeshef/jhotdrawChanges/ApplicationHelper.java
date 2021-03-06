package org.qenherkhopeshef.jhotdrawChanges;

import org.jhotdraw_7_6.app.Application;
import org.jhotdraw_7_6.app.View;

public class ApplicationHelper {

	static public View findEmptyView(final Application app) {
		View emptyView = app.getActiveView();
		if (emptyView == null || emptyView.getURI() != null
				|| emptyView.hasUnsavedChanges() || !emptyView.isEnabled()) {
			emptyView = null;
		}
	
		final View view;
		//boolean disposeView;
		if (emptyView == null) {
			view = app.createView();
			app.add(view);
			//disposeView = true;
		} else {
			view = emptyView;
			//disposeView = false;
		}
		return view;
	}

}
