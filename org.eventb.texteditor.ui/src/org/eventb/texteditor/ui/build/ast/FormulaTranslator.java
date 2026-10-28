/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.build.ast;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.swt.widgets.Display;
import org.eventb.emf.core.EventBNamedCommentedExpressionElement;
import org.eventb.emf.core.EventBElement;
import org.eventb.emf.core.EventBNamedCommentedComponentElement;
import org.eventb.emf.core.EventBNamedCommentedPredicateElement;
import org.eventb.emf.core.context.Context;
import org.eventb.emf.core.machine.Action;
import org.eventb.emf.core.machine.Machine;
import org.eventb.texteditor.ui.build.DocumentSnapshot;
import org.eventb.texteditor.ui.editor.EventBTextEditor;
import org.eventb.texttools.TextPositionUtil;
import org.eventb.texttools.model.texttools.TextRange;
import org.rodinp.keyboard.core.RodinKeyboardCore;

public class FormulaTranslator {
	private IDocument document;
	private EventBTextEditor editor;
	private final List<Replacement> pendingReplacements = new ArrayList<Replacement>();

	public void translateFormulas(
			final EventBNamedCommentedComponentElement astRoot,
			final IDocument document, final EventBTextEditor editor) {
		this.document = document;
		this.editor = editor;
		pendingReplacements.clear();

		// traverse tree using an iterator
		final TreeIterator<EObject> iterator = EcoreUtil.getAllContents(
				astRoot, false);

		if (astRoot instanceof Machine) {
			final MachineTranslateSwitch switcher = new MachineTranslateSwitch(
					this);
			while (iterator.hasNext()) {
				// visit node
				switcher.doSwitch(iterator.next());
			}
		} else if (astRoot instanceof Context) {
			final ContextTranslateSwitch switcher = new ContextTranslateSwitch(
					this);
			while (iterator.hasNext()) {
				// visit node
				switcher.doSwitch(iterator.next());
			}
		}

		applyReplacements();
	}

	public void replace(final EventBNamedCommentedPredicateElement predicate) {
		final String input = predicate.getPredicate();

		if (input != null) {
			final String translatedInput = RodinKeyboardCore.translate(input);

			if (!input.equals(translatedInput)) {
				// replace in emf node
				predicate.setPredicate(translatedInput);
				final TextRange range = updatePosition(predicate, input,
						translatedInput);

				updateDocument(input, translatedInput, range);
			}
		}
	}

	public void replace(final EventBNamedCommentedExpressionElement expression) {
		final String input = expression.getExpression();

		if (input != null) {
			final String translatedInput = RodinKeyboardCore.translate(input);

			if (!input.equals(translatedInput)) {
				// replace in emf node
				expression.setExpression(translatedInput);
				final TextRange range = updatePosition(expression, input,
						translatedInput);

				updateDocument(input, translatedInput, range);
			}
		}
	}

	public void replace(final Action action) {
		final String input = action.getAction();

		if (input != null) {
			final String translatedInput = RodinKeyboardCore.translate(input);

			if (!input.equals(translatedInput)) {
				// replace in emf node
				action.setAction(translatedInput);
				final TextRange range = updatePosition(action, input,
						translatedInput);

				updateDocument(input, translatedInput, range);
			}
		}
	}

	private void updateDocument(final String input,
			final String translatedInput, final TextRange range) {
		if (range == null) {
			// no position information, cannot update the text
			return;
		}

		/*
		 * Check length and fill output string with whitespaces to length of
		 * input. So we avoid position changes for the following text elements.
		 * Output should always be <= input.
		 */
		final int lengthDiff = input.length() - translatedInput.length();
		if (lengthDiff < 0) {
			// should not happen, but never corrupt the user's text
			return;
		}
		final String filledString = fillOutput(translatedInput, lengthDiff);

		pendingReplacements.add(new Replacement(range.getOffset(), input,
				filledString, translatedInput.length()));
	}

