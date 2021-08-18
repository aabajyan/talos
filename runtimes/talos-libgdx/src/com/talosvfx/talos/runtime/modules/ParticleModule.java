/*******************************************************************************
 * Copyright 2019 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.talosvfx.talos.runtime.modules;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.talosvfx.talos.runtime.Particle;
import com.talosvfx.talos.runtime.ScopePayload;
import com.talosvfx.talos.runtime.values.ModuleValue;
import com.talosvfx.talos.runtime.values.NumericalValue;

public class ParticleModule extends AbstractModule {

    public static final int ID = 0;
    public static final int TAG = 1;

    public static final int POINT_GENERATOR = 3;
    public static final int LIFE = 4;
    public static final int ROTATION = 7;
    public static final int COLOR = 9;
    public static final int TRANSPARENCY = 10;
    public static final int POSITION = 14;


    ModuleValue<ParticlePointDataGeneratorModule> pointGenerator;

    NumericalValue life;
    NumericalValue rotation;
    NumericalValue color;
    NumericalValue transparency;
    NumericalValue position;


    Color tmpColor = new Color();
    Vector2 tmpVec = new Vector2();
    Vector3 tmp3Vec = new Vector3();

    @Override
    protected void defineSlots() {
        pointGenerator = createInputSlot(POINT_GENERATOR, new ModuleValue<ParticlePointDataGeneratorModule>());

        life = createInputSlot(LIFE);
        rotation = createInputSlot(ROTATION);
        color = createInputSlot(COLOR);
        transparency = createInputSlot(TRANSPARENCY);
        position = createInputSlot(POSITION);
        position.set(0f, 0f, 0f, 0f);


        rotation.setFlavour(NumericalValue.Flavour.ANGLE);

    }

    @Override
    public void processValues() {
        // nothing to process, it's all cool as cucumber
    }

    public void updateScopeData(Particle particle) {
        getScope().set(ScopePayload.EMITTER_ALPHA, particle.getEmitterAlpha());
        getScope().set(ScopePayload.PARTICLE_ALPHA, particle.alpha);
        getScope().set(ScopePayload.PARTICLE_SEED, particle.seed);
        getScope().set(ScopePayload.REQUESTER_ID, particle.seed);
        getScope().set(ScopePayload.EMITTER_ALPHA_AT_P_INIT, particle.durationAtInit);
        getScope().set(ScopePayload.PARTICLE_POSITION, particle.getX(), particle.getY());

        getScope().setParticle(particle);
    }

    public float getTransparency() {
        fetchInputSlotValue(TRANSPARENCY);
        if(transparency.isEmpty()) return 1; // defaults
        return transparency.getFloat();
    }

    public float getLife() {
        fetchInputSlotValue(LIFE);
        if(life.isEmpty()) return 1; // defaults
        return life.getFloat();
    }

    public Vector3 getRotation() {
        fetchInputSlotValue(ROTATION);
        if(rotation.isEmpty()) return tmp3Vec.setZero(); // defaults
        final float[] elements = rotation.getElements();
        return tmp3Vec.set(elements[0], elements[1], elements[2]);
    }

    public Color getColor() {
        fetchInputSlotValue(COLOR);
        if(color.isEmpty()) return Color.WHITE; // defaults
        tmpColor.set(color.get(0), color.get(1), color.get(2), 1f);
        return tmpColor;
    }

    public Vector2 getPosition() {
        fetchInputSlotValue(POSITION);
        if(position.isEmpty()) {
            return null;
        }
        tmpVec.set(position.get(0), position.get(1));

        return tmpVec;
    }


    @Override
    public void write (Json json) {
        super.write(json);
    }

    @Override
    public void read (Json json, JsonValue jsonData) {
        super.read(json, jsonData);
    }

    public ParticlePointDataGeneratorModule getPointDataGenerator () {
        fetchInputSlotValue(POINT_GENERATOR);
        return pointGenerator.getModule();
    }
}
