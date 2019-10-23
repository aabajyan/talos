package com.rockbite.tools.talos.runtime.modules;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.rockbite.tools.talos.runtime.ScopePayload;
import com.rockbite.tools.talos.runtime.Slot;
import com.rockbite.tools.talos.runtime.values.Value;

import java.util.Random;

public class RandomInputModule extends Module {

    Class valueType = null;

    private Random random = new Random();

    public int slotCount = 0;

    @Override
    protected void defineSlots() {
        addInputSlot(0);
        createOutputSlot(0, null);
    }

    public void addInputSlot(int key) {
        Slot slot = new Slot(this, key, true);
        inputSlots.put(key, slot);
    }

    @Override
    public void attachModuleToMyInput(Module module, int mySlot, int targetSlot) {
        addInputSlot(slotCount++);
        super.attachModuleToMyInput(module, mySlot, targetSlot);

        // let's figure out the type
        if(valueType == null) {
            valueType = module.getOutputSlot(targetSlot).getValue().getClass();
        } else {
            Class newValueType = module.getOutputSlot(targetSlot).getValue().getClass();
            if(valueType != newValueType) {
                // changing value detaching all previous values
                // detach code goes here
                valueType = newValueType;
            }
        }
        // re init all previous values
        try {
            for(Slot slot : getInputSlots().values()) {
                slot.setValue((Value) ClassReflection.newInstance(valueType));
            }
            getOutputSlot(0).setValue((Value) ClassReflection.newInstance(valueType));
        } catch (ReflectionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void processValues() {

        Value output = outputSlots.get(0).getValue();
        if(output != null) {
            random.setSeed((long) ((getScope().getFloat(ScopePayload.EMITTER_ALPHA_AT_P_INIT) * 10000 * (index+1) * 1000)));
            int index = MathUtils.floor(random.nextFloat() * (inputSlots.size - 1));

            Value input = inputSlots.get(index).getValue();
            if(input != null) {
                output.set(input);
            }
        }
    }
}
