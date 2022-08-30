package com.talosvfx.talos.editor.addons.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.esotericsoftware.spine.TalosSkeletonRenderer;
import com.talosvfx.talos.editor.addons.scene.assets.AssetRepository;
import com.talosvfx.talos.editor.addons.scene.assets.GameAsset;
import com.talosvfx.talos.editor.addons.scene.assets.GameAssetType;
import com.talosvfx.talos.editor.addons.scene.assets.RawAsset;
import com.talosvfx.talos.editor.addons.scene.events.ComponentUpdated;
import com.talosvfx.talos.editor.addons.scene.logic.GameObject;
import com.talosvfx.talos.editor.addons.scene.logic.components.*;
import com.talosvfx.talos.editor.addons.scene.maps.GridPosition;
import com.talosvfx.talos.editor.addons.scene.maps.StaticTile;
import com.talosvfx.talos.editor.addons.scene.maps.TalosMapRenderer;
import com.talosvfx.talos.editor.addons.scene.utils.AMetadata;
import com.talosvfx.talos.editor.addons.scene.utils.EntitySelectionBuffer;
import com.talosvfx.talos.editor.addons.scene.utils.PolyBatchWithEncodingOverride;
import com.talosvfx.talos.editor.addons.scene.utils.metadata.SpriteMetadata;
import com.talosvfx.talos.editor.addons.scene.widgets.gizmos.Gizmo;
import com.talosvfx.talos.editor.notifications.EventHandler;
import com.talosvfx.talos.editor.notifications.Notifications;
import com.talosvfx.talos.runtime.ParticleEffectDescriptor;
import com.talosvfx.talos.runtime.ParticleEffectInstance;
import com.talosvfx.talos.runtime.render.SpriteBatchParticleRenderer;

import java.util.Comparator;

public class MainRenderer implements Notifications.Observer {

    public final Comparator<GameObject> layerAndDrawOrderComparator;

    private  Comparator<GameObject> activeSorter;

    private TransformComponent tempTransform = new TransformComponent();
    private Vector2 vec = new Vector2();
    private Vector2[] points = new Vector2[4];

    private ObjectMap<String, Integer> layerOrderLookup = new ObjectMap<>();

    private static final int LB = 0;
    private static final int LT = 1;
    private static final int RT = 2;
    private static final int RB = 3;

    private ObjectMap<Texture, NinePatch> patchCache = new ObjectMap<>();
    private ObjectMap<ParticleComponent, ParticleEffectInstance> particleCache = new ObjectMap<>();

    private SpriteBatchParticleRenderer talosRenderer;
    private TalosSkeletonRenderer spineRenderer;

    private TalosMapRenderer mapRenderer;
    private ShapeRenderer shapeRenderer;

    private TextureRegion textureRegion = new TextureRegion();
    private OrthographicCamera camera;

    private boolean renderParentTiles = false;

    public boolean skipUpdates = false;

    public static class RenderState {
        private Array<GameObject> list = new Array<>();
    }

    public MainRenderer() {
        for (int i = 0; i < 4; i++) {
            points[i] = new Vector2();
        }

        Notifications.registerObserver(this);

        talosRenderer = new SpriteBatchParticleRenderer();
        spineRenderer = new TalosSkeletonRenderer();
        mapRenderer = new TalosMapRenderer();
        shapeRenderer = new ShapeRenderer();

        layerAndDrawOrderComparator = new Comparator<GameObject>() {
            @Override
            public int compare (GameObject o1, GameObject o2) {

                float aSort = MainRenderer.getDrawOrderSafe(o1);
                float bSort = MainRenderer.getDrawOrderSafe(o2);

                return Float.compare(aSort, bSort);
            }
        };

        activeSorter = layerAndDrawOrderComparator;
    }

    public static float getDrawOrderSafe (GameObject gameObject) {
        if (gameObject.hasComponentType(RendererComponent.class)) {
            RendererComponent rendererComponent = gameObject.getComponentAssignableFrom(RendererComponent.class);
            return rendererComponent.orderingInLayer;
        }
        return -55;
    }

    public void setActiveSorter (Comparator<GameObject> customSorter) {
        this.activeSorter = customSorter;
    }

    public void update (GameObject root) {
        if (!root.active || !root.isEditorVisible()) return;
        if (root.hasComponent(TransformComponent.class)) {
            TransformComponent transform = root.getComponent(TransformComponent.class);


            transform.worldPosition.set(transform.position);
            transform.worldScale.set(transform.scale);
            transform.worldRotation = transform.rotation;

            if (root.parent != null) {

                if (root.parent.hasComponent(TransformComponent.class)) {
                    //Combine our world with the parent

                    TransformComponent parentTransform = root.parent.getComponent(TransformComponent.class);
                    transform.worldPosition.scl(parentTransform.worldScale);
                    transform.worldPosition.rotateDeg(parentTransform.worldRotation);
                    transform.worldPosition.add(parentTransform.worldPosition);

                    transform.worldRotation += parentTransform.worldRotation;
                    transform.worldScale.scl(parentTransform.worldScale);
                }
            }
        }

        if (root.getGameObjects() != null) {
            for (int i = 0; i < root.getGameObjects().size; i++) {
                GameObject child = root.getGameObjects().get(i);
                update(child);
            }
        }
    }

