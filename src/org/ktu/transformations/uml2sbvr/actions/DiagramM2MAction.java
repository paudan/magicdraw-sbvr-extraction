package org.ktu.transformations.uml2sbvr.actions;

import com.nomagic.magicdraw.ui.actions.DefaultDiagramAction;
import java.awt.event.ActionEvent;

@SuppressWarnings("serial")
public class DiagramM2MAction extends DefaultDiagramAction {

    public DiagramM2MAction(String id, String name) {
        super(id, name, null, null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new PluginUseCaseAction(this.getDiagram()).performTransformAction(false);
    }

}
