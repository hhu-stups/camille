/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.editor.codecompletion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateCompletionProcessor;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.editors.text.templates.ContributionTemplateStore;
import org.eventb.texteditor.ui.Images;
import org.eventb.texteditor.ui.TextEditorPlugin;
import org.eventb.texteditor.ui.build.dom.DomManager;
import org.eventb.texteditor.ui.build.dom.IComponentDom;
import org.eventb.texteditor.ui.build.dom.IDom;
import org.eventb.texteditor.ui.build.dom.IDom.IdentifierType;
import org.eventb.texteditor.ui.build.dom.IDom.Type;
import org.eventb.texteditor.ui.build.dom.MachineDom;
import org.eventb.texteditor.ui.editor.EventBTextEditor;
import org.eventb.texttools.Constants;

/**
 * Computes the content assist (code completion) proposals, i.e., for
 * identifiers, keywords and templates.
 * 
 * It uses the following scheme to measure the proposals' relevance:
 * <ul>
 * <li>Identifiers get 100 points (to be always at the top of the list)</li>
 * <li>Templates can reach 90 points</li>
 * <ul>
 * <li><code>Anywhere</code> templates get 90 points</li>
 * <li>Others get 50 points for the correct compontent</li>
 * <li><code>Events</code> templates can get 40 more points if the current
 * offset is within the events section.</li>
 * <li>All other get these 40 points per default</li>
 * </ul>
 * <li>Keywords can reach 80 points</li>
 * <ul>
 * <li>All templates get 50 points for the correct compontent</li>
 * <li>The remaining 30 points are given depending on the template type and the
 * current offset</li>
 * </ul>
 * </ul>
 * 
 */
public class DefaultContentAssist extends TemplateCompletionProcessor {

	public enum ContextType {
		Unkown(PREFIX + "unknown"), Anywhere(PREFIX + "anywhere"), Machine(
				PREFIX + "machine"), Context(PREFIX + "context"), Events(PREFIX
				+ "events");

		public final String key;

		ContextType(final String key) {
			this.key = key;
		}
	};

	private static final ICompletionProposal[] NO_PROPOSALS = new ICompletionProposal[0];
	private static final String PREFIX = "org.eventb.texteditor.";

	private ContributionTemplateStore templateStore;
	private final DomManager domManager = TextEditorPlugin.getDomManager();
	private final EventBTextEditor editor;

	private String errorMessage;
	private String contentType;
	private Region region;
	private String prefix;
	private IComponentDom dom;
	private IDom scopingDom;
	private Type componentType;
	private IDocument document;
	private ITextViewer viewer;
	private boolean offsetInEvents;

	public DefaultContentAssist(final EventBTextEditor editor) {
		this.editor = editor;
	}

	@Override
	public ICompletionProposal[] computeCompletionProposals(
			final ITextViewer viewer, final int offset) {
		errorMessage = null;

		try {
			setupContextInfo(viewer, offset);

			final List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
			computeIdentifierProposals(proposals);
			computeKeywordProposals(proposals);
			computeTemplateProposals(proposals);

			return sort(proposals);
		} catch (final BadLocationException e) {
			errorMessage = "Error while calculating content type";
		}

		return NO_PROPOSALS;
	}

	@Override
	protected ICompletionProposal createProposal(final Template template,
			final TemplateContext context, final IRegion region,
			final int relevance) {
		return new EventBTemplateProposal(template, context, region,
				getImage(template), relevance);
	}

	private void setupContextInfo(final ITextViewer viewer, int offset)
			throws BadLocationException {
		this.viewer = viewer;
		offset = adjustOffset(viewer, offset);

		document = viewer.getDocument();
		contentType = document.getContentType(offset);
		prefix = extractPrefix(viewer, offset);
		region = new Region(offset - prefix.length(), prefix.length());

		final int eventsOffset = document.get().indexOf(Constants.EVENTS);
		offsetInEvents = offset >= eventsOffset;

		dom = domManager.getDom(editor.getResource());

		if (dom != null) {
			scopingDom = dom.getScopingDom(offset);
			componentType = dom instanceof MachineDom ? Type.Machine
					: Type.Context;
		}
	}