    private void fillRenderableEntities (Array<GameObject> rootObjects, Array<GameObject> list) {
        for (GameObject root : rootObjects) {
            if (!root.active || !root.isEditorVisible()) continue;

            boolean childrenVisibleFlag = true;
            if (root.hasComponentType(RendererComponent.class)) {
                RendererComponent rendererComponent = root.getComponentAssignableFrom(RendererComponent.class);
                childrenVisibleFlag = rendererComponent.childrenVisible;
                if (rendererComponent.visible) {
                    list.add(root);
                }
            }
            if (childrenVisibleFlag) {
                if (root.getGameObjects() != null) {
                    fillRenderableEntities(root.getGameObjects(), list);
                }
            }
        }

    }


    Array<GameObject> temp = new Array<>();
    public void render (Batch batch, RenderState state, GameObject root) {
        temp.clear();
        temp.add(root);
        render(batch, state, temp);
    }
    public void render (Batch batch, RenderState state, Array<GameObject> rootObjects) {
        mapRenderer.setCamera(this.camera);

        updateLayerOrderLookup();

        //fill entities
        state.list.clear();
        fillRenderableEntities(rootObjects, state.list);
        sort(state.list);

        batch.end();
        if (renderParentTiles) {

            Gdx.gl.glEnable(GL20.GL_BLEND);
            Color color = Color.valueOf("459534");
            color.a = 0.5f;
            shapeRenderer.setColor(color);
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);


            for (int i = 0; i < state.list.size; i++) {
                GameObject gameObject = state.list.get(i);
                if (gameObject.hasComponent(TileDataComponent.class)) {

                    TransformComponent transformComponent = gameObject.getComponent(TransformComponent.class);

                    Gizmo.TransformSettings transformSettings = gameObject.getTransformSettings();

                    TileDataComponent tileDataComponent = gameObject.getComponent(TileDataComponent.class);
                    GridPosition bottomLeftParentTile = tileDataComponent.getBottomLeftParentTile();

                    for (GridPosition parentTile : tileDataComponent.getParentTiles()) {

                        if (gameObject.isPlacing) {
                            float posY = parentTile.getIntY();
                            float posX = parentTile.getIntX();

                            posX -= transformSettings.transformOffsetX;
                            posY -= transformSettings.transformOffsetY;

                            posX += transformComponent.worldPosition.x;
                            posY += transformComponent.worldPosition.y;

                            posX -= transformSettings.offsetX;
                            posY -= transformSettings.offsetY;

                            shapeRenderer.rect(posX, posY, 1,1);

                        } else {
                            float posY = parentTile.getIntY();
                            float posX = parentTile.getIntX();

                            posX -= tileDataComponent.getVisualOffset().x;
                            posY -= tileDataComponent.getVisualOffset().y;

                            posX += transformComponent.worldPosition.x;
                            posY += transformComponent.worldPosition.y;

                            posX -= bottomLeftParentTile.getIntX();
                            posY -= bottomLeftParentTile.getIntY();

                            shapeRenderer.rect(posX, posY, 1,1);

                        }



                    }
                }

            }
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
        batch.begin();

        for (int i = 0; i < state.list.size; i++) {
            GameObject gameObject = state.list.get(i);


            if (batch instanceof PolyBatchWithEncodingOverride) {
                Color colourForEntityUUID = EntitySelectionBuffer.getColourForEntityUUID(gameObject);
                ((PolyBatchWithEncodingOverride)batch).setCustomEncodingColour(colourForEntityUUID.r, colourForEntityUUID.g, colourForEntityUUID.b, colourForEntityUUID.a);
            }


            GameResourceOwner<?> resourceComponent = gameObject.getRenderResourceComponent();
            if (resourceComponent != null) {
                //Its something with a game resource

                GameAsset<?> gameResource = resourceComponent.getGameResource();
                if (gameResource == null || gameResource.isBroken()) {
                    //Render the broken sprite

                    renderBrokenComponent(batch, gameObject, gameObject.getComponent(TransformComponent.class));
                    continue;
                }
            }

            if(gameObject.hasComponent(SpriteRendererComponent.class)) {
                renderSprite(batch, gameObject);
            } else if(gameObject.hasComponent(ParticleComponent.class)) {
                renderParticle(batch, gameObject);
            } else if(gameObject.hasComponent(SpineRendererComponent.class)) {
                renderSpine(batch, gameObject);
            } else if(gameObject.hasComponent(MapComponent.class)) {
                renderMap(batch, gameObject);
            }
        }
    }



