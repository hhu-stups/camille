/**
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen,
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html)
 * */

package org.eventb.texteditor.ui.editor.proofstatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eventb.core.IContextRoot;
import org.eventb.core.IEvent;
import org.eventb.core.IEventBRoot;
import org.eventb.core.ILabeledElement;
import org.eventb.core.IMachineRoot;
import org.eventb.core.IPOSequent;
import org.eventb.core.IPOSource;
import org.eventb.core.IPSRoot;
import org.eventb.core.IPSStatus;
import org.eventb.core.seqprover.IConfidence;
import org.rodinp.core.IRodinElement;
import org.rodinp.core.IRodinFile;
import org.rodinp.core.RodinCore;
import org.rodinp.core.RodinDBException;

/**
 * The proof obligations of a machine or context and the labeled elements
 * (invariants, axioms, events, guards, actions, witnesses) they belong to.
 * The relation is taken from the sources of the proof obligations, e.g.
 * <code>inc/inv1/INV</code> has the event <code>inc</code> and the
 * invariant <code>inv1</code> as sources.
 * <p>
 * Instances are immutable, a new index is built when the proof status
 * changes.
 * </p>
 */
public final class ProofStatusIndex {

	/** Status of one proof obligation */
	public enum Status {
		DISCHARGED("discharged"), REVIEWED("reviewed"), PENDING("not proven");

		public final String description;

		Status(final String description) {
			this.description = description;
		}
	}

	/** Combined status of the proof obligations of an element */
	public enum Summary {
		/** all discharged */
		DISCHARGED,
		/** all discharged or reviewed, at least one reviewed */
		REVIEWED,
		/** some discharged or reviewed */
		PARTIAL,
		/** none discharged or reviewed */
		PENDING
	}

	public static final class ProofObligation {
		public final String name;
		public final Status status;
		public final boolean broken;
		public final boolean manual;
		public final IPSStatus handle;
		final Set<IRodinElement> sources;

		ProofObligation(final String name, final Status status,
				final boolean broken, final boolean manual,
				final IPSStatus handle, final Set<IRodinElement> sources) {
			this.name = name;
			this.status = status;
			this.broken = broken;
			this.manual = manual;
			this.handle = handle;
			this.sources = sources;
		}

		/** e.g. "discharged (manual)" */
		public String describeStatus() {
			final StringBuilder result = new StringBuilder(status.description);
			if (broken) {
				result.append(", proof broken");
			} else if (status != Status.PENDING) {
				result.append(manual ? " (manual)" : " (auto)");
			}
			return result.toString();
		}
	}

	public static final ProofStatusIndex EMPTY = new ProofStatusIndex(
			Collections.<ProofObligation> emptyList(),
			Collections.<String, IRodinElement> emptyMap());

	private final List<ProofObligation> obligations;
	private final Map<String, IRodinElement> elements;

	private ProofStatusIndex(final List<ProofObligation> obligations,
			final Map<String, IRodinElement> elements) {
		this.obligations = obligations;
		this.elements = elements;
	}

	/**
	 * @param event
	 *            the event the label belongs to, <code>null</code> for
	 *            invariants and axioms
	 * @return the proof obligations of the labeled element, sorted by name
	 */
	public List<ProofObligation> forLabel(final String event,
			final String label) {
		return forElement(elements.get(labelKey(event, label)));
	}

	/**
	 * @return the proof obligations of the event, sorted by name
	 */
	public List<ProofObligation> forEvent(final String event) {
		return forElement(elements.get(eventKey(event)));
	}

	private List<ProofObligation> forElement(final IRodinElement element) {
		if (element == null) {
			return Collections.emptyList();
		}
		final List<ProofObligation> result = new ArrayList<ProofObligation>();
		for (final ProofObligation obligation : obligations) {
			if (obligation.sources.contains(element)) {
				result.add(obligation);
			}
		}
		return result;
	}

	/**
	 * @return the combined status, or <code>null</code> for no proof
	 *         obligations
	 */
	public static Summary summarize(final List<ProofObligation> obligations) {
		if (obligations.isEmpty()) {
			return null;
		}
		int discharged = 0;
		int reviewed = 0;
		for (final ProofObligation obligation : obligations) {
			if (obligation.broken) {
				continue;
			}
			if (obligation.status == Status.DISCHARGED) {
				discharged++;
			} else if (obligation.status == Status.REVIEWED) {
				reviewed++;
			}
		}
		if (discharged == obligations.size()) {
			return Summary.DISCHARGED;
		}
		if (discharged + reviewed == obligations.size()) {
			return Summary.REVIEWED;
		}
		if (discharged + reviewed > 0) {
			return Summary.PARTIAL;
		}
		return Summary.PENDING;
	}

