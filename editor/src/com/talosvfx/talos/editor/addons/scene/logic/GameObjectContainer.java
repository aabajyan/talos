package com.talosvfx.talos.editor.addons.scene.logic;

import com.badlogic.gdx.utils.Array;
import com.talosvfx.talos.editor.addons.scene.logic.components.IComponent;

public interface GameObjectContainer {

    String getName();
    void setName(String name);
    Array<GameObject> getGameObjects();
    Iterable<IComponent> getComponents();
    void addGameObject(GameObject gameObject);
    Array<GameObject> deleteGameObject(GameObject gameObject);
    void removeObject(GameObject gameObject);
    void addComponent(IComponent component);

    boolean hasGOWithName (String name);

    void clearChildren (Array<GameObject> tmp);

    GameObject getParent();
    GameObject getSelfObject();
    void setParent(GameObject gameObject);
}