    private void renderBrokenComponent (Batch batch, GameObject gameObject, TransformComponent transformComponent) {


        batch.draw(AssetRepository.getInstance().brokenTextureRegion,
                transformComponent.worldPosition.x - 0.5f, transformComponent.worldPosition.y - 0.5f,
                0.5f, 0.5f,
                1f, 1f,
                transformComponent.worldScale.x, transformComponent.worldScale.y,
                transformComponent.worldRotation);
    }

    private void renderSpine (Batch batch, GameObject gameObject) {
        TransformComponent transformComponent = gameObject.getComponent(TransformComponent.class);
        SpineRendererComponent spineRendererComponent = gameObject.getComponent(SpineRendererComponent.class);

        spineRendererComponent.skeleton.setPosition(transformComponent.worldPosition.x, transformComponent.worldPosition.y);
        spineRendererComponent.skeleton.setScale(transformComponent.worldScale.x * spineRendererComponent.scale, transformComponent.worldScale.y * spineRendererComponent.scale);

        if (!skipUpdates) {
            spineRendererComponent.animationState.update(Gdx.graphics.getDeltaTime());
            spineRendererComponent.animationState.apply(spineRendererComponent.skeleton);
        }
        spineRendererComponent.skeleton.updateWorldTransform();

        spineRenderer.draw(batch, spineRendererComponent.skeleton);

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void renderParticle (Batch batch, GameObject gameObject) {
        TransformComponent transformComponent = gameObject.getComponent(TransformComponent.class);
        ParticleComponent particleComponent = gameObject.getComponent(ParticleComponent.class);

        ParticleEffectInstance instance = obtainParticle(gameObject, particleComponent.gameAsset.getResource());
        instance.setPosition(transformComponent.worldPosition.x, transformComponent.worldPosition.y);

        if (!skipUpdates) {
            instance.update(Gdx.graphics.getDeltaTime());
        }
        talosRenderer.setBatch(batch);
        talosRenderer.render(instance);
    }

    private void renderSprite (Batch batch, GameObject gameObject) {
        TransformComponent transformComponent = gameObject.getComponent(TransformComponent.class);

        SpriteRendererComponent spriteRenderer = gameObject.getComponent(SpriteRendererComponent.class);
        GameAsset<Texture> gameResource = spriteRenderer.getGameResource();
        RawAsset rootRawAsset = gameResource.getRootRawAsset();
        AMetadata metaData = rootRawAsset.metaData;
        if (metaData instanceof SpriteMetadata) {
            //It should be
            SpriteMetadata metadata = (SpriteMetadata)metaData;

            Texture resource = spriteRenderer.getGameResource().getResource();
            textureRegion.setRegion(resource);
            if(textureRegion != null) {
                batch.setColor(spriteRenderer.color);

                final float width = spriteRenderer.size.x;
                final float height = spriteRenderer.size.y;

                if(metadata != null && metadata.borderData != null && spriteRenderer.renderMode == SpriteRendererComponent.RenderMode.sliced) {
                    Texture texture = textureRegion.getTexture(); // todo: pelase fix me, i am such a shit
                    NinePatch patch = obtainNinePatch(texture, metadata);// todo: this has to be done better
                    //todo: and this renders wrong so this needs fixing too
                    float xSign = width < 0 ? -1 : 1;
                    float ySign = height < 0 ? -1 : 1;

                    patch.draw(batch,
                            transformComponent.worldPosition.x - 0.5f * width * xSign, transformComponent.worldPosition.y - 0.5f * height * ySign,
                            0.5f * width * xSign, 0.5f * height * ySign,
                            Math.abs(width), Math.abs(height),
                            xSign * transformComponent.worldScale.x, ySign * transformComponent.worldScale.y,
                            transformComponent.worldRotation);
                } else if(spriteRenderer.renderMode == SpriteRendererComponent.RenderMode.tiled) {
                    textureRegion.getTexture().setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

                    float repeatX = width / (textureRegion.getTexture().getWidth() / metadata.pixelsPerUnit);
                    float repeatY = height / (textureRegion.getTexture().getHeight() / metadata.pixelsPerUnit);
                    textureRegion.setRegion(0, 0, repeatX, repeatY);

                    batch.draw(textureRegion,
                        transformComponent.worldPosition.x - 0.5f, transformComponent.worldPosition.y - 0.5f,
                            0.5f, 0.5f,
                            1f, 1f,
                            width * transformComponent.worldScale.x, height * transformComponent.worldScale.y,
                            transformComponent.worldRotation);
                } else if(spriteRenderer.renderMode == SpriteRendererComponent.RenderMode.simple) {
                    textureRegion.getTexture().setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
                    textureRegion.setRegion(0, 0, textureRegion.getTexture().getWidth(), textureRegion.getTexture().getHeight());

                    batch.draw(textureRegion,
                        transformComponent.worldPosition.x - 0.5f, transformComponent.worldPosition.y - 0.5f,
                            0.5f, 0.5f,
                            1f, 1f,
                            width * transformComponent.worldScale.x, height * transformComponent.worldScale.y,
                            transformComponent.worldRotation);
                }

                batch.setColor(Color.WHITE);
            }
        }

    }

    private void renderMap (Batch batch, GameObject gameObject) {
        //We render the map with its own main renderer, its own sorter
        MapComponent map = gameObject.getComponent(MapComponent.class);

        mapRenderer.render(this, batch, gameObject, map);
    }

    public void renderStaticTileDynamic (StaticTile staticTile, Batch batch, float tileSizeX, float tileSizeY) {
        GridPosition gridPosition = staticTile.getGridPosition();
        GameAsset<?> staticTilesAsset = staticTile.getStaticTilesAsset();
        if (staticTilesAsset.type == GameAssetType.SPRITE) {
            GameAsset<Texture> texGameAsset = (GameAsset<Texture>)staticTilesAsset;
            Texture resource = texGameAsset.getResource();

            batch.draw(resource, gridPosition.getIntX(), gridPosition.getIntY(), tileSizeX, tileSizeY);
        }
    }

    private NinePatch obtainNinePatch (Texture texture, SpriteMetadata metadata) {
        if(false && patchCache.containsKey(texture)) { //something better, maybe hash on pixel size + texture for this
            return patchCache.get(texture);
        } else {
            NinePatch patch = new NinePatch(texture, metadata.borderData[0], metadata.borderData[1], metadata.borderData[2], metadata.borderData[3]);
            patch.scale(1/metadata.pixelsPerUnit, 1/metadata.pixelsPerUnit); // fix this later
            patchCache.put(texture, patch);
            return patch;
        }
    }

    private ParticleEffectInstance obtainParticle (GameObject gameObject, ParticleEffectDescriptor descriptor) {
        ParticleComponent component = gameObject.getComponent(ParticleComponent.class);

        if(particleCache.containsKey(component)) {
            return particleCache.get(component);
        } else {
            ParticleEffectInstance instance = descriptor.createEffectInstance();
            particleCache.put(component, instance);
            return instance;
        }
    }

    private void updateLayerOrderLookup () {
        Array<String> layerList = SceneEditorAddon.get().workspace.getLayerList();
        layerOrderLookup.clear();
        int i = 0;
        for(String layer: layerList) {
            layerOrderLookup.put(layer, i++);
        }
    }

    private void sort (Array<GameObject> list) {
        list.sort(activeSorter);
    }

    private TransformComponent getWorldTransform(GameObject gameObject) {
        getWorldLocAround(gameObject, points[LB], -0.5f, -0.5f);
        getWorldLocAround(gameObject, points[LT],-0.5f, 0.5f);
        getWorldLocAround(gameObject, points[RT],0.5f, 0.5f);
        getWorldLocAround(gameObject, points[RB],0.5f, -0.5f);

        TransformComponent transform = gameObject.getComponent(TransformComponent.class);
        float xSign = transform.scale.x < 0 ? -1: 1;
        float ySign = transform.scale.y < 0 ? -1: 1;

        vec.set(points[RT]).sub(points[LB]).scl(0.5f).add(points[LB]); // midpoint
        tempTransform.position.set(vec);
        vec.set(points[RT]).sub(points[LB]);
        tempTransform.scale.set(points[RT].dst(points[LT]) * xSign, points[RT].dst(points[RB]) * ySign);
        vec.set(points[RT]).sub(points[LT]).angleDeg();
        tempTransform.rotation = vec.angleDeg();

        if(xSign < 0) tempTransform.rotation -= 180;
        if(ySign < 0) tempTransform.rotation += 0;


        return tempTransform;
    }

    private Vector2 getWorldLocAround(GameObject gameObject, Vector2 point, float x, float y) {
        point.set(x, y);
        TransformComponent.localToWorld(gameObject, point);

        return point;
    }

    @EventHandler
    public void onComponentUpdated(ComponentUpdated event) {
        if(event.getComponent() instanceof ParticleComponent) {
            particleCache.remove((ParticleComponent)event.getComponent());
        }
    }

    public void setCamera (OrthographicCamera camera) {
        this.camera = camera;
    }

    public OrthographicCamera getCamera () {
        return camera;
    }

    public void setRenderParentTiles (boolean renderParentTiles) {
        this.renderParentTiles = renderParentTiles;
    }
}
