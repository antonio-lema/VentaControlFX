package com.mycompany.ventacontrolfx.presentation.controller.sidebar;

import com.mycompany.ventacontrolfx.presentation.util.RippleEffect;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import java.util.List;

/**
 * Gestor de la experiencia de usuario y animaciones del Sidebar.
 */
public class SidebarUIManager {

    private final List<VBox> contentPanes;
    private final List<Button> sectionHeaders;
    private VBox activeSection;

    public SidebarUIManager(List<VBox> contentPanes, List<Button> sectionHeaders) {
        this.contentPanes = contentPanes;
        this.sectionHeaders = sectionHeaders;
    }

    public void applyEffects(List<Button> buttons) {
        for (Button b : buttons) if (b != null) RippleEffect.applyTo(b);
    }

    public void collapseAll(boolean force) {
        for (int i = 0; i < contentPanes.size(); i++) {
            VBox pane = contentPanes.get(i);
            if (force || activeSection != pane) {
                setVisible(pane, false);
                if (i < sectionHeaders.size()) sectionHeaders.get(i).getStyleClass().remove("sidebar-section-header-active");
            }
        }
    }

    public void toggleSection(VBox container, Button header) {
        if (container == null) return;
        boolean wasVisible = container.isVisible();
        collapseAll(true);
        if (!wasVisible) {
            setVisible(container, true);
            if (header != null) header.getStyleClass().add("sidebar-section-header-active");
            this.activeSection = container;
        }
    }

    public void setActiveButton(Button activeBtn, List<Button> allButtons) {
        for (Button b : allButtons) if (b != null) b.getStyleClass().remove("active-sidebar-button");
        if (activeBtn != null) {
            activeBtn.getStyleClass().add("active-sidebar-button");
            expandParentOf(activeBtn);
        }
    }

    private void expandParentOf(Button btn) {
        if (btn == null || btn.getParent() == null) return;
        if (btn.getParent() instanceof VBox) {
            VBox parent = (VBox) btn.getParent();
            if (parent.getStyleClass().contains("sidebar-section-content")) {
                setVisible(parent, true);
                this.activeSection = parent;
                // Highlight header
                for (int i = 0; i < contentPanes.size(); i++) {
                    if (contentPanes.get(i) == parent && i < sectionHeaders.size()) {
                        sectionHeaders.get(i).getStyleClass().add("sidebar-section-header-active");
                    }
                }
            }
        }
    }

    private void setVisible(Node node, boolean visible) {
        if (node != null) { node.setVisible(visible); node.setManaged(visible); }
    }
}