	private ICompletionProposal[] sort(final List<ICompletionProposal> proposals) {
		// sort by relevance and lexically
		Collections.sort(proposals, new ProposalComparator());
		return proposals.toArray(new ICompletionProposal[proposals.size()]);
	}

	private void computeTemplateProposals(
			final List<ICompletionProposal> proposals) {
		final Template[] templates = getTemplates();
		final TemplateContext context = createContext(viewer, region);

		for (final Template template : templates) {
			if (template.getName().startsWith(prefix)) {
				final EventBTemplateProposal proposal = new EventBTemplateProposal(
						template, context, region, getImage(template),
						getRelevance(template, prefix));

				proposals.add(proposal);
			}
		}
	}

	private void computeKeywordProposals(
			final List<ICompletionProposal> proposals) {
		if (!IDocument.DEFAULT_CONTENT_TYPE.equals(contentType)) {
			return;
		}

		final Map<String, ICompletionProposal> result = new HashMap<String, ICompletionProposal>();

		int componentRelevance = 50;
		int scopeRelevance = 30;
		addKeywords(result, Arrays.asList(Constants.formula_keywords),
				componentRelevance, scopeRelevance);

		componentRelevance = componentType == Type.Machine ? 50 : 0;
		scopeRelevance = offsetInEvents ? 30 : 0;
		addKeywords(result, Arrays.asList(Constants.event_keywords),
				componentRelevance, scopeRelevance);

		componentRelevance = componentType == Type.Machine ? 50 : 0;
		scopeRelevance = offsetInEvents ? 20 : 30;
		addKeywords(result, Arrays.asList(Constants.machine_keywords),
				componentRelevance, scopeRelevance);

		componentRelevance = componentType == Type.Context ? 50 : 0;
		scopeRelevance = offsetInEvents ? 0 : 30;
		addKeywords(result, Arrays.asList(Constants.context_keywords),
				componentRelevance, scopeRelevance);

		proposals.addAll(result.values());
	}

	private void addKeywords(final Map<String, ICompletionProposal> result,
			final List<String> keywords, final int componentRelevance,
			final int scopeRelevance) {
		for (final String keyword : keywords) {
			if (keyword.startsWith(prefix)) {
				final ICompletionProposal oldProposal = result.get(keyword);
				final int relevance = componentRelevance + scopeRelevance;

				// adjust relevance if already present
				if (oldProposal != null) {
					if (oldProposal instanceof IRelevantProposal) {
						final EventBCompletionProposal evtBProp = (EventBCompletionProposal) oldProposal;
						final int oldRelevance = evtBProp.getRelevance();

						if (oldRelevance < relevance) {
							evtBProp.changeRelevance(relevance - oldRelevance);
						}
					}
				} else {
					final EventBCompletionProposal proposal = new EventBCompletionProposal(
							keyword, region.getOffset(), prefix.length(),
							keyword.length(), null, keyword, null, "keyword '"
									+ keyword + "'");
					proposal.changeRelevance(relevance);

					result.put(keyword, proposal);
				}
			}
		}
	}

	private void computeIdentifierProposals(
			final List<ICompletionProposal> proposals) {
		if (dom == null || scopingDom == null
				|| !IDocument.DEFAULT_CONTENT_TYPE.equals(contentType)) {
			return;
		}

		final Set<String> identifiers = scopingDom.getIdentifiers();
		for (final String ident : identifiers) {
			if (ident.startsWith(prefix)) {
				final IdentifierType type = scopingDom.getIdentifierType(ident);

				final String description = createDescription(ident, type);
				final Image image = getImage(type);

				final EventBCompletionProposal proposal = new EventBCompletionProposal(
						ident, region.getOffset(), prefix.length(),
						ident.length(), image, ident, null, description);
				proposals.add(proposal);
				proposal.changeRelevance(100);
			}
		}
	}