	/**
	 * @return e.g. "2 of 3 proof obligations discharged, 1 reviewed"
	 */
	public static String describe(final List<ProofObligation> obligations) {
		int discharged = 0;
		int reviewed = 0;
		for (final ProofObligation obligation : obligations) {
			if (!obligation.broken && obligation.status == Status.DISCHARGED) {
				discharged++;
			} else if (!obligation.broken
					&& obligation.status == Status.REVIEWED) {
				reviewed++;
			}
		}
		final StringBuilder result = new StringBuilder();
		result.append(discharged).append(" of ").append(obligations.size())
				.append(obligations.size() == 1 ? " proof obligation"
						: " proof obligations").append(" discharged");
		if (reviewed > 0) {
			result.append(", ").append(reviewed).append(" reviewed");
		}
		return result.toString();
	}

	private static String labelKey(final String event, final String label) {
		return (event == null ? "" : event) + "/" + label;
	}

	private static String eventKey(final String event) {
		return "event:" + event;
	}

	/**
	 * Reads the proof obligations of a component from the Rodin database.
	 * Must not be called in the UI thread.
	 */
	public static ProofStatusIndex build(final IFile file) throws CoreException {
		final IRodinFile rodinFile = RodinCore.valueOf(file);
		if (rodinFile == null || !rodinFile.exists()
				|| !(rodinFile.getRoot() instanceof IEventBRoot)) {
			return EMPTY;
		}
		final IEventBRoot root = (IEventBRoot) rodinFile.getRoot();

		final Map<String, IRodinElement> elements = new HashMap<String, IRodinElement>();
		if (root instanceof IMachineRoot) {
			final IMachineRoot machine = (IMachineRoot) root;
			addLabeled(elements, null, machine.getInvariants());
			for (final IEvent event : machine.getEvents()) {
				if (!event.hasLabel()) {
					continue;
				}
				final String label = event.getLabel();
				elements.put(eventKey(label), event);
				addLabeled(elements, label, event.getGuards());
				addLabeled(elements, label, event.getActions());
				addLabeled(elements, label, event.getWitnesses());
			}
		} else if (root instanceof IContextRoot) {
			addLabeled(elements, null, ((IContextRoot) root).getAxioms());
		}

		final List<ProofObligation> obligations = new ArrayList<ProofObligation>();
		final IPSRoot psRoot = root.getPSRoot();
		if (psRoot != null && psRoot.exists()) {
			for (final IPSStatus status : psRoot.getStatuses()) {
				obligations.add(read(status));
			}
		}
		Collections.sort(obligations, new Comparator<ProofObligation>() {
			@Override
			public int compare(final ProofObligation a, final ProofObligation b) {
				return a.name.compareTo(b.name);
			}
		});

		return new ProofStatusIndex(obligations, elements);
	}

	private static void addLabeled(final Map<String, IRodinElement> elements,
			final String event, final ILabeledElement[] labeled)
			throws RodinDBException {
		for (final ILabeledElement element : labeled) {
			if (element.hasLabel()) {
				elements.put(labelKey(event, element.getLabel()), element);
			}
		}
	}

	private static ProofObligation read(final IPSStatus status)
			throws RodinDBException {
		final int confidence = status.getConfidence();
		final Status state;
		if (confidence > IConfidence.REVIEWED_MAX) {
			state = Status.DISCHARGED;
		} else if (confidence > IConfidence.PENDING) {
			state = Status.REVIEWED;
		} else {
			state = Status.PENDING;
		}

		final Set<IRodinElement> sources = new HashSet<IRodinElement>();
		final IPOSequent sequent = status.getPOSequent();
		if (sequent != null && sequent.exists()) {
			for (final IPOSource source : sequent.getSources()) {
				try {
					sources.add(source.getSource());
				} catch (final RodinDBException e) {
					// IGNORE, source attribute missing
				}
			}
		}

		return new ProofObligation(status.getElementName(), state,
				status.isBroken(), status.getHasManualProof(), status, sources);
	}
}
