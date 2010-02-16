/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.build.dom;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eventb.texttools.TextPositionUtil;
import org.eventb.texttools.model.texttools.TextRange;

public abstract class AbstractDom implements IDom {

	private final Type type;
	private final IDom parent;
	private final List<IDom> children = new LinkedList<IDom>();

	protected AbstractDom(final Type type, final IDom parent) {
		this.type = type;
		this.parent = parent;
	}

	public Type getType() {
		return type;
	}

	public IDom getParent() {
		return parent;
	}

	public synchronized void reset() {
		children.clear();
	}

	/**
	 * Returns a {@link List} with all child DOMs of this DOM. <b>Attention:</b>
	 * The returned list is unmodifiable and will throw
	 * {@link UnsupportedOperationException}s for modification method calls.
	 */
	public synchronized List<IDom> getChildren() {
		return Collections.unmodifiableList(children);
	}

	public synchronized void addChild(final IDom child) {
		children.add(child);
	}

	public synchronized IDom getScopingDom(final int offset) {
		if (!containsOffset(TextPositionUtil.getTextRange(getEventBElement()),
				offset)) {
			return null;
		}

		for (final IDom child : children) {
			final IDom scope = child.getScopingDom(offset);
			if (scope != null) {
				return scope;
			}
		}

		return this;
	}

	public Set<String> getIdentifiers() {
		final Set<String> result = new HashSet<String>();

		if (parent != null) {
			result.addAll(parent.getIdentifiers());
		}

		result.addAll(doGetIdentifiers());

		return result;
	}

	public IdentifierType getIdentifierType(final String identifier) {
		IDom currentDom = this;
		IdentifierType type = null;

		// search identifier upwarts in hierarchy
		while (currentDom != null
				&& (type = ((AbstractDom) currentDom)
						.doGetIdentifierType(identifier)) == null) {
			currentDom = currentDom.getParent();
		}

		return type;
	}

	private boolean containsOffset(final TextRange range, final int offset) {
		return range != null && range.getOffset() <= offset
				&& range.getOffset() + range.getLength() >= offset;
	}

	protected abstract Set<String> doGetIdentifiers();

	protected abstract IdentifierType doGetIdentifierType(
			final String identifier);
}
