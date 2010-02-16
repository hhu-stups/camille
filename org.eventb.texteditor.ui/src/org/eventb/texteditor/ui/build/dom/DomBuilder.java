/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.build.dom;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eventb.emf.core.EventBNamedCommentedComponentElement;
import org.eventb.emf.core.context.Context;
import org.eventb.emf.core.context.ContextPackage;
import org.eventb.emf.core.machine.Machine;
import org.eventb.emf.core.machine.MachinePackage;
import org.eventb.emf.formulas.FormulasPackage;
import org.eventb.texteditor.ui.TextEditorPlugin;
import org.eventb.texteditor.ui.build.IBuildPhase;
import org.eventb.texteditor.ui.build.dom.DomManager.ParseResult;
import org.eventb.texteditor.ui.editor.EventBTextEditor;

public class DomBuilder implements IBuildPhase {

	public boolean isUIPhase() {
		return false;
	}

	public boolean canFail() {
		return false;
	}

	public boolean wasSuccessful() {
		return true;
	}

	public void run(final EventBTextEditor editor, final Resource resource,
			final IDocument document, final IProgressMonitor monitor) {
		final DomManager domManager = TextEditorPlugin.getDomManager();
		final IEditorInput editorInput = editor.getEditorInput();
		final ParseResult parseResult = domManager
				.getLastParseResult(editorInput);

		if (parseResult != null) {
			final EventBNamedCommentedComponentElement astRoot = (EventBNamedCommentedComponentElement) parseResult.astRoot;
			final IComponentDom dom = domManager.getDom(resource, astRoot);

			if (astRoot instanceof Machine) {
				((MachineDom) dom).setMachine((Machine) astRoot);
			} else if (astRoot instanceof Context) {
				((ContextDom) dom).setContext((Context) astRoot);
			}

			dom.resetAndinit();
		}
	}

	/**
	 * Find the {@link IComponentDom} for the given {@link EventBNamedCommentedComponentElement}. The
	 * given {@link Resource} is used as a base when resolving the resource for
	 * the referenced component.
	 *
	 * @param component
	 * @param referencingResource
	 * @return
	 */
	public static IComponentDom getReferencedDom(
			final EventBNamedCommentedComponentElement component, final Resource referencingResource) {
		final Resource resource = CollectingHelper.resolveComponentsResource(
				component, referencingResource);

		if (resource != null) {
			return TextEditorPlugin.getDomManager().getDom(resource, component);
		}

		return null;
	}

	/**
	 * Initializes the given {@link IComponentDom} with the given
	 * {@link EventBNamedCommentedComponentElement}. For all referenced {@link EventBNamedCommentedComponentElement} the
	 * given {@link Resource} is used as the base from where to search the
	 * referenced {@link Resource}.
	 *
	 * @param dom
	 * @param component
	 * @param resource
	 * @return
	 */
	public static void initializeDOM(final IComponentDom dom,
			final EventBNamedCommentedComponentElement component, final Resource resource) {
		if (component instanceof Machine) {
			initializeDOM((MachineDom) dom, (Machine) component, resource);
		} else if (component instanceof Context) {
			initializeDOM((ContextDom) dom, (Context) component, resource);
		}

		// notify listeners in dom manager about change
		TextEditorPlugin.getDomManager().notifyDomChangeListeners(dom);
	}

	/**
	 * Initializes the given {@link MachineDom} with the given {@link Machine}.
	 * For all referenced {@link EventBNamedCommentedComponentElement} the given {@link Resource} is
	 * used as the base from where to search the referenced {@link Resource}.
	 *
	 * @param dom
	 * @param component
	 * @param resource
	 * @return
	 */
	private static void initializeDOM(final MachineDom dom,
			final Machine component, final Resource resource) {
		dom.reset();

		final MachineCollectingSwitch machineSwitch = new MachineCollectingSwitch(
				dom, resource);
		machineSwitch.doSwitch(component);
		final FormulaCollectingSwitch formulaSwitch = new FormulaCollectingSwitch();

		// traverse tree using an iterator
		final TreeIterator<EObject> iterator = getContentIterator(component,
				resource);

		while (iterator.hasNext()) {
			final EObject next = iterator.next();
			final String nsURI = next.eClass().getEPackage().getNsURI();
			Boolean visitChildren = true;

			if (MachinePackage.eNS_URI.equals(nsURI)) {
				visitChildren = machineSwitch.doSwitch(next);
			} else if (FormulasPackage.eNS_URI.equals(nsURI)) {
				formulaSwitch.setCurrentParentDom(machineSwitch
						.getCurrentParentDom());
				formulaSwitch.doSwitch(next);
			}

			// visit node
			if (!visitChildren) {
				// skip children if visited node returne false
				iterator.prune();
			}
		}
	}

	/**
	 * Initializes the given {@link ContextDom} with the given {@link Context}.
	 * For all referenced {@link EventBNamedCommentedComponentElement} the given {@link Resource} is
	 * used as the base from where to search the referenced {@link Resource}.
	 *
	 * @param dom
	 * @param component
	 * @param resource
	 * @return
	 */
	private static void initializeDOM(final ContextDom dom,
			final Context component, final Resource resource) {
		dom.reset();

		final ContextCollectingSwitch contextSwitch = new ContextCollectingSwitch(
				dom, resource);
		contextSwitch.doSwitch(component);
		final FormulaCollectingSwitch formulaSwitch = new FormulaCollectingSwitch();

		// traverse tree using an iterator
		final TreeIterator<EObject> iterator = getContentIterator(component,
				resource);

		while (iterator.hasNext()) {
			final EObject next = iterator.next();
			final String nsURI = next.eClass().getEPackage().getNsURI();
			Boolean visitChildren = true;

			if (ContextPackage.eNS_URI.equals(nsURI)) {
				visitChildren = contextSwitch.doSwitch(next);
			} else if (FormulasPackage.eNS_URI.equals(nsURI)) {
				formulaSwitch.setCurrentParentDom(contextSwitch
						.getCurrentParentDom());
				formulaSwitch.doSwitch(next);
			}

			// visit node
			if (!visitChildren) {
				// skip children if visited node returne false
				iterator.prune();
			}
		}
	}

	private static TreeIterator<EObject> getContentIterator(
			final EventBNamedCommentedComponentElement component, final Resource resource) {
		if (component.eIsProxy()) {
			// the component has not been resolved yet, use resource to do so
			return EcoreUtil.getAllContents(resource, true);
		} else {
			// we can use the contents of the component itself
			return EcoreUtil.getAllContents(component, false);
		}
	}
}
