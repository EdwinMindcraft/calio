package io.github.apace100.calio.resource;

import com.google.common.collect.Lists;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;

import java.util.*;
import java.util.function.Consumer;

public class OrderedResourceListenerManager {
    private static final OrderedResourceListenerManager INSTANCE = new OrderedResourceListenerManager();

    public static OrderedResourceListenerManager getInstance() {
        return INSTANCE;
    }

    private final HashMap<PackType, Instance> instances = new HashMap<>();

    OrderedResourceListenerManager() {}

    public OrderedResourceListener.Registration register(PackType resourceType, ResourceLocation id, PreparableReloadListener resourceReloadListener) {
        Instance inst = this.instances.computeIfAbsent(resourceType, rt -> new Instance());
        return new OrderedResourceListener.Registration(inst, id, resourceReloadListener);
    }

    public void addResources(PackType type, Consumer<PreparableReloadListener> registrationMethod) {
        Instance instance = this.instances.get(type);
        if (instance != null)
            instance.finish(registrationMethod);
    }

    static class Instance {
        private final HashMap<ResourceLocation, OrderedResourceListener.Registration> registrations = new HashMap<>();
        private final HashMap<Integer, List<ResourceLocation>> sortedMap = new HashMap<>();
        private int maxIndex = 0;

        private Instance() {
        }

        void add(OrderedResourceListener.Registration registration) {
            this.registrations.put(registration.id, registration);
        }

        void finish(Consumer<PreparableReloadListener> registrationMethod) {
            this.prepareSetsAndSort();
            List<ResourceLocation> sortedList = new LinkedList<>();
            List<ResourceLocation> nextListeners;
            while(!(nextListeners = copy(this.getRegistrations(0))).isEmpty()) {
                sortedList.addAll(nextListeners);
                this.sortedMap.remove(0);
                for(int i = 1; i <= this.maxIndex; i++) {
                    for(ResourceLocation regId : copy(this.getRegistrations(i))) {
                        OrderedResourceListener.Registration registration = this.registrations.get(regId);
                        int before = registration.dependencies.size();
                        nextListeners.forEach(registration.dependencies::remove);
                        this.update(registration, before);
                    }
                }
            }
            if(!this.sortedMap.isEmpty()) {
                StringBuilder errorBuilder = new StringBuilder("Couldn't resolve ordered resource listener dependencies. Unsolved:");
                for(int i = 0; i <= this.maxIndex; i++) {
                    if(!this.getRegistrations(i).isEmpty()) {
                        errorBuilder.append("\t").append(i).append(" dependencies:");
                        for(ResourceLocation id : this.getRegistrations(i)) {
                            OrderedResourceListener.Registration registration = this.registrations.get(id);
                            errorBuilder.append("\t\t").append(registration.toString());
                            registrationMethod.accept(registration.resourceReloadListener);
                        }
                    }
                }
                throw new RuntimeException(errorBuilder.toString());
            } else {
                for(ResourceLocation id : sortedList) {
                    OrderedResourceListener.Registration registration = this.registrations.get(id);
                    registrationMethod.accept(registration.resourceReloadListener);
                }
            }
        }

        private void prepareSetsAndSort() {
            for (OrderedResourceListener.Registration reg : this.registrations.values()) {
                reg.dependencies.removeIf(id -> !this.registrations.containsKey(id));
                reg.dependants.forEach(id -> {
                    if(this.registrations.containsKey(id)) {
                        this.registrations.get(id).dependencies.add(reg.id);
                    }
                });
            }
            this.registrations.values().forEach(this::sortIntoMap);
        }

        private void sortIntoMap(OrderedResourceListener.Registration registration) {
            int index = registration.dependencies.size();
            List<ResourceLocation> list = this.sortedMap.computeIfAbsent(index, i -> new LinkedList<>());
            list.add(registration.id);
            if(index > this.maxIndex) {
                this.maxIndex = index;
            }
        }

        private void update(OrderedResourceListener.Registration registration, int indexBefore) {
            int index = registration.dependencies.size();
            if(index == indexBefore) {
                return;
            }
            List<ResourceLocation> regs = this.getRegistrations(indexBefore);
            regs.remove(registration.id);
            if(regs.isEmpty()) {
                this.sortedMap.remove(indexBefore);
            }
            List<ResourceLocation> list = this.sortedMap.computeIfAbsent(index, i -> new LinkedList<>());
            list.add(registration.id);
        }

        private List<ResourceLocation> getRegistrations(int index) {
            return this.sortedMap.getOrDefault(index, new LinkedList<>());
        }
    }

    private static <T> List<T> copy(List<T> list) {
        return Lists.newLinkedList(list);
    }
}
