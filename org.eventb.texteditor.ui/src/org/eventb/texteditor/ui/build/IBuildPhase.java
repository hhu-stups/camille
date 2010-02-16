/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.build;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.text.IDocument;
import org.eventb.texteditor.ui.editor.EventBTextEditor;

public interface IBuildPhase {
	/**
	 * Returns whether this phase involves UI related work.
	 * 
	 * @return
	 */
	public boolean isUIPhase();

	/**
	 * Can this build phase fail? If <code>true</code> the method
	 * {@link #wasSuccessful()} might be called after running
	 * {@link #run(IProgressMonitor)}.
	 * 
	 * @see #wasSuccessful()
	 * @return
	 */
	public boolean canFail();

	/**
	 * Has the call of {@link #run(IProgressMonitor)}, i.e. the run of this
	 * build phase been successful? It only makes sense to return
	 * <code>false</code> if {@link #canFail()} returned <code>true</code>,
	 * otherwise the result will be ignored anyway.
	 * 
	 * @see #canFail()
	 * @return
	 */
	public boolean wasSuccessful();

	/**
	 * Run this build phase.
	 * 
	 * @param monitor
	 */
	public void run(final EventBTextEditor editor, final Resource resource,
			final IDocument document, final IProgressMonitor monitor);
}
