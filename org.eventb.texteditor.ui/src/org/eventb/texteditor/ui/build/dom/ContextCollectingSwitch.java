/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.build.dom;

import org.eclipse.core.runtime.Assert;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.resource.Resource;
import org.eventb.emf.core.EventBObject;
import org.eventb.emf.core.context.Axiom;
import org.eventb.emf.core.context.CarrierSet;
import org.eventb.emf.core.context.Constant;
import org.eventb.emf.core.context.Context;
import org.eventb.emf.core.context.util.ContextSwitch;
import org.eventb.emf.formulas.BFormula;
import org.eventb.texttools.formulas.ExtensionHelper;

public class ContextCollectingSwitch extends ContextSwitch<Boolean> {
	private final ContextDom contextDom;
	private final Resource contextResource;

	protected ContextCollectingSwitch(final ContextDom dom,
			final Resource contextResource) {
		contextDom = dom;
		this.contextResource = contextResource;
	}

	protected AbstractDom getDom() {
		return contextDom;
	}

	public IDom getCurrentParentDom() {
		return contextDom;
	}

	@Override
	public Boolean caseEventBObject(final EventBObject object) {
		// default to avoid NPEs
		return true;
	}

	@Override
	public Boolean caseContext(final Context object) {
		contextDom.setContext(object);

		final EList<Context> extendedContexts = object.getExtends();
		for (final Context context : extendedContexts) {
			final IComponentDom dom = DomBuilder.getReferencedDom(context,
					contextResource);

			if (dom != null) {
				Assert.isTrue(dom instanceof ContextDom);
				contextDom.addExtendedContext((ContextDom) dom);
			}

			/*
			 * If no DOM is found the reference must be to a not-existing
			 * machine.
			 */
		}

		return true;
	}

	@Override
	public Boolean caseAxiom(final Axiom object) {
		final BFormula formula = ExtensionHelper.getFormula(object
				.getExtensions());

		if (formula != null) {
			final FormulaDom formDom = new FormulaDom(formula, contextDom);
			contextDom.addChild(formDom);
		}

		return false;
	}

	@Override
	public Boolean caseConstant(final Constant object) {
		contextDom.addConstant(object);
		return false;
	}

	@Override
	public Boolean caseCarrierSet(final CarrierSet object) {
		contextDom.addSet(object);
		return false;
	}
}
