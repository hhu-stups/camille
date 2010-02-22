package org.eventb.texttools.internal.parsing;

import junit.framework.TestCase;

import org.eclipse.emf.common.util.EList;
import org.eventb.emf.core.machine.Machine;
import org.eventb.emf.core.machine.MachineFactory;

public class CoreModelTest extends TestCase {

	public void testRefinesNames() throws Exception {
		Machine machine = MachineFactory.eINSTANCE.createMachine();
		EList<String> list = machine.getRefinesNames();

		list.add(0, "AbstractMac3");
		list.add(0, "AbstractMac2");
		list.add(0, "AbstractMac1");

		assertEquals(3, machine.getRefinesNames().size());
		assertEquals(3, machine.getRefines().size());
	}
}
