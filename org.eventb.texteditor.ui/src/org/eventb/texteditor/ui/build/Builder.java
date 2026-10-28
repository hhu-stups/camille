/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.build;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import org.eventb.texteditor.ui.TextEditorPlugin;
import org.eventb.texteditor.ui.build.ast.AstBuilder;
import org.eventb.texteditor.ui.build.dom.DomBuilder;
import org.eventb.texteditor.ui.editor.EventBTextEditor;

public class Builder {
	/**
	 * How long the UI thread waits for a running background build before it
	 * continues anyway. Keeps the UI responsive even if the background build
	 * is blocked (e.g. waiting for the workspace lock).
	 */
	private static final long UI_LOCK_TIMEOUT_MS = 2000;
	private static final long BACKGROUND_LOCK_TIMEOUT_MS = 30000;

	private final List<IBuildPhase> buildPhases = new ArrayList<IBuildPhase>();
	private boolean successful;

	public Builder() {
		buildPhases.add(new AstBuilder());
		buildPhases.add(new DomBuilder());
	}

	public void run(final EventBTextEditor editor, final IDocument document,
			final IProgressMonitor monitor) {
		successful = false;
		final Resource resource = editor.getResource();
		if (resource == null || document == null) {
			return;
		}

		final boolean isUIThread = Display.getCurrent() != null;
		final ReentrantLock lock = editor.getBuildLock();
		boolean locked;
		try {
			locked = lock.tryLock(isUIThread ? UI_LOCK_TIMEOUT_MS
					: BACKGROUND_LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			return;
		}

		if (!locked && !isUIThread) {
			// another build is still running, the next reconcile will retry
			return;
		}

		try {
			runPhases(editor, resource, document, monitor);
		} catch (final RuntimeException e) {
			/*
			 * Never let an exception escape into the reconciler thread. This
			 * would leave the editor without highlighting/outline updates
			 * until it is reopened.
			 */
			successful = false;
			TextEditorPlugin
					.getPlugin()
					.getLog()
					.log(new Status(IStatus.ERROR, TextEditorPlugin.PLUGIN_ID,
							"Building the Event-B text representation failed",
							e));
		} finally {
			if (locked) {
				lock.unlock();
			}
		}
	}

	private void runPhases(final EventBTextEditor editor,
			final Resource resource, final IDocument document,
			final IProgressMonitor monitor) {
		/*
		 * Work on a consistent copy of the document. The user may continue
		 * typing while we are parsing in the background.
		 */
		final IDocument snapshot = document instanceof DocumentSnapshot ? document
				: new DocumentSnapshot(document);

		final SubMonitor subMonitor = SubMonitor.convert(monitor, "Building '"
				+ resource.getURI().lastSegment() + "'...", buildPhases.size());
		successful = true;

		for (final IBuildPhase phase : buildPhases) {
			if (subMonitor.isCanceled()) {
				successful = false;
				return;
			}

			phase.run(editor, resource, snapshot, subMonitor.newChild(1));

			if (phase.canFail() && !phase.wasSuccessful()) {
				successful = false;
				return;
			}
		}
	}

	public boolean wasSuccessful() {
		return successful;
	}
}
