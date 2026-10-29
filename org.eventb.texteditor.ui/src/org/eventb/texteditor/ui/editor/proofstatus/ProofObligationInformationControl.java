/**
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen,
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html)
 * */

package org.eventb.texteditor.ui.editor.proofstatus;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.AbstractInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IInformationControlExtension2;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eventb.texteditor.ui.editor.proofstatus.ProofStatusIndex.ProofObligation;
import org.eventb.ui.EventBUIPlugin;
import org.eventb.ui.IEventBSharedImages;

/**
 * Hover for labels: the proof obligations of the labeled element as a part
 * of the proof obligation tree, with Rodin's status icons. In the focused
 * hover (F2, or moving the mouse into the hover) a double click opens a
 * proof obligation in the prover.
 */
public class ProofObligationInformationControl extends
		AbstractInformationControl implements IInformationControlExtension2 {

	private final boolean interactive;
	private StyledText header;
	private Tree tree;
	private ProofObligationHoverInput input;

	/** the hover */
	public ProofObligationInformationControl(final Shell parent,
			final String statusText) {
		super(parent, statusText);
		this.interactive = false;
		create();
	}

	/**
	 * the focused, resizable hover, where a double click opens a proof
	 * obligation
	 */
	public ProofObligationInformationControl(final Shell parent) {
		super(parent, true);
		this.interactive = true;
		create();
	}

	@Override
	protected void createContent(final Composite parent) {
		final Composite composite = new Composite(parent, SWT.NONE);
		final GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 2;
		layout.marginHeight = 2;
		composite.setLayout(layout);
		composite.setForeground(parent.getForeground());
		composite.setBackground(parent.getBackground());

		header = new StyledText(composite, SWT.READ_ONLY | SWT.WRAP);
		header.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		header.setForeground(parent.getForeground());
		header.setBackground(parent.getBackground());
		header.setFont(JFaceResources.getDialogFont());
		header.setEditable(false);

		tree = new Tree(composite, SWT.SINGLE
				| (interactive ? SWT.V_SCROLL | SWT.H_SCROLL : SWT.NONE));
		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tree.setForeground(parent.getForeground());
		tree.setBackground(parent.getBackground());
		tree.setFont(JFaceResources.getDialogFont());

		tree.addListener(SWT.DefaultSelection, new Listener() {
			@Override
			public void handleEvent(final Event event) {
				if (event.item != null
						&& event.item.getData() instanceof ProofObligation) {
					final ProofObligation obligation = (ProofObligation) event.item
							.getData();
					dispose();
					ProofObligationOpener.open(obligation.handle);
				}
			}
		});
	}

	@Override
	public void setInput(final Object newInput) {
		input = newInput instanceof ProofObligationHoverInput ? (ProofObligationHoverInput) newInput
				: null;
		tree.removeAll();
		header.setText("");
		if (input == null) {
			return;
		}

		header.setText(input.text);
		final int firstLineEnd = input.text.indexOf('\n');
		final StyleRange bold = new StyleRange();
		bold.start = 0;
		bold.length = firstLineEnd < 0 ? input.text.length() : firstLineEnd;
		bold.fontStyle = SWT.BOLD;
		header.setStyleRange(bold);
		header.setVisible(input.text.length() > 0);
		((GridData) header.getLayoutData()).exclude = input.text.length() == 0;

		final TreeItem root = new TreeItem(tree, SWT.NONE);
		root.setText(input.component + " – "
				+ ProofStatusIndex.describe(input.obligations));
		root.setImage(imageFor(ProofStatusIndex.summarize(input.obligations)));
		for (final ProofObligation obligation : input.obligations) {
			final TreeItem item = new TreeItem(root, SWT.NONE);
			item.setText(obligation.name + " – "
					+ obligation.describeStatus());
			item.setImage(imageFor(obligation));
			item.setData(obligation);
		}
		root.setExpanded(true);
	}

	private static Image imageFor(final ProofObligation obligation) {
		final String key;
		switch (obligation.status) {
		case DISCHARGED:
			key = obligation.broken ? IEventBSharedImages.IMG_DISCHARGED_BROKEN
					: IEventBSharedImages.IMG_DISCHARGED;
			break;
		case REVIEWED:
			key = obligation.broken ? IEventBSharedImages.IMG_REVIEWED_BROKEN
					: IEventBSharedImages.IMG_REVIEWED;
			break;
		default:
			key = obligation.broken ? IEventBSharedImages.IMG_PENDING_BROKEN
					: IEventBSharedImages.IMG_PENDING;
		}
		return image(key);
	}

	private static Image imageFor(final ProofStatusIndex.Summary summary) {
		if (summary == null) {
			return null;
		}
		switch (summary) {
		case DISCHARGED:
			return image(IEventBSharedImages.IMG_DISCHARGED);
		case REVIEWED:
			return image(IEventBSharedImages.IMG_REVIEWED);
		default:
			return image(IEventBSharedImages.IMG_PENDING);
		}
	}

	private static Image image(final String key) {
		try {
			return EventBUIPlugin.getDefault().getImageRegistry().get(key);
		} catch (final RuntimeException e) {
			return null;
		}
	}

	@Override
	public boolean hasContents() {
		return input != null;
	}

	@Override
	public Point computeSizeHint() {
		final Point constraints = getSizeConstraints();
		final Point size = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT,
				true);
		if (constraints != null) {
			size.x = Math.min(size.x, constraints.x);
			size.y = Math.min(size.y, constraints.y);
		}
		return size;
	}

	@Override
	public void setFocus() {
		super.setFocus();
		tree.setFocus();
	}

	@Override
	public IInformationControlCreator getInformationPresenterControlCreator() {
		return new IInformationControlCreator() {
			@Override
			public IInformationControl createInformationControl(
					final Shell parent) {
				return new ProofObligationInformationControl(parent);
			}
		};
	}

	/**
	 * Creator for the hover.
	 */
	public static class Creator implements IInformationControlCreator {
		private final String statusText;

		public Creator(final String statusText) {
			this.statusText = statusText;
		}

		@Override
		public IInformationControl createInformationControl(final Shell parent) {
			return new ProofObligationInformationControl(parent, statusText);
		}
	}
}
