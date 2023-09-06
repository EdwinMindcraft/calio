var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI')

function initializeCoreMod() {
    return {
        "calio_ContainerScreenAccessor": addInterface('net.minecraft.client.gui.screens.inventory.AbstractContainerScreen', 'io/github/apace100/calio/mixin/HandledScreenFocusedSlotAccessor')
    }
}

function addInterface(className, interfaceType) {
    return {
        'target': {
            'type': 'CLASS',
            'name': className
        },
        'transformer': function (node) {
            node.interfaces.add(interfaceType);
            return node;
        }
    }
}