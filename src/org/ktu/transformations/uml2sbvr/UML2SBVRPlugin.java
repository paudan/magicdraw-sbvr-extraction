package org.ktu.transformations.uml2sbvr;

import com.nomagic.actions.ActionsManager;
import com.nomagic.magicdraw.actions.ActionsConfiguratorsManager;
import com.nomagic.magicdraw.actions.BrowserContextAMConfigurator;
import com.nomagic.magicdraw.actions.ConfiguratorWithPriority;
import com.nomagic.magicdraw.actions.DiagramContextAMConfigurator;
import com.nomagic.magicdraw.actions.MDActionsCategory;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.plugins.Plugin;
import com.nomagic.magicdraw.ui.browser.Node;
import com.nomagic.magicdraw.ui.browser.Tree;
import com.nomagic.magicdraw.uml.DiagramTypeConstants;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.magicdraw.uml.symbols.PresentationElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Diagram;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;
import java.util.ResourceBundle;
import org.ktu.transformations.uml2sbvr.actions.BrowserCombinedAction;
import org.ktu.transformations.uml2sbvr.actions.BrowserM2MAction;
import org.ktu.transformations.uml2sbvr.actions.BrowserWizardAction;
import org.ktu.transformations.uml2sbvr.actions.DiagramCombinedAction;
import org.ktu.transformations.uml2sbvr.actions.DiagramM2MAction;
import org.ktu.transformations.uml2sbvr.actions.DiagramWizardAction;

public class UML2SBVRPlugin extends Plugin {

    private static UML2SBVRPlugin instance;
    private static final Object instanceLock = new Object();
    
    private static final ResourceBundle messages = ResourceBundle.getBundle("org/ktu/transformations/uml2sbvr/messages");

    @Override
    public boolean close() {
        return true;
    }

    @Override
    public void init() {
        DiagramContextAMConfigurator diagCfg = new DiagramContextAMConfigurator() {

            @Override
            public int getPriority() {
                return ConfiguratorWithPriority.MEDIUM_PRIORITY;
            }

            @Override
            public void configure(ActionsManager manager, DiagramPresentationElement arg1,
                    PresentationElement[] arg2, PresentationElement arg3) {
                if (Application.getInstance().getProject() == null)
                    return;
                final MDActionsCategory category = new MDActionsCategory(messages.getString("UML2SBVRPlugin.1"), messages.getString("UML2SBVRPlugin.2"));
                category.setNested(true);
                category.addAction(new DiagramM2MAction("", messages.getString("UML2SBVRPlugin.3")));
                category.addAction(new DiagramWizardAction("", messages.getString("UML2SBVRPlugin.4")));
                category.addAction(new DiagramCombinedAction("", messages.getString("UML2SBVRPlugin.5")));
                manager.addCategory(category);
            }
        };
        ActionsConfiguratorsManager.getInstance().addDiagramContextConfigurator(DiagramTypeConstants.UML_USECASE_DIAGRAM, diagCfg);

        BrowserContextAMConfigurator browserCfg = new BrowserContextAMConfigurator() {
            @Override
            public void configure(ActionsManager manager, Tree tree) {
                Node node = tree.getSelectedNode();
                if (node == null)
                    return;
                Object el = node.getUserObject();
                if (Application.getInstance().getProject() == null || (el != null && !(el instanceof Diagram || el instanceof Package)))
                    return;
                final MDActionsCategory category = new MDActionsCategory(messages.getString("UML2SBVRPlugin.1"), messages.getString("UML2SBVRPlugin.2"));
                category.setNested(true);
                category.addAction(new BrowserM2MAction("", messages.getString("UML2SBVRPlugin.3")));
                category.addAction(new BrowserWizardAction("", messages.getString("UML2SBVRPlugin.4")));
                category.addAction(new BrowserCombinedAction("", messages.getString("UML2SBVRPlugin.5")));
                manager.addCategory(category);
            }

            @Override
            public int getPriority() {
                return ConfiguratorWithPriority.MEDIUM_PRIORITY;
            }
        };
        ActionsConfiguratorsManager.getInstance().addContainmentBrowserContextConfigurator(browserCfg);
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    public UML2SBVRPlugin() {
        synchronized (instanceLock) {
            instance = this;
        }
    }

    public static UML2SBVRPlugin getInstance() {
        synchronized (instanceLock) {
            return instance;
        }
    }

}