	/**
	 * Writes all collected translations back into the editor's document.
	 * <p>
	 * All replacements of one build are applied together as one undoable
	 * change, and only if the document has not been modified since it was
	 * parsed. Otherwise the stored offsets would point to the wrong text and
	 * the user's input could be overwritten. In that case nothing is done;
	 * the next reconcile run will try again.
	 * </p>
	 */
	private void applyReplacements() {
		if (pendingReplacements.isEmpty() || editor == null) {
			return;
		}

		final List<Replacement> replacements = new ArrayList<Replacement>(
				pendingReplacements);
		pendingReplacements.clear();

		final DocumentSnapshot snapshot = document instanceof DocumentSnapshot ? (DocumentSnapshot) document
				: null;
		final IDocument target = snapshot != null ? snapshot.getOriginal()
				: document;
		final EventBTextEditor targetEditor = editor;

		final Runnable runnable = new Runnable() {
			@Override
			public void run() {
				if (!targetEditor.isEditorAlive()
						|| !targetEditor.isAutoTranslationActive()) {
					return;
				}
				if (snapshot != null && !snapshot.isUpToDate()) {
					return;
				}
				// only ever modify the document shown in the editor
				if (targetEditor.getDocumentProvider() == null
						|| target != targetEditor.getDocumentProvider()
								.getDocument(targetEditor.getEditorInput())) {
					return;
				}
				applyReplacements(targetEditor, target, replacements);
			}
		};

		if (Display.getCurrent() != null) {
			// we are in the UI thread already (e.g. while saving)
			runnable.run();
		} else {
			Display.getDefault().asyncExec(runnable);
		}
	}

	private static void applyReplacements(final EventBTextEditor editor,
			final IDocument target, final List<Replacement> replacements) {
		final int cursorOffset = editor.getCursorOffset();
		int newCursorOffset = cursorOffset;

		final IRewriteTarget rewriteTarget = (IRewriteTarget) editor
				.getAdapter(IRewriteTarget.class);
		if (rewriteTarget != null) {
			rewriteTarget.beginCompoundChange();
		}

		try {
			for (final Replacement replacement : replacements) {
				try {
					final int offset = replacement.offset;
					final int length = replacement.original.length();

					// only replace if the text is still what we parsed
					if (offset < 0 || offset + length > target.getLength()
							|| !replacement.original.equals(target.get(offset,
									length))) {
						continue;
					}

					target.replace(offset, length, replacement.replacement);

					// keep cursor inside the translated formula
					if (cursorOffset >= offset
							&& cursorOffset <= offset + length) {
						newCursorOffset = Math.min(cursorOffset, offset
								+ replacement.visibleLength);
					}
				} catch (final BadLocationException e) {
					// IGNORE, text changed in the meantime
				}
			}
		} finally {
			if (rewriteTarget != null) {
				rewriteTarget.endCompoundChange();
			}
		}

		if (newCursorOffset != cursorOffset) {
			editor.setCurserOffset(newCursorOffset);
		}
	}

	private static final class Replacement {
		final int offset;
		final String original;
		final String replacement;
		final int visibleLength;

		Replacement(final int offset, final String original,
				final String replacement, final int visibleLength) {
			this.offset = offset;
			this.original = original;
			this.replacement = replacement;
			this.visibleLength = visibleLength;
		}
	}

	private String fillOutput(final String output, final int count) {
		if (count > 0) {
			final StringBuilder buffer = new StringBuilder(output);
			for (int i = 0; i < count; i++) {
				buffer.append(' ');
			}

			return buffer.toString();
		} else {
			return output;
		}
	}

	private TextRange updatePosition(final EventBElement parent,
			final String oldContent, final String newContent) {
		final TextRange range = TextPositionUtil.getInternalPosition(parent,
				oldContent);
		if (range == null) {
			return null;
		}

		range.setLength(newContent.length());
		TextPositionUtil.replaceInternalPosition(parent, oldContent,
				newContent, range);

		return range;
	}
}
