/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.editor;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.texteditor.MarkerUtilities;
import org.eventb.texteditor.ui.TextEditorPlugin;
import org.rodinp.core.RodinMarkerUtil;

public class GotoMarker implements IGotoMarker {

	private final EventBTextEditor editor;

	public GotoMarker(final EventBTextEditor editor) {
		this.editor = editor;
	}

	public void gotoMarker(final IMarker marker) {
		/*
		 * Unfortunately the marker can already be gone, i.e., not exiting by
		 * the time we are called. This happens, for example, when the
		 * corresponding editor needs to opened before and starts an initial
		 * reconcile which replaces the markers.
		 */
		if (marker.exists()) {
			try {
				if (TextEditorPlugin.SYNTAXERROR_MARKER_ID.equals(marker.getType())) {
					final int charStart = MarkerUtilities.getCharStart(marker);
					final int charEnd = MarkerUtilities.getCharEnd(marker);

					editor.selectAndReveal(charStart, charEnd - charStart);
				} else if (RodinMarkerUtil.RODIN_PROBLEM_MARKER.equals(marker
						.getType())) {
					// TODO find a way to locate the problem in our text
				}
			} catch (final CoreException exception) {
				TextEditorPlugin.INSTANCE.log(exception);
			}
		}
	}
}
