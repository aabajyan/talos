package com.talosvfx.talos.editor.layouts;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.talosvfx.talos.editor.project2.SharedResources;
import com.talosvfx.talos.editor.widgets.ui.common.ColorLibrary;
import com.talosvfx.talos.editor.widgets.ui.menu.BasicPopup;
import lombok.Getter;

import java.util.UUID;

public class DummyLayoutApp implements LayoutApp {

    private String tabName;

    private String uuid;

    private transient TabWidget tabWidget;
    private transient Actor mainContent;
    private transient Skin skin;
    @Getter
    private DestroyCallback destroyCallback;
    private boolean active;
    private VisLabel visLabel;
    private LayoutContent layoutContent;
    private boolean currentFocusState;

    private static class TabWidget extends Table {

        private Table highlight = new Table();


        public TabWidget () {
            highlight.setFillParent(true);
            addActor(highlight);

            Table highlightPixel = new Table();
            highlightPixel.setBackground(SharedResources.skin.newDrawable("white", ColorLibrary.BORDER_BLUE));

            highlight.top();
            highlight.defaults().top();
            highlight.add(highlightPixel).growX().height(4).top();

            highlight.setVisible(false);
        }

        public void setFocused (boolean focused) {
            highlight.setVisible(focused);
            highlight.toFront();
        }
    }

    public DummyLayoutApp (Skin skin, String tabName) {
        this.tabName = tabName;
        build(skin);
        uuid = UUID.randomUUID().toString();
    }

    public void build (Skin skin) {
        this.skin = skin;

        tabWidget = createTab(tabName);
        mainContent = createMainContent();
    }

    /**
     * please override this in your apps to do shit with it
     * @param popup
     */
    protected void createPopupActions (BasicPopup<String> popup) {

    }

    protected void popupButtonClicked (String payload) {

    }

    private TabWidget createTab (String tabName) {
        TabWidget tab = new TabWidget();
        tab.setTouchable(Touchable.enabled);
        tab.setBackground(ColorLibrary.obtainBackground(skin, ColorLibrary.SHAPE_SQUIRCLE_TOP, ColorLibrary.BackgroundColor.LIGHT_GRAY));

        tab.padLeft(10);
        tab.padRight(10);
        visLabel = new VisLabel(tabName.substring(0, Math.min(10, tabName.length())));
        tab.add(visLabel).pad(5).padLeft(0).left().maxWidth(300).minWidth(0);
        visLabel.setAlignment(Align.left);
        visLabel.setEllipsis(true);

        ImageButton actor = new ImageButton(skin.getDrawable("ic-vertical-dots"));
        actor.addListener(new ClickListener() {
            @Override
            public void clicked (InputEvent event, float x, float y) {
                super.clicked(event, x, y);

                BasicPopup<String> popup = BasicPopup.build(String.class)
                        .addItem("Maximize", "maximize")
                        .addItem("Close Tab", "close");

                createPopupActions(popup);
                popup.onClick(new BasicPopup.PopupListener<String>() {
                    @Override
                    public void itemClicked (String payload) {
                        if (payload.equals("close")) {
                            if (destroyCallback != null) {
                                destroyCallback.onDestroyRequest();
                            }

                            return;
                        }

                        popupButtonClicked(payload);
                    }
                }).show(actor, x, y);

            }
        });
        actor.getStyle().up = null;
        actor.getImage().setScaling(Scaling.fill);
        tab.add(actor).size(16).padLeft(5).padRight(-10);

        return tab;
    }

    @Override
    public void setTabActive (boolean active) {
        this.active = active;

        if (active) {
            tabWidget.setBackground(ColorLibrary.obtainBackground(skin, ColorLibrary.SHAPE_SQUIRCLE_TOP, ColorLibrary.BackgroundColor.LIGHT_GRAY));
        } else {
            tabWidget.setBackground(ColorLibrary.obtainBackground(skin, ColorLibrary.SHAPE_SQUIRCLE_TOP, ColorLibrary.BackgroundColor.DARK_GRAY));
        }
    }

    @Override
    public void setTabFocused (boolean focused) {
        boolean shouldFocus = isTabActive() && focused;

        if (currentFocusState != shouldFocus) {
            tabWidget.setFocused(shouldFocus);
            onTouchFocused();
        }

        currentFocusState = shouldFocus;
    }

    protected void onTouchFocused () {

    }

    @Override
    public boolean isTabActive () {
        return active;
    }

    private Actor createMainContent () {
        Table table = new Table();
        table.setBackground(skin.newDrawable("white", 0.2f, 0.2f, 0.2f, 1f));
        return table;
    }

    @Override
    public String getUniqueIdentifier () {
        return uuid;
    }

    @Override
    public void setUniqueIdentifier (String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getFriendlyName () {
        return tabName;
    }

    @Override
    public Actor getTabWidget () {
        return tabWidget;
    }

    @Override
    public Actor copyTabWidget () {
        return createTab(tabName);
    }

    @Override
    public Actor getMainContent () {
        return mainContent;
    }

    @Override
    public Actor getCopyMainContent () {
        Table table = new Table();
        table.setBackground(skin.newDrawable("white", 0.5f, 0.5f, 0.5f, 1f));
        return table;
    }

    @Override
    public DestroyCallback getDestroyCallback () {
        return destroyCallback;
    }

    @Override
    public void setDestroyCallback (DestroyCallback destroyCallback) {
        this.destroyCallback = destroyCallback;
    }

    @Override
    public void setScrollFocus () {

    }

    @Override
    public void onInputProcessorAdded () {

    }

    @Override
    public void onInputProcessorRemoved () {

    }

    @Override
    public void updateTabName (String name) {
        visLabel.setText(name);
    }

    @Override
    public void setLayoutContent (LayoutContent layoutContent) {
        this.layoutContent = layoutContent;
    }

    @Override
    public LayoutContent getLayoutContent () {
        return layoutContent;
    }
}
