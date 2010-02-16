/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.build.dom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.ui.IEditorInput;
import org.eventb.emf.core.EventBNamedCommentedComponentElement;
import org.eventb.emf.core.EventBObject;
import org.eventb.emf.core.context.Context;
import org.eventb.emf.core.machine.Machine;

public class DomManager {

	private final Map<Resource, IComponentDom> lastDom = new HashMap<Resource, IComponentDom>();
	private final Map<IEditorInput, ParseResult> lastParseResults = new HashMap<IEditorInput, ParseResult>();

	private final Set<IDomChangeListener> domListeners = new HashSet<IDomChangeListener>();
	private final Map<IEditorInput, List<IParseResultListener>> parseResultListeners = new HashMap<IEditorInput, List<IParseResultListener>>();

	public void storeDom(final IComponentDom dom) {
		if (dom != null) {
			synchronized (lastDom) {
				lastDom.put(dom.getResource(), dom);
			}
		}
	}

	public void removeDom(final Resource resource) {
		synchronized (lastDom) {
			lastDom.remove(resource);
		}
	}

	/**
	 * Returns an {@link IComponentDom} for the given {@link Resource} or
	 * <code>null</code> if none is available. See
	 * {@link #getDom(Resource, EventBNamedCommentedComponentElement)} if you want lookup and
	 * on-demand creation.
	 *
	 * @see #getDom(Resource, EventBNamedCommentedComponentElement)
	 * @param resource
	 * @return
	 */
	public IComponentDom getDom(final Resource resource) {
		synchronized (lastDom) {
			return lastDom.get(resource);
		}
	}

	/**
	 * Returns an {@link IComponentDom} object for the given {@link Resource}.
	 * If no DOM is available so far a new and uninitialized one is created for
	 * the resource and the given {@link EventBNamedCommentedComponentElement}.
	 *
	 * @param resource
	 * @param object
	 * @return
	 */
	public IComponentDom getDom(final Resource resource,
			final EventBNamedCommentedComponentElement component) {
		synchronized (lastDom) {
			IComponentDom dom = lastDom.get(resource);

			if (dom == null && component != null) {
				dom = createNewDom(resource, component);
				storeDom(dom);
			}

			return dom;
		}
	}

	/**
	 * Returns the {@link IComponentDom} object for the given
	 * {@link EventBObject}'s resource. If no DOM is available so far a new and
	 * uninitialized one is created.
	 *
	 * @see #getDom(Resource)
	 * @param object
	 * @return
	 */
	public IComponentDom getDom(final EventBNamedCommentedComponentElement component) {
		final Resource resource = component.eResource();

		if (resource != null) {
			synchronized (lastDom) {
				return getDom(resource, component);
			}
		}

		return null;
	}

	/**
	 * Creates a new and uninitialized {@link IComponentDom} for the given
	 * {@link EventBNamedCommentedComponentElement}.
	 *
	 * @param component
	 * @return
	 */
	private IComponentDom createNewDom(final Resource resource,
			final EventBNamedCommentedComponentElement component) {
		IComponentDom dom;
		if (component instanceof Machine) {
			dom = new MachineDom(resource, (Machine) component);
		} else {
			dom = new ContextDom(resource, (Context) component);
		}

		return dom;
	}

	public void storeParseResult(final IEditorInput editorInput,
			final String textInput, final EventBNamedCommentedComponentElement astRoot) {
		final ParseResult parseResult = new ParseResult(astRoot, textInput);
		lastParseResults.put(editorInput, parseResult);

		notifyParseResultListeners(editorInput, parseResult);
	}

	/**
	 * <p>
	 * Returns the last successful parse result, i.e., the last successfully
	 * parsed text and its AST that was produced.
	 * </p>
	 * <p>
	 * Attention: The result may be old and not consistent with the current
	 * content of the editor.
	 * </p>
	 *
	 * @param editorInput
	 * @return
	 */
	public ParseResult getLastParseResult(final IEditorInput editorInput) {
		return lastParseResults.get(editorInput);
	}

	public void addParseResultListener(final IEditorInput editorInput,
			final IParseResultListener listener) {
		if (!parseResultListeners.containsKey(editorInput)) {
			final ArrayList<IParseResultListener> list = new ArrayList<IParseResultListener>();
			list.add(listener);
			parseResultListeners.put(editorInput, list);
		} else {
			parseResultListeners.get(editorInput).add(listener);
		}
	}

	public void removeParseResultListener(final IEditorInput editorInput,
			final IParseResultListener listener) {
		if (parseResultListeners.containsKey(editorInput)) {
			final List<IParseResultListener> list = parseResultListeners
					.get(editorInput);
			list.remove(listener);

			if (list.isEmpty()) {
				parseResultListeners.remove(editorInput);
			}
		}
	}

	private void notifyParseResultListeners(final IEditorInput editorInput,
			final ParseResult parseResult) {
		if (parseResultListeners.containsKey(editorInput)) {
			for (final IParseResultListener listener : parseResultListeners
					.get(editorInput)) {
				listener.parseResultChanged(parseResult);
			}
		}
	}

	public void addDomChangeListener(final IDomChangeListener listener) {
		if (listener != null && !domListeners.contains(listener)) {
			domListeners.add(listener);
		}
	}

	public void removeDomChangeListener(final IDomChangeListener listener) {
		if (listener != null) {
			domListeners.remove(listener);
		}
	}

	public void notifyDomChangeListeners(final IComponentDom dom) {
		for (final IDomChangeListener listener : domListeners) {
			listener.domChanged(dom);
		}
	}

	public class ParseResult {
		public final EventBObject astRoot;
		public final String textInput;

		public ParseResult(final EventBObject astRoot, final String textInput) {
			this.astRoot = astRoot;
			this.textInput = textInput;
		}
	}
}
