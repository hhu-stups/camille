/**
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen,
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html)
 * */

package org.eventb.texteditor.ui.editor.proofstatus;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.part.FileEditorInput;
import org.eventb.core.EventBPlugin;
import org.eventb.core.IPSStatus;
import org.eventb.core.pm.IUserSupport;
import org.eventb.ui.EventBUIPlugin;
import org.eventb.texteditor.ui.TextEditorPlugin;
import org.rodinp.core.RodinDBException;

/**
 * Opens a proof obligation in Rodin's prover (Proving perspective), like a
 * double click on the proof obligation in the Event-B Explorer.
 */
public final class ProofObligationOpener {

	/** ID of Rodin's prover editor */
	private static final String PROVER_EDITOR_ID = "org.eventb.ui.editors.ProverUI";

	private ProofObligationOpener() {
		// utility class
	}

	/**
	 * Must be called in the UI thread.
	 */
	public static void open(final IPSStatus status) {
		final IWorkbench workbench = PlatformUI.getWorkbench();
		final IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		if (window == null) {
			return;
		}

		try {
			workbench.showPerspective(EventBUIPlugin.PROVING_PERSPECTIVE_ID,
					window);
		} catch (final WorkbenchException e) {
			// IGNORE, open the prover in the current perspective
		}

		final IWorkbenchPage page = window.getActivePage();
		final IFile psFile = status.getRodinFile().getResource();
		BusyIndicator.showWhile(window.getShell().getDisplay(), new Runnable() {
			@Override
			public void run() {
				try {
					page.openEditor(new FileEditorInput(psFile),
							PROVER_EDITOR_ID, true);
					for (final IUserSupport userSupport : EventBPlugin
							.getUserSupportManager().getUserSupports()) {
						if (status.getRoot().equals(userSupport.getInput())) {
							userSupport.setCurrentPO(status,
									new NullProgressMonitor());
							return;
						}
					}
				} catch (final RodinDBException e) {
					showError(window, e);
				} catch (final org.eclipse.ui.PartInitException e) {
					showError(window, e);
				}
			}
		});
	}

	private static void showError(final IWorkbenchWindow window,
			final Exception e) {
		TextEditorPlugin.getPlugin().getLog()
				.warn("Could not open the proof obligation", e);
		MessageDialog.openError(window.getShell(), "Open Proof Obligation",
				"Could not open the proof obligation: " + e.getMessage());
	}
}
