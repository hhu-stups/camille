/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.build.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

				updateDocument(input, range);
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

				updateDocument(input, range);
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

				updateDocument(input, range);
			}
		}
	}

	private void updateDocument(final String input, final TextRange range) {
		if (range == null) {
			// no position information, cannot update the text
			return;
		}

		/*
		 * The text is translated when it is applied in the UI thread, because
		 * the handling of "{}" depends on the editor state at that time.
		 */
		pendingReplacements.add(new Replacement(range.getOffset(), input));
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

		/*
		 * "{}" is translated to the empty set only when something follows it
		 * or the editor does not have the focus (any more). See
		 * translateForEditor().
		 */
		final boolean focused = editor.hasTextFocus();

		/*
		 * Apply from the end of the document to the beginning, so the offsets
		 * of the remaining replacements stay valid. (The translated text used
		 * to be padded with blanks to keep all offsets unchanged. Those blanks
		 * looked like text typed after "{}".)
		 */
		final List<Replacement> sorted = new ArrayList<Replacement>(
				replacements);
		Collections.sort(sorted, new Comparator<Replacement>() {
			@Override
			public int compare(final Replacement r1, final Replacement r2) {
				return Integer.compare(r2.offset, r1.offset);
			}
		});

		final IRewriteTarget rewriteTarget = editor.getAdapter(IRewriteTarget.class);
		if (rewriteTarget != null) {
			rewriteTarget.beginCompoundChange();
		}

		try {
			for (final Replacement replacement : sorted) {
				try {
					final int offset = replacement.offset;
					final int length = replacement.original.length();

					// only replace if the text is still what we parsed
					if (offset < 0 || offset + length > target.getLength()
							|| !replacement.original.equals(target.get(offset,
									length))) {
						continue;
					}

					// formulas do not include trailing blanks, look them up
					final char followingChar = offset + length < target
							.getLength() ? target.getChar(offset + length)
							: '\n';
					final String translated = translateForEditor(
							replacement.original, followingChar, !focused);
					if (translated.length() > length
							|| translated.equals(replacement.original)) {
						continue;
					}

					target.replace(offset, length, translated);

					// keep the cursor at the same position in the text
					if (cursorOffset >= offset
							&& cursorOffset <= offset + length) {
						// inside the formula, like before
						newCursorOffset = Math.min(cursorOffset, offset
								+ translated.length());
					} else if (cursorOffset > offset + length) {
						newCursorOffset -= length - translated.length();
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

	private static final String EMPTY_SET_BRACES = "{}";

	/*
	 * Placeholder for "{}" that must not be translated yet. Characters from
	 * the Unicode private use area do not occur in Event-B text and are not
	 * touched by the Rodin keyboard translation.
	 */
	private static final String PROTECTED_BRACES = "\uE000\uE001";

	/**
	 * Translates the formula text like the Rodin keyboard, except that
	 * <code>{}</code> stays as it is while nothing follows it in its line (or
	 * <code>translateAllBraces</code> is set). Until something is typed after
	 * it, <code>{}</code> is usually the beginning of a set whose elements are
	 * entered next. The formula in the Rodin database is always translated
	 * completely.
	 *
	 * @param followingChar
	 *            the character after the formula in the document
	 */
	static String translateForEditor(final String input,
			final char followingChar, final boolean translateAllBraces) {
		if (translateAllBraces || !input.contains(EMPTY_SET_BRACES)) {
			return RodinKeyboardCore.translate(input);
		}

		final StringBuilder protectedInput = new StringBuilder(input);
		int index = input.indexOf(EMPTY_SET_BRACES);
		while (index >= 0) {
			final int next = index + EMPTY_SET_BRACES.length();
			final char following = next < input.length() ? input.charAt(next)
					: followingChar;
			if (following == '\n' || following == '\r') {
				// nothing typed after "{}" yet
				protectedInput.replace(index, next, PROTECTED_BRACES);
			}
			index = input.indexOf(EMPTY_SET_BRACES, next);
		}

		return RodinKeyboardCore.translate(protectedInput.toString()).replace(
				PROTECTED_BRACES, EMPTY_SET_BRACES);
	}

	private static final class Replacement {
		final int offset;
		final String original;

		Replacement(final int offset, final String original) {
			this.offset = offset;
			this.original = original;
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
