/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.reconciler;

import java.util.Set;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eventb.texteditor.ui.TextEditorPlugin;
import org.eventb.texteditor.ui.build.dom.DomManager;
import org.eventb.texteditor.ui.build.dom.IComponentDom;
import org.eventb.texteditor.ui.build.dom.IDomChangeListener;
import org.eventb.texteditor.ui.reconciler.partitioning.PartitionScanner;

/**
 * This {@link PresentationReconciler} does basic syntax highlighting based on
 * content types of partitions. It reset text to default style if default
 * content type is found. Beside that it recognizes the following elements in
 * EventB components:
 * <ul>
 * <li>Comment</li>
 * <li>Label</li>
 * <li>Structural keywords</li>
 * <li>Formula keywords</li>
 * </ul>
 * 
 */
public class EventBPresentationReconciler extends PresentationReconciler
		implements IDomChangeListener {
	private static final DomManager domManager = TextEditorPlugin
			.getDomManager();
	private SemanticTokenScanner semanticScanner;
	private ITextViewer viewer;
	private IComponentDom dom = null;
	private Resource resource;

	public EventBPresentationReconciler() {
		super();
		init();
	}

	private void init() {
		// init semantic token scanner
		semanticScanner = new SemanticTokenScanner();

		// default content type: semantic highlighing
		DefaultDamagerRepairer damager = new DefaultDamagerRepairer(
				semanticScanner);
		setDamager(damager, IDocument.DEFAULT_CONTENT_TYPE);
		setRepairer(damager, IDocument.DEFAULT_CONTENT_TYPE);

		// comments
		damager = new DefaultDamagerRepairer(new CommentTokenScanner());
		setDamager(damager, PartitionScanner.CONTENT_TYPE_COMMENT);
		setRepairer(damager, PartitionScanner.CONTENT_TYPE_COMMENT);

		// labels
		damager = new DefaultDamagerRepairer(new LabelTokenScanner());
		setDamager(damager, PartitionScanner.CONTENT_TYPE_LABEL);
		setRepairer(damager, PartitionScanner.CONTENT_TYPE_LABEL);
	}

	public void reconcilePresentation() {
		if (viewer == null && viewer.getTextWidget() == null
				&& viewer.getTextWidget().isDisposed()) {
			return;
		}

		viewer.getTextWidget().getDisplay().asyncExec(new Runnable() {
			public void run() {
				viewer.invalidateTextPresentation();
			}
		});
	}

	@Override
	public void install(final ITextViewer viewer) {
		super.install(viewer);
		this.viewer = viewer;
		domManager.addDomChangeListener(this);
	}

	@Override
	public void uninstall() {
		domManager.removeDomChangeListener(this);
		super.uninstall();

		setInputResource(null);
	}

	public void setInputResource(final Resource input) {
		resource = input;

		if (input != null) {
			dom = domManager.getDom(input);
		} else {
			dom = null;
		}

		semanticScanner.setInputResource(input);
	}

	public void domChanged(final IComponentDom changedDom) {
		checkInit();

		// we are directly affected by the update
		if (changedDom == dom) {
			reconcilePresentation();
		} else if (dom != null) {
			// any (transitive) relation to the changed dom?
			final Set<IComponentDom> referencedDoms = dom
					.getReferencedDoms(true);
			if (referencedDoms.contains(changedDom)) {
				reconcilePresentation();
			}
		}
	}

	private void checkInit() {
		if (dom == null && resource != null) {
			dom = domManager.getDom(resource);
		}
	}
}