	private String createDescription(final String ident,
			final IdentifierType type) {
		final StringBuffer description = new StringBuffer(ident);

		if (type != null) {
			switch (type) {
			case GlobalVariable:
				description.append(" : global variable");
				break;
			case LocalVariable:
				description.append(" : local variable");
				break;
			case Parameter:
				description.append(" : event parameter");
				break;
			case Constant:
				description.append(" : constant");
				break;
			case Set:
				description.append(" : carrier set");
				break;
			default:
				return null;
			}
		}

		return description.toString();
	}

	private Image getImage(final IdentifierType type) {
		if (type != null) {
			switch (type) {
			case GlobalVariable:
			case LocalVariable:
			case Parameter:
				return Images.getImage(Images.IMG_VARIABLE);
			case Constant:
				return Images.getImage(Images.IMG_CONSTANT);
			case Set:
				return Images.getImage(Images.IMG_CARRIER_SET);
			}
		}

		return null;
	}

	private int adjustOffset(final ITextViewer viewer, int offset) {
		// adjust offset to end of normalized selection
		final ITextSelection selection = (ITextSelection) viewer
				.getSelectionProvider().getSelection();
		if (selection.getOffset() == offset) {
			offset = selection.getOffset() + selection.getLength();
		}
		return offset;
	}

	@Override
	protected TemplateContextType getContextType(final ITextViewer viewer,
			final IRegion region) {
		// we don't care about context types here
		return new TemplateContextType(ContextType.Unkown.key);
	}

	@Override
	protected Image getImage(final Template template) {
		return Images.getImage(Images.IMG_TEMPLATE, Images.IMG_TEMPLATE_PATH);
	}

	private Template[] getTemplates() {
		if (templateStore == null) {
			templateStore = TextEditorPlugin.getPlugin().getTemplateStore();
		}

		/*
		 * We don't filter by context types here. They are consider when
		 * calculating the relevance.
		 */
		return templateStore.getTemplates(null);
	}

	@Override
	protected Template[] getTemplates(final String contextTypeId) {
		return getTemplates();
	}

	@Override
	protected int getRelevance(final Template template, final String prefix) {
		final String id = template.getContextTypeId();

		// These templates are relevant everywhere
		if (ContextType.Anywhere.key.equals(id)) {
			return 90;
		}

		int relevance = 0;

		if (componentType != null) {
			// 50 points for matching component
			switch (componentType) {
			case Machine:
				if (ContextType.Machine.key.equals(id)
						|| ContextType.Events.key.equals(id)) {
					relevance += 50;
				}
				break;
			case Context:
				if (ContextType.Context.key.equals(id)) {
					relevance += 50;
				}
				break;
			case Event:
				break;
			case Formula:
				break;
			case Unknown:
				break;
			default:
				break;
			}
		} else {
			// fallback: do not distinguish
			relevance += 50;
		}

		/*
		 * Templates for the events section get their points when the current
		 * offset is in this section.
		 */
		if (ContextType.Events.key.equals(id)) {
			if (offsetInEvents) {
				relevance += 40;
			}
		}
		// other templates get these points in all cases
		else {
			relevance += 40;
		}

		return relevance;
	}

	@Override
	public String getErrorMessage() {
		return errorMessage;
	}

	private final class ProposalComparator implements
			Comparator<ICompletionProposal> {
		@Override
		public int compare(final ICompletionProposal o1,
				final ICompletionProposal o2) {
			final int rel1 = getRelevance(o1);
			final int rel2 = getRelevance(o2);
			final int diff = rel2 - rel1;

			if (diff != 0 && rel1 >= 0 && rel2 >= 0) {
				return diff;
			}

			// fall back to alpha-numerical comparison
			return o1.getDisplayString().toLowerCase()
					.compareTo(o2.getDisplayString().toLowerCase());
		}

		private int getRelevance(final ICompletionProposal o) {

			if (o instanceof IRelevantProposal) {
				return ((IRelevantProposal) o).getRelevance();
			}

			return -1;
		}
	}
}
