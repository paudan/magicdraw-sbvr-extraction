package org.ktu.transformations.uml2sbvr.actions;

import com.nomagic.magicdraw.ui.actions.DefaultDiagramAction;
import java.awt.event.ActionEvent;

@SuppressWarnings("serial")
public class DiagramWizardAction extends DefaultDiagramAction {

    public DiagramWizardAction(String id, String name) {
        super(id, name, null, null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new PluginUseCaseAction(this.getDiagram()).performWizardAction();
    }

}
