package com.talosvfx.talos.editor.addons.scene.apps;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.kotcrab.vis.ui.widget.VisWindow;
import com.talosvfx.talos.TalosMain;

public abstract class AEditorApp<T> {

    protected Table content;
    protected String identifier;
    protected T object;

    protected AppListener listener;

    interface AppListener {
        void closeRequested();
    }

    public boolean notifyClose() {

        if(listener != null) {
            listener.closeRequested();
        }

        return true;
    }

    public enum AppOpenStrategy {
        WINDOW,
        BOTTOM_TAB,
        RIGHT_TAB
    }

    public AEditorApp(T object) {
        this.object = object;
    }

    public abstract void initContent();

    public abstract String getTitle();

    public Table getContent() {
        return content;
    }

    public void addListener(AppListener listener) {
        this.listener = listener;
    }
}
