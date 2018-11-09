/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.outline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eventb.emf.core.EventBObject;
import org.eventb.emf.core.context.Context;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Machine;
import org.eventb.texteditor.ui.TextEditorPlugin;
import org.eventb.texteditor.ui.build.dom.DomManager;
import org.eventb.texteditor.ui.build.dom.IParseResultListener;
import org.eventb.texteditor.ui.build.dom.DomManager.ParseResult;

/**
 * {@link ITreeContentProvider} which can handle {@link IEditorInput}s as input.
 */
public class ContentProvider implements ITreeContentProvider,
		IParseResultListener {

	private final DomManager domManager = TextEditorPlugin.getDomManager();

	private static final Object[] NO_ELEMENTS = new Object[] {};

	/**
	 * Cache for children of a machine
	 */
	private Object[] machineChildren;

	/**
	 * Cache for children of a context
	 */
	private Object[] contextChildren;

	/**
	 * Caches for children of events
	 */
	private final Map<Event, Object[]> eventChildren = new HashMap<Event, Object[]>();

	private TreeViewer viewer;

	private Object currentInput;

	synchronized public Object[] getElements(final Object inputElement) {
		if (inputElement instanceof IEditorInput) {
			// show root element based on last successful parse result
			final ParseResult lastParseResult = domManager
					.getLastParseResult((IEditorInput) inputElement);

			if (lastParseResult != null) {
				return new Object[] { lastParseResult.astRoot };
			} else if (inputElement instanceof IFileEditorInput) {
				final IFile inputFile = ((IFileEditorInput) inputElement)
						.getFile();
				return createFallbackStructure(inputFile);
			}
		}

		return NO_ELEMENTS;
	}

	synchronized public Object[] getChildren(final Object parentElement) {
		if (parentElement instanceof Machine) {
			return internalGetChildren((Machine) parentElement);
		}

		if (parentElement instanceof Context) {
			return internalGetChildren((Context) parentElement);
		}

		if (parentElement instanceof Event) {
			return internalGetChildren((Event) parentElement);
		}

		// no children for unknown parents
		return NO_ELEMENTS;
	}

	synchronized public Object getParent(final Object element) {
		// node is one of our emf classes
		if (element instanceof EventBObject) {
			final EventBObject emfObject = (EventBObject) element;
			return emfObject.eContainer();
		}

		// we cannot compute the parent
		return null;
	}

	synchronized public boolean hasChildren(final Object element) {
		int count = 0;

		if (element instanceof Machine) {
			count = internalGetChildren((Machine) element).length;
		}

		if (element instanceof Context) {
			count = internalGetChildren((Context) element).length;
		}

		if (element instanceof Event) {
			count = internalGetChildren((Event) element).length;
		}

		return count > 0;
	}

	synchronized public void inputChanged(final Viewer viewer,
			final Object oldInput, final Object newInput) {
		registerLister(newInput);

		if (viewer instanceof TreeViewer) {
			this.viewer = (TreeViewer) viewer;
		}

		clearCache();
		currentInput = newInput;
	}

	synchronized public void dispose() {
		registerLister(null);
		viewer = null;
		clearCache();
	}

	private void clearCache() {
		machineChildren = null;
		contextChildren = null;
		eventChildren.clear();
	}

	private void registerLister(final Object newInput) {
		if (currentInput != null && currentInput instanceof IEditorInput) {
			domManager.removeParseResultListener((IEditorInput) currentInput,
					this);
		}

		if (newInput != null && newInput instanceof IEditorInput) {
			domManager.addParseResultListener((IEditorInput) newInput, this);
		}
	}

	private Object[] createFallbackStructure(final IFile inputFile) {
		/*
		 * TODO Use fallback strategy if no AST available, for example scan text
		 * for some keywords and use them to offer quick navigation by clicking
		 */
		return NO_ELEMENTS;
	}

	private Object[] internalGetChildren(final Machine machine) {
		if (machineChildren == null) {
			final ArrayList<EventBObject> children = new ArrayList<EventBObject>();
			children.addAll(machine.getVariables());
			children.addAll(machine.getInvariants());
			children.addAll(machine.getVariants());
			children.addAll(machine.getEvents());

			machineChildren = children.toArray();
		}

		return machineChildren;
	}

	private Object[] internalGetChildren(final Context context) {
		if (contextChildren == null) {
			final ArrayList<EventBObject> children = new ArrayList<EventBObject>();
			children.addAll(context.getConstants());
			children.addAll(context.getAxioms());
			children.addAll(context.getSets());

			contextChildren = children.toArray();
		}

		return contextChildren;
	}

	private Object[] internalGetChildren(final Event event) {
		if (!eventChildren.containsKey(event)) {
			final ArrayList<EventBObject> children = new ArrayList<EventBObject>();
			children.addAll(event.getWitnesses());
			children.addAll(event.getGuards());
			children.addAll(event.getActions());

			eventChildren.put(event, children.toArray());
		}

		return eventChildren.get(event);
	}

	private void refresh() {
		if (viewer != null && viewer.getControl() != null
				&& !viewer.getControl().isDisposed()) {
			viewer.refresh();
			viewer.expandAll();
		}
	}

	public void parseResultChanged(final ParseResult parseResult) {
		if (viewer != null && viewer.getControl() != null
				&& !viewer.getControl().isDisposed()) {
			// remove our now old cache so it can be rebuild
			clearCache();

			// refresh the viewer async because we might not be in the UI thread
			viewer.getControl().getDisplay().asyncExec(new Runnable() {
				public void run() {
					refresh();
				}
			});
		}
	}
}
