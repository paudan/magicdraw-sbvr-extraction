package org.ktu.transformations.uml2sbvr.actions;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.ui.browser.actions.DefaultBrowserAction;
import com.nomagic.magicdraw.uml.BaseElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Diagram;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;
import java.awt.event.ActionEvent;

public class BrowserCombinedAction extends DefaultBrowserAction {

    public BrowserCombinedAction(String arg0, String arg1) {
        super(arg0, arg1, null, null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        BaseElement element = this.getFirstElement();
        Project proj = Application.getInstance().getProject();
        if (proj == null)
            return;
        if (element instanceof Diagram)
            new PluginUseCaseAction(proj.getDiagram((Diagram) element)).performTransformAction(true);
        else if (element instanceof Package) 
            new PluginUseCaseAction((Package) element).performTransformAction(true);
    }
}
