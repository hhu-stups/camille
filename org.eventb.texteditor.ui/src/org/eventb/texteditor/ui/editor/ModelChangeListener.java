/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.editor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;

public class ModelChangeListener implements IResourceChangeListener,
		IResourceDeltaVisitor {

	private final EventBTextEditor editor;

	public ModelChangeListener(final EventBTextEditor editor) {
		this.editor = editor;
	}

	/*
	 * @see IResourceChangeListener#resourceChanged(IResourceChangeEvent)
	 */
	public void resourceChanged(final IResourceChangeEvent e) {
		final IResourceDelta delta = e.getDelta();
		try {
			if (delta != null) {
				delta.accept(this);
			}
		} catch (final CoreException x) {
			// TODO log exception
		}
	}

	/*
	 * @see
	 * IResourceDeltaVisitor#visit(org.eclipse.core.resources.IResourceDelta)
	 */
	public boolean visit(IResourceDelta delta) throws CoreException {
		if (delta == null) {
			return false;
		}

		final IFile file = getFile();
		if (file == null) {
			return false;
		}

		delta = delta.findMember(file.getFullPath());

		if (delta == null) {
			return false;
		}

		if (delta.getKind() == IResourceDelta.CHANGED) {
			// is change affecting content?
			if ((IResourceDelta.CONTENT & delta.getFlags()) != 0) {
				// has editor unsaved changes?
				if (editor.isDirty()) {
					// notify editor about change
					editor.changeWhileDirty();
				}
			}
		}

		return false;
	}

	/**
	 * Computes the initial modification stamp for the given resource.
	 * 
	 * @param resource
	 *            the resource
	 * @return the modification stamp
	 */
	protected long computeModificationStamp(final IResource resource) {
		long modificationStamp = resource.getModificationStamp();

		final IPath path = resource.getLocation();
		if (path == null) {
			return modificationStamp;
		}

		modificationStamp = path.toFile().lastModified();
		return modificationStamp;
	}

	private IFile getFile() {
		final IEditorInput editorInput = editor.getEditorInput();

		if (editorInput instanceof IFileEditorInput) {
			return ((IFileEditorInput) editorInput).getFile();
		}

		return null;
	}
}
