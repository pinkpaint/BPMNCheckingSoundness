package org.processmining.analysis.causality;

import javax.swing.JComponent;

import org.processmining.analysis.AnalysisInputItem;
import org.processmining.analysis.AnalysisPlugin;
import org.processmining.framework.models.ModelGraph;
import org.processmining.framework.models.causality.CausalFootprint;
import org.processmining.framework.models.causality.CausalityFootprintFactory;
import org.processmining.framework.plugin.ProvidedObject;

/**
 * <p>Title: </p>
 *
 * <p>Description: This plugin present Causality Structure AnalysisPlugin </p>
 *
 * <p>Copyright: Copyright (c) 2015</p>
 *
 */
public class CausalityStructureAnalysisPlugin implements AnalysisPlugin {
	public CausalityStructureAnalysisPlugin() {
	}

	public String getName() {
		return "Causal footprint analyzer";
	}

	public AnalysisInputItem[] getInputItems() {
		AnalysisInputItem[] items = {
									new AnalysisInputItem("Causal footprint") {
			public boolean accepts(ProvidedObject object) {
				int i = 0;
				boolean b = false;
				while (!b && (i < object.getObjects().length)) {
					b |= (object.getObjects()[i] instanceof CausalFootprint);
					b |= CausalityFootprintFactory.canConvert(object.getObjects()[i]);
					i++;
				}
				return b;
			}
		}
		} ;
		return items;
	}

	public JComponent analyse(AnalysisInputItem[] inputs) {
		Object[] o = (inputs[0].getProvidedObjects())[0].getObjects();
		int i = 0;
		boolean b = false;
		while (!b && (i < o.length)) {
			b |= (o[i] instanceof CausalFootprint);
			b |= CausalityFootprintFactory.canConvert(o[i]);
			i++;
		}

		CausalFootprint cs;

		Object o2 = o[i - 1];
		if (!(o2 instanceof CausalFootprint)) {
			cs = CausalityFootprintFactory.make(o2);
			if (cs != null) {
				cs.Test("DerivedFootprint");
				return new CausalFootprintAnalysisResult(cs,
						((ModelGraph) o2), (inputs[0].getProvidedObjects())[0]);
			} else {
				return null;
			}
		} else {
			cs = (CausalFootprint) o2;
			cs.Test("ReceivedFootprint");
			return new CausalFootprintAnalysisResult(cs, null, null);
		}

	}

	public String getHtmlDescription() {
		return "Analyses a causality structure for one of the erroneous patterns." +
				" It accepts EPCs, Petri nets, BPMN and causality structures. <br/>"+
				"First, select one BPMN process (.ibp) file to import. "+
				"<br/>Then, choose Causal Footprint Analyzer."
				+"<br/> Final, choose one erroneous pattern to find. There are 4 erroneous patterns: "
				+ "<br/> 1. Deadlock pattern"
				+ "<br/> 2. Multiple pattern"
				+ "<br/> 3. Singular Trap pattern"
				+ "<br/> 4. Generalized Trap pattern";
	}
}
