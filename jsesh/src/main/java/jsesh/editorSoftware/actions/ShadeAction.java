/**
 * author : Serge ROSMORDUC
 * This file is distributed according to the LGPL (GNU lesser public license)
 */
package jsesh.editorSoftware.actions;

import java.awt.event.ActionEvent;

import jsesh.editorSoftware.MDCDisplayerAppliWorkflow;
import jsesh.editorSoftware.actions.generic.MDCIconAction;

public class ShadeAction extends MDCIconAction {

	/**
	 * The shading to apply to the current cadrat.
	 */
	private int shade;

	/**
	 * @param shade
	 * @param mdc
	 * @param workflow TODO
	 */
	public ShadeAction(int shade, String mdc, MDCDisplayerAppliWorkflow workflow) {
		super(workflow,mdc);
		this.shade = shade;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		workflow.doShade(shade);
	}
}