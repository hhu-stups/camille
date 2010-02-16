/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui;

import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.edit.ui.EMFEditUIPlugin;
import org.eclipse.emf.edit.ui.util.EditUIMarkerHelper;
import org.eclipse.ui.texteditor.MarkerUtilities;
import org.eventb.texteditor.ui.build.ast.IParseProblemWrapper;

public class MarkerHelper extends EditUIMarkerHelper {

	private final String markerId;

	public MarkerHelper(final String markerId) {
		this.markerId = markerId;

	}

	@Override
	protected String getMarkerID() {
		return markerId;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected boolean adjustMarker(final IMarker marker,
			final Diagnostic diagnostic) throws CoreException {
		if (diagnostic.getData() != null) {
			for (final Object element : diagnostic.getData()) {
				if (element instanceof IParseProblemWrapper) {
					final IParseProblemWrapper parseProblem = (IParseProblemWrapper) element;
					final int offset = parseProblem.getOffset();
					final int line = parseProblem.getLine();
					final int column = parseProblem.getColumn();

					// Setting attributes for marker
					final Map attributes = marker.getAttributes();

					MarkerUtilities.setCharStart(attributes, offset);
					MarkerUtilities.setCharEnd(attributes, offset
							+ parseProblem.getTokenLength());
					MarkerUtilities.setMessage(attributes, parseProblem
							.getMessage());
					MarkerUtilities.setLineNumber(attributes, line);

					attributes.put(IMarker.LOCATION, EMFEditUIPlugin
							.getPlugin().getString(
									"_UI_MarkerLocation",
									new String[] { Integer.toString(line + 1),
											Integer.toString(column + 1) }));

					marker.setAttributes(attributes);
					return true;
				}
			}
		}

		return super.adjustMarker(marker, diagnostic);
	}

	public void deleteMarkers(final Resource resource, final String markerType,
			final boolean includeSubtypes, final int depth) {
		final IFile file = getFile(resource);
		try {
			file.deleteMarkers(markerType, includeSubtypes, depth);
		} catch (final CoreException e) {
			// IGNORE
		}
	}
}
